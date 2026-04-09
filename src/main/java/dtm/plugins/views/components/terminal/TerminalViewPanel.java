package dtm.plugins.views.components.terminal;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import dtm.apps.core.extension.PluginContext;
import dtm.apps.models.enums.PanelPosition;
import dtm.apps.views.MainFrameWindow;
import dtm.plugins.services.OutputTtyConnector;
import dtm.stools.component.panels.BlockingPanel;

import javax.swing.*;
import java.awt.*;

public class TerminalViewPanel extends BlockingPanel {

    private final PluginContext pluginContext;
    private final OutputTtyConnector connector;
    private final TerminalWidget widget;

    public TerminalViewPanel(
            PluginContext pluginContext,
            OutputTtyConnector outputTtyConnector,
            TerminalWidget widget
    ) {
        this.pluginContext = pluginContext;
        this.connector = new OutputTtyConnector();
        this.widget = widget;
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(widget, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new FlatLineBorder(new Insets(0, 0, 1, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));
        bar.setPreferredSize(new Dimension(0, 28));

        JLabel lbl = new JLabel("Terminal");
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        lbl.putClientProperty(FlatClientProperties.STYLE,
                "font: 10; foreground: $Label.disabledForeground");

        JButton btnClose = new JButton(closeIcon());
        btnClose.setPreferredSize(new Dimension(26, 26));
        btnClose.setFocusable(false);
        btnClose.putClientProperty(FlatClientProperties.STYLE,
                "background: null; borderWidth: 0; arc: 4; margin: 0,0,0,0");
        btnClose.addActionListener(e -> {
            close();
        });

        bar.add(lbl,      BorderLayout.WEST);
        bar.add(btnClose, BorderLayout.EAST);
        return bar;
    }

    private Icon closeIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.disabledForeground"));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 3, y + 3, x + 9, y + 9);
                g2.drawLine(x + 9, y + 3, x + 3, y + 9);
                g2.dispose();
            }
            public int getIconWidth()  { return 12; }
            public int getIconHeight() { return 12; }
        };
    }

    private void close(){
        pluginContext.getContextWindowAs(MainFrameWindow.class).closeUtilityPanels(PanelPosition.BOTTOM);
    }
}
