package dtm.plugins.views.components.pipeline;

import javax.swing.*;
import java.awt.*;

class ZoomablePanel extends JPanel {

    private final JPanel content;
    private double zoom = 1.0;

    ZoomablePanel(JPanel content) {
        super(null);
        setOpaque(false);
        this.content = content;
        add(content);
    }

    double getZoom() {
        return zoom;
    }

    void setZoom(double zoom) {
        this.zoom = Math.max(0.3, Math.min(2.0, zoom));
        revalidate();
        repaint();
    }

    @Override
    public void doLayout() {
        Dimension natural = content.getPreferredSize();
        content.setBounds(0, 0, natural.width, natural.height);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = content.getPreferredSize();
        return new Dimension(
                (int) (d.width * zoom),
                (int) (d.height * zoom));
    }

    @Override
    protected void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(zoom, zoom);
        super.paintChildren(g2);
        g2.dispose();
    }

    @Override
    public Component findComponentAt(int x, int y) {
        return super.findComponentAt(
                (int) (x / zoom),
                (int) (y / zoom));
    }

    @Override
    public Component getComponentAt(int x, int y) {
        return super.getComponentAt(
                (int) (x / zoom),
                (int) (y / zoom));
    }
}