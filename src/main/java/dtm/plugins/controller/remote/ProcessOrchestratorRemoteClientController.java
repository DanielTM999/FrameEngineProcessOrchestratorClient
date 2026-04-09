package dtm.plugins.controller.remote;

import dtm.apps.core.ApplicationProperties;
import dtm.apps.core.WindowFactory;
import dtm.apps.core.extension.PluginContext;
import dtm.apps.exceptions.DisplayException;
import dtm.apps.models.enums.PanelPosition;
import dtm.apps.views.MainFrameWindow;
import dtm.apps.views.popup.LoadingDialog;
import dtm.di.annotations.aop.DisableAop;
import dtm.di.application.startup.ManagedApplication;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.dto.ProcessDTO;
import dtm.manager.process.enums.ProcessEvents;
import dtm.manager.process.enums.ProcessManagerEvent;
import dtm.plugins.ProcessOrchestratorClientPluginLaunch;
import dtm.plugins.context.LocalProcessContext;
import dtm.plugins.context.LockContext;
import dtm.plugins.context.RemoteProcessContext;
import dtm.plugins.exceptions.DisconnectedException;
import dtm.plugins.models.Constants;
import dtm.plugins.models.ProcessNodeModel;
import dtm.plugins.models.enums.ProcessPhase;
import dtm.plugins.models.remote.ProcessRemoteServer;
import dtm.plugins.models.remote.ProcessRemoteServerState;
import dtm.plugins.models.remote.RemoteAuthentication;
import dtm.plugins.models.remote.impl.ProcessRemoteServerImpl;
import dtm.plugins.models.remote.res.ProcessStateChangeEvent;
import dtm.plugins.models.remote.res.ServerInitialStateEvent;
import dtm.plugins.services.remote.ProcessServerServices;
import dtm.plugins.services.remote.attacher.ProcessAttachListenerService;
import dtm.plugins.services.remote.attacher.impl.MainProcessAttachListenerService;
import dtm.plugins.views.components.form.ProcessNodeFormView;
import dtm.plugins.views.components.pipeline.ProcessPipelineView;
import dtm.plugins.views.components.pipeline.ProcessSidebarItem;
import dtm.plugins.views.components.terminal.TerminalViewPanel;
import dtm.plugins.views.notification.ServerStatusNotification;
import dtm.plugins.views.remote.ProcessOrchestratorRemoteClientView;
import dtm.request_actions.http.simple.core.HttpAction;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.context.NotificationManager;
import dtm.stools.controllers.component.AbstractViewController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static javax.swing.SwingUtilities.invokeLater;

@Slf4j
@DisableAop
@RequiredArgsConstructor
public class ProcessOrchestratorRemoteClientController extends AbstractViewController<BlockingPanel> {

    private final ApplicationProperties processOrchestratorSettingsProps;
    private final WindowFactory windowFactory;
    private final PluginContext pluginContext;
    private final HttpAction httpAction;
    private final Runnable logoutAction;


    private final AtomicReference<ProcessServerServices> processServerServicesRef = new AtomicReference<>();
    private final AtomicReference<RemoteAuthentication> remoteAuthenticationAtomicReference = new AtomicReference<>();
    private final AtomicReference<ProcessAttachListenerService> processAttachListenerServiceRef = new AtomicReference<>();

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, ProcessAttachListenerService> processAttachMap = new ConcurrentHashMap<>();
    private final Map<String, RemoteProcessContext> contextMap = new ConcurrentHashMap<>();

    private ProcessOrchestratorRemoteClientView remoteClientView;
    private MainFrameWindow mainFrameWindow;
    private ProcessPipelineView pipelineView;
    private ProcessSidebarItem currentSelected;

    @Override
    public void onInit(BlockingPanel view) {
        super.onInit(view);
        if(view instanceof ProcessOrchestratorRemoteClientView v) remoteClientView = v;
        mainFrameWindow = pluginContext.getContextWindowAs(MainFrameWindow.class);
        pipelineView = new ProcessPipelineView()
                .onNodeClick(this::onNodeClick)
                .onNodeEdit(this::onNodeEdit)
                .onNodeDelete(this::onNodeDelete)
                .onAddClick(this::onAddClick);

        ProcessOrchestratorClientPluginLaunch.logoutListener.add(this::closeSession);

        remoteClientView.getBtnCreate().addActionListener(e -> {
            showLateralRightDetailsPanel(
                    new ProcessNodeFormView(null)
                            .onCancel(this::hideLateralRightDetailsPanel)
                            .onSave(m -> onSaveProcessNodeModel(m, true))
            );
        });

        remoteClientView.getBtnRefresh().addActionListener(e -> {
            contextMap.clear();
            remoteClientView.clearSidebar();
            loadProcessAsync(() -> {
               invokeLater(() -> {
                   if(currentSelected != null) {
                       selectFromSideBar(currentSelected.getProcessId());
                   }
               });
            });
        });

        remoteClientView.getBtnBack().addActionListener(e -> {
            logoutAction.run();
        });

        remoteClientView.getTxtSearch().addEventListner(EventType.SELECT, event -> {
            ProcessSidebarItem sidebarItem = event.tryGetValue();
            if(sidebarItem != null) {
                RemoteProcessContext ctx = contextMap.get(sidebarItem.getProcessId());
                if(ctx != null){
                    onSidebarItemClick(ctx.getProcessSidebar(), ctx.getProcessDefinition());
                }
            }
        });
    }

    public void loadSession(){
        if(remoteAuthenticationAtomicReference.compareAndSet(null, pluginContext.getMemory(LockContext.INSTANCE).get(Constants.PROCESS_ORCHESTRATOR_REMOTE_KEY))){
            loadProcessAsync();
            attachGlobalProcessListener();
        }else{
            RemoteAuthentication remoteAuthentication =  remoteAuthenticationAtomicReference.get();
            LocalDateTime expirationDateTime = remoteAuthentication.getExpirationDateTime();
            if (expirationDateTime == null || LocalDateTime.now().isAfter(expirationDateTime)) {
                logoutAction.run();
            }
        }
    }


    private void onMainProcessEvent(String eventStr, String content){
        try{
            log.debug("SSE evento recebido: {} | conteúdo: {}", eventStr, content);
            if("CONNECTED".equalsIgnoreCase(eventStr)){
                ServerInitialStateEvent event = ServerInitialStateEvent.ofString(content);
                onMainProcessEvent(event);
            } else {
                ProcessEvents processStateEvent = getEnumByName(eventStr, ProcessEvents.class);
                if(processStateEvent != null){
                    ProcessStateChangeEvent eventContent = ProcessStateChangeEvent.ofString(content);
                    onMainProcessEvent(processStateEvent, eventContent);
                    return;
                }

                ProcessManagerEvent processManagerEvent = getEnumByName(eventStr, ProcessManagerEvent.class);
                if(processManagerEvent != null){
                    ProcessRemoteServer processRemoteServer = ProcessRemoteServerImpl.ofString(content);
                    onMainProcessEvent(processManagerEvent, processRemoteServer);
                    return;
                }

            }
        } catch (Exception e) {
            log.error("Erro as receber Evento: {}", eventStr, e);
            throw new DisplayException("Erro ao Receber Evento: "+e.getMessage()).title("Erro ao Receber Evento");
        }
    }

    private void onMainProcessEvent(ServerInitialStateEvent event){
        NotificationManager.startNotification(new ServerStatusNotification(event), 10, TimeUnit.SECONDS);
    }

    private void onMainProcessEvent(ProcessEvents eventType, ProcessStateChangeEvent event){
        String mainProcessId = event.getMainProcessDefinition().getProcessId();
        String currentProcessId = event.getProcessDefinition().getProcessId();


        RemoteProcessContext ctx = contextMap.get(mainProcessId);
        if (ctx == null) return;

        ProcessNodeModel root = ctx.getProcessDefinition() != null
                ? ProcessNodeModel.from(ctx.getProcessDefinition())
                : null;
        if (root == null) return;

        if(event.isMainProcess()){

            if(eventType == ProcessEvents.STARTING || eventType == ProcessEvents.RUNNING){
                ctx.setRunning(true);
            }else if(eventType == ProcessEvents.STOPPED || eventType == ProcessEvents.DESTROYED){
                ctx.setRunning(false);
            }

        }
        ProcessNodeModel found = findNodeRecursive(root, currentProcessId);
        if (found == null) return;

        boolean running = eventType == ProcessEvents.STARTING || eventType == ProcessEvents.RUNNING;
        forEachNodeRecursive(root, node -> {
            boolean isCurrentNode = currentProcessId.equalsIgnoreCase(node.getProcessId());
            invokeLater(() -> pipelineView.setNodeRunning(node.getProcessId(), isCurrentNode && running));
        });
    }

    private void onMainProcessEvent(ProcessManagerEvent processManagerEvent, ProcessRemoteServer processRemoteServer){
        if(processManagerEvent == ProcessManagerEvent.DELETE){
            deleteProcessFromSideBar(processRemoteServer.getMainProcessDefinition());
            pipelineView.clear();
            return;
        }

        reloadProcess(processRemoteServer);
        selectFromSideBar(processRemoteServer.getMainProcessDefinition().getProcessId());
    }

    private void onMainProcessError(Throwable throwable){
        log.error("erro mainProcess", throwable);
        ManagedApplication.executeInMainContext(() -> {
            ProcessAttachListenerService current = processAttachListenerServiceRef.get();
            if (current == null || current.isClosed()) return;

            if (throwable instanceof DisconnectedException) {
                logoutAction.run();
            } else {
                log.warn("Conexão SSE perdida, reconectando em 3s...");
                virtualExecutor.submit(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(3);
                        attachGlobalProcessListener();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        });
    }



    private void onProcessAttachEvent(String event, String content, RemoteProcessContext ctx){
        event = ((event != null) ? event : "").toUpperCase();

        try{
            switch (event) {
                case "PROCESS-METADATA" -> {
                    break;
                }
                case "PROCESS-OUTPUT" -> {
                    ctx.appendOutputLine(content);
                }
                default -> {
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Erro as receber Evento: {}", event, e);
            throw new DisplayException("Erro ao Receber Evento: "+e.getMessage()).title("Erro ao Receber Evento");
        }
    }

    private void onProcessAttachError(Throwable throwable, RemoteProcessContext ctx){
        ctx.setMonitoring(false);
        ctx.setRunning(false);
        log.error("Erro no Attach", throwable);
    }


    private void loadProcessAsync(){
        loadProcessAsync(null);
    }

    private void loadProcessAsync(Runnable onLoad){
        virtualExecutor.submit(() -> {
            contextMap.clear();
            String selectedId = null;

            if(currentSelected != null){
                selectedId = contextMap.entrySet().stream()
                        .filter(e -> e.getValue().getProcessDefinitionId().equalsIgnoreCase(currentSelected.getProcessId()))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(null);
            }

            LoadingDialog loadingDialog = windowFactory.newWindow(LoadingDialog.class, false, new Object[]{
                    pluginContext.getContextWindowAs(MainFrameWindow.class),
                    "Sincronizando",
                    "Obtendo lista de processos do servidor remoto..."
            });

            Collection<ProcessRemoteServer> processRemoteServers = getProcessesFromServer();
            processRemoteServers.forEach(processRemoteServer -> {
                ProcessDefinition processDefinition = processRemoteServer.getProcessDefinition();
                String id = processDefinition.getProcessId();
                if (contextMap.containsKey(id)) {
                    RemoteProcessContext ctx = contextMap.get(id);
                    invokeLater(() -> {
                        remoteClientView.removeSidebarItem(ctx.getProcessSidebar());
                    });
                    contextMap.remove(id);
                }
                registerProcess(processRemoteServer);
            });
            setSearchDatasource();
            if (selectedId != null) {
                RemoteProcessContext ctx = contextMap.get(selectedId);
                if (ctx != null) {
                    invokeLater(() -> {
                        ctx.getProcessSidebar().setSelected(true);
                        currentSelected = ctx.getProcessSidebar();
                        pipelineView.load(ctx.getProcessDefinition());
                    });
                }
            }

            loadingDialog.dispose();

            if(onLoad != null) {
                onLoad.run();
            }
        });
    }

    private void attachGlobalProcessListener(){
        ProcessAttachListenerService processAttachListenerService = new MainProcessAttachListenerService (
                httpAction,
                remoteAuthenticationAtomicReference::get
        );
        processAttachListenerService.onEvent(this::onMainProcessEvent);
        processAttachListenerService.onError(this::onMainProcessError);
        processAttachListenerServiceRef.set(processAttachListenerService);
        processAttachListenerService.attach();
    }




    private void onNodeClick(ProcessNodeModel definition){
        showLateralRightDetailsPanel(
                new ProcessNodeFormView(definition)
                        .onCancel(this::hideLateralRightDetailsPanel)
                        .onSave(this::onSaveProcessNodeModel)
        );
    }

    private void onNodeEdit(ProcessNodeModel definition) {
        showLateralRightDetailsPanel(
                new ProcessNodeFormView(definition)
                        .onCancel(this::hideLateralRightDetailsPanel)
                        .onSave(this::onSaveProcessNodeModel)
        );
    }

    private void onAddClick(ProcessPhase phase, ProcessNodeModel parent) {
        showLateralRightDetailsPanel(
                new ProcessNodeFormView(null)
                        .onCancel(this::hideLateralRightDetailsPanel)
                        .onSave((model) -> onSaveProcessNodeModel(model, parent, phase))
        );
    }

    private void onNodeDelete(ProcessNodeModel definition) {
        int option = ModernDialog.builder()
                .type(ModernDialog.Type.INFO)
                .accentColor(new Color(59, 130, 246))
                .title("Remover Processo")
                .message("Esta ação é irreversível e removerá o processo permanentemente. Deseja continuar?")
                .option("Não", 0)
                .option("Sim", 1, new Color(220, 53, 69), Color.WHITE)
                .show();

        if(option != 1)return;
        ProcessServerServices processServerServices = getProcessServerServices();
        ProcessNodeModel root = pipelineView.getCurrent();

        if(root.getProcessId().equalsIgnoreCase(definition.getProcessId())){
            processServerServices.delete(root.getProcessId());
        }else{
            removeOfParentById(root,  definition.getProcessId());
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(root);
            processServerServices.save(root.getProcessId(), processDTO);
        }
    }

    private void onSaveProcessNodeModel(ProcessNodeModel model, ProcessNodeModel parent, ProcessPhase phase){
        ProcessNodeModel root = pipelineView.getCurrent();
        String processId = root.getProcessId();
        if(phase == ProcessPhase.AFTER){
            if(parent.getSubProcessesAfter() == null){
                parent.setSubProcessesAfter(new ArrayList<>());
            }
            parent.getSubProcessesAfter().add(model);
        }else{
            if(parent.getSubProcessesBefore() == null){
                parent.setSubProcessesBefore(new ArrayList<>());
            }
            parent.getSubProcessesBefore().add(model);
        }
        ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(root);
        getProcessServerServices().save(processId, processDTO);
        hideLateralRightDetailsPanel();
    }

    private void onSaveProcessNodeModel(ProcessNodeModel model){
        onSaveProcessNodeModel(model, false);
    }

    private void onSaveProcessNodeModel(ProcessNodeModel model, boolean newProcess){
        ProcessServerServices processServerServices = getProcessServerServices();
        if(newProcess){
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(model);
            processServerServices.save(processDTO);
        }else{
            ProcessNodeModel root = pipelineView.getCurrent();
            String processId = root.getProcessId();
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(root);
            processServerServices.save(processId, processDTO);
        }
        hideLateralRightDetailsPanel();
    }




    private Collection<ProcessRemoteServer> getProcessesFromServer(){
        return getProcessServerServices().getAllProcesses();
    }

    private void setSearchDatasource(){
        remoteClientView.getTxtSearch().setDataSource(
                new ArrayList<>(
                        contextMap.values()
                                .stream()
                                .map(RemoteProcessContext::getProcessSidebar)
                                .toList()
                )
        );
    }

    private void registerProcess(ProcessRemoteServer processRemoteServer) {
        ProcessDefinition processDefinition = processRemoteServer.getProcessDefinition();
        ProcessSidebarItem item = remoteClientView.addProcessSidebarItem(processDefinition, processRemoteServer.getProcessRemoteServerState().isRunning());
        RemoteProcessContext context = new RemoteProcessContext(pluginContext, item, processDefinition);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSidebarItemClick(item, processRemoteServer.getProcessDefinition());
                if (SwingUtilities.isRightMouseButton(e)) handleMouseEvents(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseEvents(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseEvents(e);
            }

            private void handleMouseEvents(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e, context);
                }
            }
        });
        updateContextState(context, processRemoteServer.getProcessRemoteServerState());
        contextMap.put(context.getProcessDefinitionId(), context);
    }

    private void updateContextState(RemoteProcessContext ctx, ProcessRemoteServerState processRemoteServerState) {
        ProcessSidebarItem sidebarItem = ctx.getProcessSidebar();
        invokeLater(() -> sidebarItem.setRunning(processRemoteServerState.isRunning()));
    }

    private ProcessServerServices getProcessServerServices(){
        processServerServicesRef.compareAndSet(null, pluginContext.getService(ProcessServerServices.class));
        return processServerServicesRef.get();
    }

    private void onSidebarItemClick(ProcessSidebarItem item, ProcessDefinition process) {
        invokeLater(() -> {
            try{
                if(currentSelected != null){
                    contextMap.values().forEach(ctx -> ctx.getProcessSidebar().setSelected(false));
                    item.setSelected(true);

                    if(currentSelected.getProcessId().equalsIgnoreCase(item.getProcessId())) return;

                }else{
                    item.setSelected(true);
                }
                currentSelected = item;
                pipelineView.load(process);
                remoteClientView.setDefinitionContent(pipelineView);
            } catch (Exception e) {
                log.error("erro ao selecionar sidebar", e);
            }
        });
    }

    private void showContextMenu(MouseEvent e, RemoteProcessContext ctx) {
        JPopupMenu menu = new JPopupMenu();

        if (ctx.isRunning()) {
            JMenuItem btnStop = new JMenuItem("Parar Processo");
            btnStop.addActionListener(evt -> {
                pluginContext.getContextWindow().getWindowExecutor().executeAsync(() -> {
                    stopProcess(ctx);
                });
            });
            menu.add(btnStop);


            if(!ctx.isMonitoring()){
                JMenuItem btnMonitor = new JMenuItem("Monitorar (Abrir Terminal)");
                btnMonitor.addActionListener(evt -> {
                    showTerminalMonitor(ctx);
                });
                menu.add(btnMonitor);
            }

            JMenuItem btnRestart = new JMenuItem("Reiniciar");
            btnRestart.addActionListener(evt -> {
                //restartProcess(ctx);
            });
            menu.add(btnRestart);

        }else{
            JMenuItem btnStart = new JMenuItem("Iniciar");
            btnStart.addActionListener(evt -> {
                pluginContext.getContextWindow().getWindowExecutor().executeAsync(() -> {
                    startProcess(ctx);
                });
            });
            menu.add(btnStart);

            JMenuItem btnRemove = new JMenuItem("Remover da Lista");
            btnRemove.addActionListener(evt -> {
                pluginContext.getContextWindow().getWindowExecutor().executeAsync(() -> {
                    onNodeDelete(ProcessNodeModel.from(ctx.getProcessDefinition()));
                });
            });
            menu.add(btnRemove);
        }

        if (!ctx.getOutputBuffer().isEmpty()){
            JMenuItem btnClear = new JMenuItem("Limpar Saída");
            btnClear.addActionListener(evt -> {
                ctx.clearLog();
            });
            menu.add(btnClear);
        }

        menu.show(e.getComponent(), e.getX(), e.getY());

    }

    private void startProcess(RemoteProcessContext ctx) {
        mainFrameWindow.runOnUi((t) -> {
            ctx.clearLog();
            JPanel monitorPanel = ctx.getMonitorPanel();
            mainFrameWindow.requireUtilityPanel(PanelPosition.BOTTOM, monitorPanel, 350, 0);
        });

        int option = ModernDialog.builder()
                .type(ModernDialog.Type.INFO)
                .accentColor(new Color(59, 130, 246))
                .title("Histórico de Logs")
                .message("Deseja recuperar as saídas (logs) anteriores deste processo?")
                .option("Sim", 0)
                .option("Não", 1, new Color(220, 53, 69), Color.WHITE)
                .show();


        LoadingDialog loadingDialog = windowFactory.newWindow(LoadingDialog.class, false, new Object[]{
                pluginContext.getContextWindow(),
                "Inciando Processo",
                "Inciando Processo: "+ctx.getProcessName()
        });

        try{
            getProcessServerServices().start(ctx.getProcessDefinitionId());
            attachRunningProcess(ctx, option == 0);
        }finally {
            loadingDialog.dispose();
        }
    }

    private void attachRunningProcess(RemoteProcessContext ctx, boolean showHistory){
        processAttachMap.compute(ctx.getProcessDefinitionId(), (key, attacher) -> {
            if (attacher != null) {
                try {
                    attacher.interrupt();
                } catch (Exception e) {
                    log.error("Erro ao limpar", e);
                }
            }


            ProcessAttachListenerService processAttachListenerService = getProcessServerServices().newAttachProcess(ctx.getProcessDefinition(), showHistory);
            processAttachListenerService.onEvent((e, o) -> ProcessOrchestratorRemoteClientController.this.onProcessAttachEvent(e, o, ctx));
            processAttachListenerService.onError((e) -> ProcessOrchestratorRemoteClientController.this.onProcessAttachError(e, ctx));
            processAttachListenerService.attach();


            return processAttachListenerService;
        });
    }

    private void stopProcess(RemoteProcessContext ctx){
        int option = ModernDialog.builder()
                .type(ModernDialog.Type.INFO)
                .accentColor(new Color(59, 130, 246))
                .title("Parar Processo")
                .message("Essa ação irá encerrar o processo remoto imediatamente. Deseja continuar?")
                .option("Não", 0)
                .option("Sim", 1, new Color(220, 53, 69), Color.WHITE)
                .show();

        if (option == 1) {
            LoadingDialog loadingDialog = windowFactory.newWindow(LoadingDialog.class, false, new Object[]{
                    pluginContext.getContextWindow(),
                    "Finalizando Processo",
                    "Finalizando Processo: "+ctx.getProcessName()
            });
            try{
                getProcessServerServices().stop(ctx.getProcessDefinitionId());
                ctx.setRunning(false);
                ctx.setMonitoring(false);
            }finally {
                loadingDialog.dispose();
            }

        }
    }

    private void showTerminalMonitor(RemoteProcessContext ctx){
        mainFrameWindow.runOnUi((t) -> {
            JPanel monitorPanel = ctx.getMonitorPanel();
            mainFrameWindow.requireUtilityPanel(PanelPosition.BOTTOM, monitorPanel, 350, 0);
        });
    }

    private void selectFromSideBar(String processId){
        try{
            if(currentSelected != null){
                if(currentSelected.getProcessId().equalsIgnoreCase(processId)){
                    RemoteProcessContext context = contextMap.get(processId);
                    if(context != null){
                        context.getProcessSidebar().setSelected(true);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void deleteProcessFromSideBar(ProcessDefinition processDefinition){
        String id = processDefinition.getProcessId();
        if (contextMap.containsKey(id)) {
            RemoteProcessContext ctx = contextMap.get(id);
            invokeLater(() -> {
                remoteClientView.removeSidebarItem(ctx.getProcessSidebar());
            });
            contextMap.remove(id);
            setSearchDatasource();
        }
    }

    private void reloadProcess(ProcessRemoteServer processRemoteServer){
        String selectedId = null;

        if(currentSelected != null){
            selectedId = contextMap.entrySet().stream()
                    .filter(e -> e.getValue().getProcessDefinitionId().equalsIgnoreCase(currentSelected.getProcessId()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
        }

        String id = processRemoteServer.getMainProcessDefinition().getProcessId();
        if (contextMap.containsKey(id)) {
            RemoteProcessContext ctx = contextMap.get(id);
            invokeLater(() -> {
                remoteClientView.removeSidebarItem(ctx.getProcessSidebar());
            });
            contextMap.remove(id);
        }
        registerProcess(processRemoteServer);
        setSearchDatasource();
        if (selectedId != null) {
            RemoteProcessContext ctx = contextMap.get(selectedId);
            if (ctx != null) {
                invokeLater(() -> {
                    ctx.getProcessSidebar().setSelected(true);
                    currentSelected = ctx.getProcessSidebar();
                    pipelineView.load(ctx.getProcessDefinition());
                });
            }
        }
    }

    private void showLateralRightDetailsPanel(JComponent component) {
        mainFrameWindow.runOnUi(w -> {
            mainFrameWindow.requireUtilityPanel(PanelPosition.RIGHT, component, 350, 100);
        });
    }

    private void hideLateralRightDetailsPanel() {
        mainFrameWindow.runOnUi(w -> {
            mainFrameWindow.closeUtilityPanels(PanelPosition.RIGHT);
        });
    }

    private void removeOfParentById(ProcessNodeModel root, String targetId){
        ProcessNodeModel parent = findParentOf(root, targetId);
        if (parent == null) return;
        if (parent.getSubProcessesBefore() != null) {
            parent.getSubProcessesBefore().removeIf(sub -> sub.getProcessId().equalsIgnoreCase(targetId));
        }

        if (parent.getSubProcessesAfter() != null) {
            parent.getSubProcessesAfter().removeIf(sub -> sub.getProcessId().equalsIgnoreCase(targetId));
        }
    }

    private ProcessNodeModel findParentOf(ProcessNodeModel root, String targetId) {
        if (root == null) return null;

        if (root.getSubProcessesBefore() != null) {
            for (ProcessNodeModel sub : root.getSubProcessesBefore()) {
                if(sub.getProcessId().equalsIgnoreCase(targetId)) return root;
                ProcessNodeModel found = findNodeRecursive(sub, targetId);
                if (found != null) return found;
            }
        }

        if (root.getSubProcessesAfter() != null) {
            for (ProcessNodeModel sub : root.getSubProcessesAfter()) {
                if(sub.getProcessId().equalsIgnoreCase(targetId)) return root;
                ProcessNodeModel found = findNodeRecursive(sub, targetId);
                if (found != null) return found;
            }
        }


        return null;
    }

    private void forEachNodeRecursive(ProcessNodeModel node, Consumer<ProcessNodeModel> action) {
        if (node == null) return;
        action.accept(node);

        if (node.getSubProcessesBefore() != null) {
            for (ProcessNodeModel sub : node.getSubProcessesBefore()) {
                forEachNodeRecursive(sub, action);
            }
        }

        if (node.getSubProcessesAfter() != null) {
            for (ProcessNodeModel sub : node.getSubProcessesAfter()) {
                forEachNodeRecursive(sub, action);
            }
        }
    }

    private <T extends Enum<T>> T getEnumByName(String name, Class<T> clazz){
        return Arrays.stream(clazz.getEnumConstants())
                .filter(e -> e.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private ProcessNodeModel findNodeRecursive(ProcessNodeModel node, String targetId) {
        if (node == null) return null;
        if (node.getProcessId().equalsIgnoreCase(targetId)) return node;

        if (node.getSubProcessesBefore() != null) {
            for (ProcessNodeModel sub : node.getSubProcessesBefore()) {
                ProcessNodeModel found = findNodeRecursive(sub, targetId);
                if (found != null) return found;
            }
        }

        if (node.getSubProcessesAfter() != null) {
            for (ProcessNodeModel sub : node.getSubProcessesAfter()) {
                ProcessNodeModel found = findNodeRecursive(sub, targetId);
                if (found != null) return found;
            }
        }

        return null;
    }

    private void closeSession(){
        invokeLater(() -> {
            currentSelected = null;
            pipelineView.clear();
            remoteClientView.clearSidebar();
            mainFrameWindow.closeUtilityPanelIf(PanelPosition.BOTTOM, TerminalViewPanel.class);
            mainFrameWindow.closeUtilityPanelIf(PanelPosition.RIGHT, ProcessNodeFormView.class);
        });

        contextMap.clear();
        remoteAuthenticationAtomicReference.set(null);
        this.processAttachMap.forEach((key, attacher) -> {
            attacher.interrupt();
        });
        this.processAttachMap.clear();
        ProcessAttachListenerService processAttachListenerService = this.processAttachListenerServiceRef.get();
        if(processAttachListenerService != null) processAttachListenerService.interrupt();
        processAttachListenerServiceRef.set(null);
    }
}
