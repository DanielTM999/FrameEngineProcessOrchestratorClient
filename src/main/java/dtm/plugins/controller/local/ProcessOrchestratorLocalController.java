package dtm.plugins.controller.local;

import dtm.apps.core.ApplicationProperties;
import dtm.apps.core.extension.PluginContext;
import dtm.apps.exceptions.DisplayException;
import dtm.apps.models.enums.PanelPosition;
import dtm.apps.views.MainFrameWindow;
import dtm.di.annotations.aop.DisableAop;
import dtm.manager.ProcessOrchestratorManager;
import dtm.manager.common.exception.ProcessNotFoundException;
import dtm.manager.common.exception.StartProcessException;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.manager.process.dto.ProcessDTO;
import dtm.manager.process.dto.ProcessOutputEvent;
import dtm.manager.process.engine.ProcessEngine;
import dtm.manager.process.enums.ProcessEvents;
import dtm.manager.process.enums.ProcessManagerEvent;
import dtm.manager.process.runtime.ProcessExecutor;
import dtm.plugins.ProcessOrchestratorClientPluginLaunch;
import dtm.plugins.context.LockContext;
import dtm.plugins.models.Constants;
import dtm.plugins.models.ProcessNodeModel;
import dtm.plugins.context.LocalProcessContext;
import dtm.plugins.models.UserNotificationContent;
import dtm.plugins.models.enums.ProcessPhase;
import dtm.plugins.views.components.form.ProcessNodeFormView;
import dtm.plugins.views.components.terminal.TerminalViewPanel;
import dtm.plugins.views.local.ProcessOrchestratorLocalClientView;
import dtm.plugins.views.components.pipeline.ProcessPipelineView;
import dtm.plugins.views.components.pipeline.ProcessSidebarItem;
import dtm.plugins.views.notification.UserNotification;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.context.NotificationManager;
import dtm.stools.controllers.component.BindingAbstractViewController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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
public class ProcessOrchestratorLocalController extends BindingAbstractViewController<BlockingPanel> {


    private final ApplicationProperties processOrchestratorSettingsProps;
    private final PluginContext pluginContext;
    private final AtomicReference<ProcessOrchestratorManager> processOrchestratorRef = new AtomicReference<>();
    private final Map<String, LocalProcessContext> contextMap = new ConcurrentHashMap<>();
    private final ExecutorService writeExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Runnable logoutAction;

    private ProcessOrchestratorLocalClientView localClientView;
    private MainFrameWindow mainFrameWindow;

    private ProcessPipelineView pipelineView;
    private ProcessSidebarItem currentSelected;


    @Override
    public void onInit(BlockingPanel view) {
        super.onInit(view);
        if(view instanceof ProcessOrchestratorLocalClientView v) localClientView = v;
        mainFrameWindow = pluginContext.getContextWindowAs(MainFrameWindow.class);
        pipelineView = new ProcessPipelineView()
                .onNodeClick(this::onNodeClick)
                .onNodeEdit(this::onNodeEdit)
                .onNodeDelete(this::onNodeDelete)
                .onDragEnd(this::onDragEnd)
                .onAddClick(this::onAddClick);

        ProcessOrchestratorClientPluginLaunch.logoutListener.add(() -> {
            invokeLater(() -> {
                mainFrameWindow.closeUtilityPanelIf(PanelPosition.BOTTOM, TerminalViewPanel.class);
                mainFrameWindow.closeUtilityPanelIf(PanelPosition.RIGHT, ProcessNodeFormView.class);
            });
        });

        localClientView.getBtnCreate().addActionListener(e -> {
            showLateralRightDetailsPanel(
                    new ProcessNodeFormView(null)
                            .onCancel(this::hideLateralRightDetailsPanel)
                            .onSave(m -> onSaveProcessNodeModel(m, true))
            );
        });

        localClientView.getBtnRefresh().addActionListener(e -> {
            contextMap.clear();
            localClientView.clearSidebar();
            reloadAllProcess();
            if(currentSelected != null) {
                selectFromSideBar(currentSelected.getProcessId());
            }
        });

        localClientView.getBtnBack().addActionListener(e -> {
            logoutAction.run();
        });

        localClientView.getTxtSearch().addEventListner(EventType.SELECT, event -> {
            ProcessSidebarItem sidebarItem = event.tryGetValue();
            if(sidebarItem != null) {
                LocalProcessContext ctx = contextMap.get(sidebarItem.getProcessId());
                if(ctx != null){
                    onSidebarItemClick(ctx.getProcessSidebar(), ctx.getProcessDefinition());
                }
            }
        });
    }

    public void loadSession(){
        if(processOrchestratorRef.compareAndSet(null, pluginContext.getMemory(LockContext.INSTANCE).get(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY))){
            loadProcess();
            attachGlobalProcessListener(processOrchestratorRef.get());
        }
    }



    private void loadProcess(){
        contextMap.clear();
        reloadAllProcess();
    }

    private void attachGlobalProcessListener(ProcessOrchestratorManager processOrchestrator){
        processOrchestrator.getEngine().addProcessListener((processExecutor, status) -> {
            log.debug("attachGlobalProcessListener process: {}, event: {}", processExecutor.getDefinition().getProcessName(), status);
            String mainProcessId = processExecutor.getDefinition().getProcessId();
            String currentProcessId = status.getProcessDefinition().getProcessId();

            LocalProcessContext ctx = contextMap.get(mainProcessId);
            if (ctx == null) return;

            ProcessNodeModel root = ctx.getProcessDefinition() != null
                    ? ProcessNodeModel.from(ctx.getProcessDefinition())
                    : null;
            if (root == null) return;

            if(status.isMainProcess()){

                ctx.setProcessExecutor(processExecutor);
                ProcessEvents processEvents = status.getAction();

                if(processEvents == ProcessEvents.STARTING ||  processEvents == ProcessEvents.RUNNING){
                    ctx.setRunning(true);
                }else if(processEvents == ProcessEvents.STOPPED || processEvents == ProcessEvents.DESTROYED){
                    ctx.setRunning(false);
                }
            }

            ProcessNodeModel found = findNodeRecursive(root, currentProcessId);
            if (found == null) return;

            ProcessEvents processEvents = status.getAction();
            boolean running = processEvents == ProcessEvents.STARTING || processEvents == ProcessEvents.RUNNING;
            forEachNodeRecursive(root, node -> {
                boolean isCurrentNode = currentProcessId.equalsIgnoreCase(node.getProcessId());
                invokeLater(() -> pipelineView.setNodeRunning(node.getProcessId(), isCurrentNode && running));
            });
        });

        processOrchestrator.getProcessManager().addProcessListener(this::updateSideBarProcess);
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
               localClientView.setDefinitionContent(pipelineView);
           } catch (Exception e) {
               log.error("erro ao selecionar sidebar", e);
           }
        });
    }




    private void onDragEnd(ProcessNodeModel process, List<ProcessNodeModel> definitionList) {

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

        ProcessNodeModel root = pipelineView.getCurrent();
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();

        if(root.getProcessId().equalsIgnoreCase(definition.getProcessId())){
            processOrchestrator.getProcessManager().removeProcess(root.getProcessId());
        }else{
            removeOfParentById(root,  definition.getProcessId());
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(root);
            processOrchestrator.getProcessManager().saveProcess(root.getProcessId(), processDTO);
        }
    }

    private void onAddClick(ProcessPhase phase, ProcessNodeModel parent) {
        showLateralRightDetailsPanel(
                new ProcessNodeFormView(null)
                        .onCancel(this::hideLateralRightDetailsPanel)
                        .onSave((model) -> onSaveProcessNodeModel(model, parent, phase))
        );
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
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        processOrchestrator.getProcessManager().saveProcess(processId, processDTO);
        hideLateralRightDetailsPanel();
    }

    private void onSaveProcessNodeModel(ProcessNodeModel model){
        onSaveProcessNodeModel(model, false);
    }

    private void onSaveProcessNodeModel(ProcessNodeModel model, boolean newProcess){
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        if(newProcess){
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(model);
            processOrchestrator.getProcessManager().saveProcess(processDTO);
        }else{
            ProcessNodeModel root = pipelineView.getCurrent();
            String processId = root.getProcessId();
            ProcessDTO processDTO = ProcessNodeModel.toProcessDTO(root);
            processOrchestrator.getProcessManager().saveProcess(processId, processDTO);
        }
        hideLateralRightDetailsPanel();
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


    private void updateSideBarProcess(ProcessDefinition processDefinition, ProcessManagerEvent processManagerEvent){
        if(processManagerEvent == ProcessManagerEvent.DELETE){
            deleteProcessFromSideBar(processDefinition);
            pipelineView.clear();
            return;
        }

        reloadProcess(processDefinition);
        selectFromSideBar(processDefinition);
    }

    private void selectFromSideBar(ProcessDefinition processDefinition){
        selectFromSideBar(processDefinition.getProcessId());
    }

    private void selectFromSideBar(String processId){
        try{
            if(currentSelected != null){
                if(currentSelected.getProcessId().equalsIgnoreCase(processId)){
                    LocalProcessContext context = contextMap.get(processId);
                    if(context != null){
                        context.getProcessSidebar().setSelected(true);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void reloadAllProcess() {
        String selectedId = null;

        if(currentSelected != null){
            selectedId = contextMap.entrySet().stream()
                    .filter(e -> e.getValue().getProcessDefinitionId().equalsIgnoreCase(currentSelected.getProcessId()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
        }

        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        processOrchestrator.getProcessManager().getAllProcess().forEach(process -> {
            String id = process.getProcessId();
            if (contextMap.containsKey(id)) {
                LocalProcessContext ctx = contextMap.get(id);
                invokeLater(() -> {
                    localClientView.removeSidebarItem(ctx.getProcessSidebar());
                });
                contextMap.remove(id);
            }
            registerProcess(process);
        });

        setSearchDatasource();
        if (selectedId != null) {
            LocalProcessContext ctx = contextMap.get(selectedId);
            if (ctx != null) {
                invokeLater(() -> {
                    ctx.getProcessSidebar().setSelected(true);
                    currentSelected = ctx.getProcessSidebar();
                    pipelineView.load(ctx.getProcessDefinition());
                });
            }
        }
    }

    private void reloadProcess(ProcessDefinition processDefinition){
        String selectedId = null;

        if(currentSelected != null){
            selectedId = contextMap.entrySet().stream()
                    .filter(e -> e.getValue().getProcessDefinitionId().equalsIgnoreCase(currentSelected.getProcessId()))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
        }

        String id = processDefinition.getProcessId();
        if (contextMap.containsKey(id)) {
            LocalProcessContext ctx = contextMap.get(id);
            invokeLater(() -> {
                localClientView.removeSidebarItem(ctx.getProcessSidebar());
            });
            contextMap.remove(id);
        }
        registerProcess(processDefinition);
        setSearchDatasource();
        if (selectedId != null) {
            LocalProcessContext ctx = contextMap.get(selectedId);
            if (ctx != null) {
                invokeLater(() -> {
                    ctx.getProcessSidebar().setSelected(true);
                    currentSelected = ctx.getProcessSidebar();
                    pipelineView.load(ctx.getProcessDefinition());
                });
            }
        }
    }

    private void deleteProcessFromSideBar(ProcessDefinition processDefinition){
        String id = processDefinition.getProcessId();
        if (contextMap.containsKey(id)) {
            LocalProcessContext ctx = contextMap.get(id);
            invokeLater(() -> {
                localClientView.removeSidebarItem(ctx.getProcessSidebar());
            });
            contextMap.remove(id);
            setSearchDatasource();
        }
    }

    private void setSearchDatasource(){
        localClientView.getTxtSearch().setDataSource(
                new ArrayList<>(
                        contextMap.values()
                                .stream()
                                .map(LocalProcessContext::getProcessSidebar)
                                .toList()
                )
        );
    }



    private void registerProcess(ProcessDefinition process) {
        ProcessSidebarItem item = localClientView.addProcessSidebarItem(process, false);
        LocalProcessContext context = new LocalProcessContext(pluginContext, item, process);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSidebarItemClick(item, process);
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

        updateContextState(context);
        contextMap.put(process.getProcessId(), context);
    }

    private void showContextMenu(MouseEvent e, LocalProcessContext ctx) {
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
                restartProcess(ctx);
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


    private void startProcess(LocalProcessContext ctx) {
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        ProcessEngine processEngine = processOrchestrator.getEngine();
        mainFrameWindow.runOnUi((t) -> {
            ctx.clearLog();
            JPanel monitorPanel = ctx.getMonitorPanel();
            mainFrameWindow.requireUtilityPanel(PanelPosition.BOTTOM, monitorPanel, 350, 0);
        });

        try{
            processEngine.start(ctx.getProcessDefinitionId(), executor -> {
                ctx.setProcessExecutor(executor);
                executor.addOutputListener(this::onProcessOutputListener);
                executor.addErrorListener(this::onProcessErrorListener);
                executor.addUserNotifyListener(this::onProcessUserNotifyListener);
                invokeLater(() -> ctx.setRunning(true));
            });
        }catch (StartProcessException | ProcessNotFoundException startProcessException){
            log.error("erro ao iniciar processo", startProcessException);
            throw new DisplayException("Erro ao iniciar o processo", startProcessException);
        }
    }

    private void restartProcess(LocalProcessContext ctx){
        ctx.clearLog();
        mainFrameWindow.runOnUi((t) -> {
            ctx.clearLog();
            JPanel monitorPanel = ctx.getMonitorPanel();
            mainFrameWindow.requireUtilityPanel(PanelPosition.BOTTOM, monitorPanel, 350, 0);
        });


        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        ProcessEngine processEngine = processOrchestrator.getEngine();
        try{
            processEngine.restart(ctx.getProcessDefinitionId());
        }catch (ProcessNotFoundException | StartProcessException e){
            throw new DisplayException("Erro ao iniciar o processo", e);
        }
    }

    private void stopProcess(LocalProcessContext ctx){
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        ProcessEngine processEngine = processOrchestrator.getEngine();

        try{
            int option = ModernDialog.builder()
                    .type(ModernDialog.Type.INFO)
                    .accentColor(new Color(59, 130, 246))
                    .title("Parar o processo")
                    .message("Tem certeza que deseja interromper este processo? Essa ação pode interromper tarefas em andamento e não poderá ser desfeita.")
                    .option("Cancelar", 0)
                    .option("Parar processo", 1, new Color(220, 53, 69), Color.WHITE)
                    .show();

            if(option == 1){
                ProcessExecutor processExecutor = processEngine.stop(ctx.getProcessDefinitionId());
                ctx.setProcessExecutor(processExecutor);
            }
        }catch (ProcessNotFoundException e){
            throw new DisplayException("Erro ao parar o processo", e);
        }
    }

    private void showTerminalMonitor(LocalProcessContext ctx){
        mainFrameWindow.runOnUi((t) -> {
            JPanel monitorPanel = ctx.getMonitorPanel();
            mainFrameWindow.requireUtilityPanel(PanelPosition.BOTTOM, monitorPanel, 350, 0);
        });
    }


    private void updateContextState(LocalProcessContext ctx) {
        ProcessOrchestratorManager processOrchestrator = processOrchestratorRef.get();
        ProcessEngine processEngine = processOrchestrator.getEngine();
        boolean isRunning = processEngine.isRunning(ctx.getProcessDefinitionId());
        if (isRunning) {
            ProcessExecutor executor = processEngine.getProcess(ctx.getProcessDefinitionId());
            ctx.setProcessExecutor(executor);
        }else {
            ctx.setProcessExecutor(null);
        }

        invokeLater(() -> ctx.getProcessSidebar().setRunning(isRunning));
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

    private void onProcessOutputListener(ProcessOutputEvent event) {
        ProcessDefinition processDefinition = event.getMainProcessDefinition();
        LocalProcessContext ctx = contextMap.get(processDefinition.getProcessId());

        if (ctx != null) {
            writeExecutor.submit(() -> {
                ctx.appendOutput(event.getProcessOutput());
            });
        }
    }

    private void onProcessUserNotifyListener(ProcessOutputEvent event){
        try{
            String content = event.getProcessOutput();
            UserNotificationContent userNotificationContent = Constants.OBJECT_MAPPER.readValue(content, UserNotificationContent.class);
            NotificationManager.startNotification(new UserNotification(userNotificationContent.getTitle(), userNotificationContent.getContent(), this::onNotificationClick), 30, TimeUnit.SECONDS);
        }catch (Exception ignored){}
    }

    private void onProcessErrorListener(ProcessDefinition processDefinition, Throwable error){
        LocalProcessContext ctx = contextMap.get(processDefinition.getProcessId());

        if (ctx != null) {
            writeExecutor.submit(() -> {
                StringWriter sw = new java.io.StringWriter();
                PrintWriter pw = new java.io.PrintWriter(sw);
                error.printStackTrace(pw);
                String stackTrace = sw.toString();
                ctx.appendOutput("[ERROR] "+stackTrace);
            });
        }
    }

    private void onNotificationClick(){
        if(mainFrameWindow != null && mainFrameWindow.isDisplayable()){
            mainFrameWindow.runOnUi((w) -> {
                mainFrameWindow.setVisible(true);
                mainFrameWindow.setAlwaysOnTop(true);
                mainFrameWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
                mainFrameWindow.toFront();
                mainFrameWindow.requestFocus();
                mainFrameWindow.setAlwaysOnTop(false);
            });
        }
    }

}
