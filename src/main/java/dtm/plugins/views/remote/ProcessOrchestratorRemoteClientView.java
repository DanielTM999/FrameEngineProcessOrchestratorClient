package dtm.plugins.views.remote;

import dtm.plugins.controller.remote.ProcessOrchestratorRemoteClientController;
import dtm.plugins.views.BaseProcessView;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessOrchestratorRemoteClientView extends BaseProcessView<ProcessOrchestratorRemoteClientController> {

    private final ProcessOrchestratorRemoteClientController controller;

    @Override
    protected ProcessOrchestratorRemoteClientController newController() { return controller; }

    public void loadSession() {
        controller.loadSession();
    }
}
