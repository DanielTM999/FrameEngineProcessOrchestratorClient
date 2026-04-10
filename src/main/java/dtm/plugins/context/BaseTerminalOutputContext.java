package dtm.plugins.context;

import dtm.apps.core.extension.PluginContext;
import dtm.plugins.services.OutputTtyConnector;
import dtm.plugins.services.utils.LogColorizer;
import dtm.plugins.views.components.terminal.LnfTerminalSettingsProvider;
import dtm.plugins.views.components.terminal.TerminalViewPanel;
import dtm.plugins.views.components.terminal.TerminalWidget;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class BaseTerminalOutputContext {

    @Getter
    protected final StringBuffer outputBuffer;

    @Getter
    protected final JPanel monitorPanel;

    protected final TerminalWidget terminalWidget;

    protected final OutputTtyConnector connector;

    public BaseTerminalOutputContext(PluginContext pluginContext) {
        this.outputBuffer = new StringBuffer();
        this.connector = new OutputTtyConnector();
        this.terminalWidget = createTerminalWidget();
        this.monitorPanel = createMonitorPanel(pluginContext);
    }


    public void appendOutput(String text) {
        outputBuffer.append(text);
        connector.feed(LogColorizer.colorize(text));
    }

    public void appendOutputLine(String text) {
        appendOutput(text + "\r"+System.lineSeparator());
    }

    public void clearLog() {
        outputBuffer.setLength(0);
        SwingUtilities.invokeLater(() -> terminalWidget.getTerminal().reset(true));
    }

    private TerminalWidget createTerminalWidget() {
        TerminalWidget widget = new TerminalWidget(new LnfTerminalSettingsProvider());
        widget.setTtyConnector(connector);
        widget.start();
        return widget;
    }

    private TerminalViewPanel createMonitorPanel(PluginContext pluginContext) {
        return new TerminalViewPanel(pluginContext, connector, terminalWidget);
    }

}
