package dtm.plugins.views.components.pipeline;

import javax.swing.*;
import java.awt.*;

class DotComponent extends JComponent {
    private Color color;
    private final int size;

    DotComponent(Color color, int size) {
        this.color = color;
        this.size  = size;
        setPreferredSize(new Dimension(size + 2, size + 2));
    }

    void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(1, (getHeight() - size) / 2, size, size);
        g2.dispose();
    }
}