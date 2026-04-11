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
import java.util.function.Consumer;

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

    public void setOnUserInput(Consumer<String> action){
        connector.setOnUserInput(action);
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
        installUrlClickListener(widget);
        return widget;
    }

    private TerminalViewPanel createMonitorPanel(PluginContext pluginContext) {
        return new TerminalViewPanel(pluginContext, connector, terminalWidget);
    }

    private void installUrlClickListener(TerminalWidget widget) {
        JComponent panel = widget.getTerminalPanel();

        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
                "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (!(event instanceof java.awt.event.MouseEvent me)) return;
            if (me.getSource() != panel) return;

            if (me.getID() == java.awt.event.MouseEvent.MOUSE_CLICKED && me.isControlDown()) {
                String line = getLineAtY(widget, me.getY());
                if (line == null) return;
                int charX = getCharX(widget, me.getX());
                java.util.regex.Matcher m = urlPattern.matcher(line);
                while (m.find()) {
                    if (charX >= m.start() && charX <= m.end()) {
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(m.group()));
                        } catch (Exception ignored) {}
                        return;
                    }
                }
            }

            if (me.getID() == java.awt.event.MouseEvent.MOUSE_MOVED) {
                if (!me.isControlDown()) {
                    panel.setCursor(Cursor.getDefaultCursor());
                    return;
                }
                String line = getLineAtY(widget, me.getY());
                if (line == null) return;
                int charX = getCharX(widget, me.getX());
                java.util.regex.Matcher m = urlPattern.matcher(line);
                boolean onUrl = false;
                while (m.find()) {
                    if (charX >= m.start() && charX <= m.end()) {
                        onUrl = true;
                        break;
                    }
                }
                panel.setCursor(onUrl
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }

        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private String getLineAtY(TerminalWidget widget, int y) {
        try {
            com.jediterm.terminal.model.TerminalTextBuffer buf = widget.getTerminalTextBuffer();
            java.awt.Dimension charSize = widget.getTerminalPanel().getPreferredSize();
            int cellHeight = charSize.height / (buf.getHeight());
            if (cellHeight <= 0) return null;
            int row = y / cellHeight;
            if (row < 0 || row >= buf.getHeight()) return null;
            return buf.getLine(row).getText();
        } catch (Exception e) {
            return null;
        }
    }

    private int getCharX(TerminalWidget widget, int x) {
        try {
            com.jediterm.terminal.model.TerminalTextBuffer buf = widget.getTerminalTextBuffer();
            java.awt.Dimension charSize = widget.getTerminalPanel().getPreferredSize();
            int cellWidth = charSize.width / buf.getWidth();
            return cellWidth > 0 ? x / cellWidth : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}
