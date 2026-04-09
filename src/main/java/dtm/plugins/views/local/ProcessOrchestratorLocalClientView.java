package dtm.plugins.views.local;

import dtm.plugins.controller.local.ProcessOrchestratorLocalController;
import dtm.plugins.views.BaseProcessView;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessOrchestratorLocalClientView extends BaseProcessView<ProcessOrchestratorLocalController> {

    private final ProcessOrchestratorLocalController controller;

    @Override
    protected ProcessOrchestratorLocalController newController() { return controller; }

    public void loadSession() {
        controller.loadSession();
    }
}