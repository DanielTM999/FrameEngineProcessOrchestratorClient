package dtm.plugins.views.remote;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.plugins.controller.remote.ProcessOrchestratorRemoteClientController;
import dtm.plugins.views.BaseProcessView;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;

@RequiredArgsConstructor
public class ProcessOrchestratorRemoteClientView extends BaseProcessView<ProcessOrchestratorRemoteClientController> {

    private final ProcessOrchestratorRemoteClientController controller;

    private JLabel timerLabel;

    @Override
    protected ProcessOrchestratorRemoteClientController newController() { return controller; }

    @Override
    protected void onDrawing() {
        super.onDrawing();

        JLabel sessionLabel = new JLabel("Sessão ativa");
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(11f));
        sessionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        timerLabel = new JLabel("00:00:00");
        timerLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        timerLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(sessionLabel);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(timerLabel);
        toolbar.add(Box.createHorizontalStrut(10));
    }

    public void loadSession() {
        controller.loadSession();
    }

    public void setSessionTime(String time) {
        if (timerLabel != null) timerLabel.setText(time);
    }
}