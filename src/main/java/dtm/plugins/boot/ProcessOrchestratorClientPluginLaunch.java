package dtm.plugins.boot;

import dtm.apps.annotations.PluginReference;
import dtm.apps.core.ApplicationProperties;
import dtm.apps.core.ApplicationPropertiesManager;
import dtm.apps.core.extension.PluginMemory;
import dtm.apps.core.extension.WindowPluginAdapter;
import dtm.apps.core.extension.WindowPluginContext;
import dtm.apps.exceptions.DisplayException;
import dtm.apps.models.enums.PanelPosition;
import dtm.apps.services.listeners.Listener;
import dtm.apps.services.listeners.impl.ListenerStorage;
import dtm.apps.views.MainFrameWindow;
import dtm.di.annotations.aop.DisableAop;
import dtm.manager.ProcessOrchestratorManager;
import dtm.plugins.context.LockContext;
import dtm.plugins.controller.ProcessOrchestratorConnectionManagerController;
import dtm.plugins.controller.local.ProcessOrchestratorLocalController;
import dtm.plugins.controller.remote.ProcessOrchestratorRemoteClientController;
import dtm.plugins.models.Constants;
import dtm.plugins.views.ProcessOrchestratorConnectionManagerView;
import dtm.plugins.views.local.ProcessOrchestratorLocalClientView;
import dtm.plugins.views.remote.ProcessOrchestratorRemoteClientView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@DisableAop
@RequiredArgsConstructor
@PluginReference(id = "ProcessOrchestratorClientPluginLaunch", singleton = true)
public class ProcessOrchestratorClientPluginLaunch extends WindowPluginAdapter {

    public static final Listener<Runnable> logoutListener = new ListenerStorage<>();

    private final AtomicBoolean isSelfContext = new AtomicBoolean(true);
    private final ApplicationPropertiesManager applicationPropertiesManager;
    private final Map<Class<?>, String> windowsContextsSet = new ConcurrentHashMap<>();

    @Override
    public void onInit() {
        addToTopMenu();
    }

    @Override
    public void onCreatedContext(WindowPluginContext context) {
        Component contextWindow = context.getComponent();
        if (contextWindow instanceof JPanel panel) {
            windowsContextsSet.put(panel.getClass(), context.getContextKey());
        }
    }

    @Override
    public void onContextChange(WindowPluginContext context) {
        super.onContextChange(context);
        MainFrameWindow frameWindow = getContextWindowAs(MainFrameWindow.class);
        isSelfContext.set(context.isPluginContext());
        if(context.getComponent() instanceof ProcessOrchestratorLocalClientView processOrchestratorLocalClientView) {
            processOrchestratorLocalClientView.loadSession();
        }else if(context.getComponent() instanceof ProcessOrchestratorRemoteClientView processOrchestratorRemoteClientView) {
            processOrchestratorRemoteClientView.loadSession();
        }

        if(!context.isPluginContext()){
            frameWindow.closeUtilityPanelIf(PanelPosition.RIGHT, ProcessOrchestratorClientPluginLaunch.class.getClassLoader());
            frameWindow.closeUtilityPanelIf(PanelPosition.BOTTOM, ProcessOrchestratorClientPluginLaunch.class.getClassLoader());
        }
    }

    @Override
    public void onDestroy() {
        shutdown();
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    public Collection<JPanel> createContexts() {
        return new ArrayList<>() {{
            add(createInitialView());
            add(createProcessOrchestratorLocalClientView());
            add(createProcessOrchestratorRemoteClientView());
        }};
    }


    private void addToTopMenu(){
        MainFrameWindow mainFrameWindow = getContextWindowAs(MainFrameWindow.class);

        JMenu sshMenu = new JMenu("Process Orchestrator");

        JMenuItem openPanelItem = new JMenuItem("Abrir Conexão/Painel");
        openPanelItem.addActionListener(e -> handleSidebarAction());
        sshMenu.add(openPanelItem);

        sshMenu.addSeparator();

        JMenuItem logoutItem = new JMenuItem("Sair/Desconectar");
        logoutItem.addActionListener(e -> logoutSession());
        sshMenu.add(logoutItem);

        runOnUIThread(w -> {
            mainFrameWindow.addMenu(sshMenu);
        });
    }

    private void handleSidebarAction() {
        String contextKey = null;
        PluginMemory pluginMemory = getMemory(LockContext.INSTANCE);
        if (isSessionInitialized()) {

            String sessionType = pluginMemory.get("SessionType");

            if("local".equals(sessionType)){
                contextKey = windowsContextsSet.get(ProcessOrchestratorLocalClientView.class);
            }else if("remote".equals(sessionType)){
                contextKey = windowsContextsSet.get(ProcessOrchestratorRemoteClientView.class);
            }else{
                throw new DisplayException("Sem sessão registrada");
            }

        }else{
            contextKey = windowsContextsSet.get(ProcessOrchestratorConnectionManagerView.class);
        }

        if (contextKey != null) {
            requireContext(contextKey);
        } else {
            log.warn("Painel ProcessOrchestrator ainda não foi carregado.");
        }
    }


    private boolean isSessionInitialized(){
        PluginMemory pluginMemory = getMemory(LockContext.INSTANCE);
        return pluginMemory.contains("SessionType");
    }

    private JPanel createInitialView() {
        ApplicationProperties settingsProps = applicationPropertiesManager.getProperties("ProcessOrchestratorSettings");
        Runnable onSessionCreated = this::handleSidebarAction;
        return new ProcessOrchestratorConnectionManagerView(
                newInstance(
                        ProcessOrchestratorConnectionManagerController.class,
                        settingsProps,
                        this,
                        onSessionCreated
                )
        );
    }


    private JPanel createProcessOrchestratorRemoteClientView(){
        ApplicationProperties settingsProps = applicationPropertiesManager.getProperties("ProcessOrchestratorSettings");
        Runnable logoutAction = this::logoutSession;

        return new ProcessOrchestratorRemoteClientView(
                newInstance(
                        ProcessOrchestratorRemoteClientController.class,
                        settingsProps,
                        this,
                        logoutAction
                )
        );
    }

    private JPanel createProcessOrchestratorLocalClientView() {
        ApplicationProperties settingsProps = applicationPropertiesManager.getProperties("ProcessOrchestratorSettings");
        Runnable logoutAction = this::logoutSession;

        return new ProcessOrchestratorLocalClientView(
                newInstance(
                        ProcessOrchestratorLocalController.class,
                        settingsProps,
                        this,
                        logoutAction
                )
        );
    }

    private void logoutSession(){
        PluginMemory pluginMemory = getMemory(LockContext.INSTANCE);
        pluginMemory.remove("SessionType");
        pluginMemory.remove(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY);
        pluginMemory.remove(Constants.PROCESS_ORCHESTRATOR_REMOTE_KEY);

        logoutListener.notifyAllListeners(
                Runnable::run,
                throwable -> log.error("logoutSession Listnner error", throwable)
        );
        if(isSelfContext.get()){
            requireContext(windowsContextsSet.get(ProcessOrchestratorConnectionManagerView.class));
        }
    }

    private void shutdown(){
        PluginMemory pluginMemory = getMemory(LockContext.INSTANCE);

        if(pluginMemory.contains(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY)){
            Object object = pluginMemory.get(Constants.PROCESS_ORCHESTRATOR_LOCAL_KEY);
            if(object instanceof ProcessOrchestratorManager processOrchestratorManager){
                processOrchestratorManager.shutdown();
            }
        }

        pluginMemory.clear();
        getMemory().clear();
    }
}
