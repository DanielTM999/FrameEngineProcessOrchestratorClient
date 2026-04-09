package dtm.plugins.views.components.pipeline;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CollapsiblePanel extends JPanel {

    private final JPanel content;

    @Getter
    private boolean collapsed = false;

    public CollapsiblePanel(JPanel content, String title) {
        this.content = content;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new NestedBorder());

        add(content);
    }

    public void toggle() {
        collapsed = !collapsed;
        content.setVisible(!collapsed);
        revalidate();
        repaint();
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        content.setVisible(!collapsed);
        revalidate();
        repaint();
    }

    private static class NestedBorder extends EmptyBorder {
        NestedBorder() { super(4, 14, 4, 0); }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color border = UIManager.getColor("Component.borderColor");
            g2.setColor(border != null ? border : new Color(0x3C3F41));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(x + 6, y + 4, x + 6, y + h - 4);
            g2.dispose();
        }
    }
}