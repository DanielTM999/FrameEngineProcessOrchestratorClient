package dtm.plugins.context;

import dtm.apps.core.extension.PluginContext;
import dtm.plugins.services.OutputTtyConnector;
import dtm.plugins.views.components.terminal.LnfTerminalSettingsProvider;
import dtm.plugins.views.components.terminal.TerminalViewPanel;
import dtm.plugins.views.components.terminal.TerminalWidget;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class BaseTerminalOutputContext {

    private static final String TS = "^((?:\\d{2,4}[/\\-]\\d{2}[/\\-]\\d{2,4}[T ])?\\d{2}:\\d{2}[:\\d.+\\-]*)\\s+";
    private static final String RST = "\033[0m";
    private static final String RED = "\033[31m";
    private static final String YEL = "\033[33m";
    private static final String CYA = "\033[36m";
    private static final String GRY = "\033[90m";

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
        connector.feed(colorizeLogLevel(text));
    }

    public void appendOutputLine(String text) {
        appendOutput(text + System.lineSeparator());
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

    private String colorizeLogLevel(String text) {
        if (text.contains("[DEBUG]") || text.contains("[TRACE]") ||
                text.matches(TS + "(DEBUG|TRACE)\\b.*")) {
            return GRY + text + RST;
        }

        text = text
                .replace("[ERROR]",   "[" + RED + "ERROR"   + RST + "]")
                .replace("[ERRO]",    "[" + RED + "ERRO"    + RST + "]")
                .replace("[SEVERE]",  "[" + RED + "SEVERE"  + RST + "]")
                .replace("[WARN]",    "[" + YEL + "WARN"    + RST + "]")
                .replace("[WARNING]", "[" + YEL + "WARNING" + RST + "]")
                .replace("[INFO]",    "[" + CYA + "INFO"    + RST + "]");

        text = text.replaceAll(TS + "(ERROR|ERRO|SEVERE)\\b",  "$1 " + RED + "$2" + RST);
        text = text.replaceAll(TS + "(WARN|WARNING)\\b",       "$1 " + YEL + "$2" + RST);
        text = text.replaceAll(TS + "(INFO)\\b",               "$1 " + CYA + "$2" + RST);

        return text;
    }
}
