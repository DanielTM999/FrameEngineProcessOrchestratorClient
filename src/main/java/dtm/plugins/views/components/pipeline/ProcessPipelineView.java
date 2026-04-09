package dtm.plugins.views.components.pipeline;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.plugins.models.ProcessNodeModel;
import dtm.plugins.models.enums.ProcessPhase;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProcessPipelineView extends JPanel {

    private Consumer<ProcessNodeModel> onNodeClick;
    private Consumer<ProcessNodeModel> onNodeEdit;
    private Consumer<ProcessNodeModel> onNodeDelete;
    private BiConsumer<ProcessPhase, ProcessNodeModel> onAddClick;
    private BiConsumer<ProcessNodeModel, List<ProcessNodeModel>> onDragEnd;
    private ProcessNodeCard selectedCard;

    @Getter
    private ProcessNodeModel current;

    private ZoomablePanel zoomPanel;

    public ProcessPipelineView() {
        setLayout(new BorderLayout());
        setOpaque(false);
    }

    public ProcessPipelineView onNodeClick(Consumer<ProcessNodeModel> callback) {
        this.onNodeClick = callback;
        return this;
    }

    public ProcessPipelineView onNodeEdit(Consumer<ProcessNodeModel> callback) {
        this.onNodeEdit = callback;
        return this;
    }

    public ProcessPipelineView onNodeDelete(Consumer<ProcessNodeModel> callback) {
        this.onNodeDelete = callback;
        return this;
    }

    public ProcessPipelineView onAddClick(BiConsumer<ProcessPhase, ProcessNodeModel> callback) {
        this.onAddClick = callback;
        return this;
    }

    public ProcessPipelineView onDragEnd(BiConsumer<ProcessNodeModel, List<ProcessNodeModel>> callback) {
        this.onDragEnd = callback;
        return this;
    }

    public void load(ProcessDefinition definition) {
        this.current = ProcessNodeModel.from(definition);
        removeAll();
        add(buildToolbar(current), BorderLayout.NORTH);
        add(buildScrollCanvas(current), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    public void reload() {
        if (current == null) return;
        removeAll();
        add(buildToolbar(current), BorderLayout.NORTH);
        add(buildScrollCanvas(current), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    public void clear() {
        this.current = null;
        removeAll();
        add(buildEmptyState(), BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void setNodeRunning(String processId, boolean running) {
        applyToNodeRecursive(this, processId, card -> {
            SwingUtilities.invokeLater(() -> card.setRunning(running));
        });
    }

    public void setNodeMonitoring(String processId, boolean monitoring) {
        applyToNodeRecursive(this, processId, card -> {
            SwingUtilities.invokeLater(() -> card.setMonitoring(monitoring));
        });
    }


    void selectCard(ProcessNodeCard card) {
        if (selectedCard != null && selectedCard != card) {
            selectedCard.setSelected(false);
        }
        selectedCard = card;
        card.setSelected(true);
    }

    private void applyToNodeRecursive(Container container, String processId, Consumer<ProcessNodeCard> action) {
        for (Component c : container.getComponents()) {
            if (c instanceof ProcessNodeCard card) {
                if (card.getData().getProcessId().equalsIgnoreCase(processId)) {
                    action.accept(card);
                    return;
                }
            }
            if (c instanceof Container child) {
                applyToNodeRecursive(child, processId, action);
            }
        }
    }

    private JPanel buildToolbar(ProcessNodeModel def) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);
        bar.setBorder(new FlatLineBorder(new Insets(0, 0, 1, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));
        bar.setPreferredSize(new Dimension(0, 36));

        bar.add(new JLabel(pipelineIcon()));

        JLabel name = new JLabel(def.getProcessName());
        name.putClientProperty(FlatClientProperties.STYLE,
                "font: bold 12; foreground: $Label.foreground");
        bar.add(name);

        JLabel sep = new JLabel("/");
        sep.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        bar.add(sep);

        JLabel id = new JLabel(def.getProcessId());
        id.setFont(UIManager.getFont("monospaced.font") != null
                ? UIManager.getFont("monospaced.font").deriveFont(10f)
                : id.getFont().deriveFont(10f));
        id.setForeground(UIManager.getColor("Label.disabledForeground"));
        id.setBackground(UIManager.getColor("Panel.background"));
        id.setOpaque(true);
        id.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                new EmptyBorder(0, 5, 0, 5)
        ));
        bar.add(id);

        JLabel type = new JLabel(def.getType());
        type.setFont(type.getFont().deriveFont(10f));
        type.setForeground(UIManager.getColor("Label.disabledForeground"));
        type.setBackground(UIManager.getColor("Panel.background"));
        type.setOpaque(true);
        type.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                new EmptyBorder(0, 6, 0, 6)
        ));
        bar.add(type);



        return bar;
    }

    private JScrollPane buildScrollCanvas(ProcessNodeModel def) {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        content.add(buildGrid(def, true), gbc);

        zoomPanel = new ZoomablePanel(content);

        JPanel sizer = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = zoomPanel.getPreferredSize();
                if (getParent() instanceof JViewport vp) {
                    d.width = Math.max(d.width, vp.getWidth());
                    d.height = Math.max(d.height, vp.getHeight());
                }
                return d;
            }
        };
        sizer.setOpaque(false);
        sizer.add(zoomPanel, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(sizer);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getHorizontalScrollBar().setUnitIncrement(14);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));

        installCanvasPan(sizer, scroll);
        installZoom(sizer);
        return scroll;
    }

    private void installZoom(JPanel canvas) {
        canvas.addMouseWheelListener(e -> {
            if (!e.isControlDown()) return;
            double delta = e.getWheelRotation() < 0 ? 0.1 : -0.1;
            zoomPanel.setZoom(zoomPanel.getZoom() + delta);
            canvas.revalidate();
            canvas.repaint();
            e.consume();
        });
    }

    private void installCanvasPan(JPanel canvas, JScrollPane scroll) {
        int[] dragStart = new int[2];
        int[] viewStart = new int[2];
        boolean[] panning = {false};

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (isOverNodeCard(canvas, e.getPoint())) return;
                dragStart[0] = e.getXOnScreen();
                dragStart[1] = e.getYOnScreen();
                viewStart[0] = scroll.getHorizontalScrollBar().getValue();
                viewStart[1] = scroll.getVerticalScrollBar().getValue();
                panning[0] = true;
                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                panning[0] = false;
                canvas.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!panning[0]) return;
                scroll.getHorizontalScrollBar().setValue(viewStart[0] + (dragStart[0] - e.getXOnScreen()));
                scroll.getVerticalScrollBar().setValue(viewStart[1] + (dragStart[1] - e.getYOnScreen()));
            }
        };

        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(ma);
    }

    private boolean isOverNodeCard(JPanel canvas, Point p) {
        Component c = SwingUtilities.getDeepestComponentAt(canvas, p.x, p.y);
        while (c != null && c != canvas) {
            if (c instanceof ProcessNodeCard) return true;
            c = c.getParent();
        }
        return false;
    }

    JPanel buildGrid(ProcessNodeModel def, boolean isRoot) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;
        g.weighty = 0;

        g.gridx = 0; g.weightx = 1;
        grid.add(buildPhaseColumn(ProcessPhase.BEFORE, def.getSubProcessesBefore(), def), g);

        g.gridx = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        grid.add(buildArrow(), g);

        g.gridx = 2; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        grid.add(buildMainColumn(def, isRoot), g);

        g.gridx = 3; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        grid.add(buildArrow(), g);

        g.gridx = 4; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        grid.add(buildPhaseColumn(ProcessPhase.AFTER, def.getSubProcessesAfter(), def), g);

        return grid;
    }

    private JPanel buildMainColumn(ProcessNodeModel def, boolean isRoot) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setBorder(new EmptyBorder(0, 4, 0, 4));

        col.add(phaseLabel("Principal", BLUE));
        col.add(new ProcessNodeCard(def, isRoot, false, this));
        return col;
    }

    private JPanel buildPhaseColumn(ProcessPhase phase,
                                    List<ProcessNodeModel> nodes,
                                    ProcessNodeModel parent) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setBorder(new EmptyBorder(0, 4, 0, 4));

        String label = phase == ProcessPhase.BEFORE ? "Antes" : "Depois";
        Color color = phase == ProcessPhase.BEFORE ? GREEN : AMBER;
        int count = nodes != null ? nodes.size() : 0;

        col.add(phaseLabel(label + " · " + count, color));

        if (nodes != null) {
            for (ProcessNodeModel node : nodes) {
                ProcessNodeCard card = new ProcessNodeCard(node, false, false, this);
                col.add(card);
                col.add(Box.createVerticalStrut(4));

                JPanel nested = buildGrid(node, false);
                nested.setAlignmentX(LEFT_ALIGNMENT);

                CollapsiblePanel collapsible = new CollapsiblePanel(nested, node.getProcessName());
                collapsible.setAlignmentX(LEFT_ALIGNMENT);
                col.add(collapsible);
                col.add(Box.createVerticalStrut(6));

                card.setCollapsible(collapsible);
                if (!node.hasSub()) {
                    if(!collapsible.isCollapsed()){
                        collapsible.toggle();
                    }
                }

            }
        }

        col.add(buildAddButton(phase, parent));
        return col;
    }

    private JButton buildAddButton(ProcessPhase phase, ProcessNodeModel parent) {
        JButton btn = new JButton("Adicionar");
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
        btn.setFocusable(false);
        btn.setIcon(addSmallIcon());
        btn.setBackground(null);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        btn.setForeground(UIManager.getColor("Label.disabledForeground"));
        btn.setFont(btn.getFont().deriveFont(10f));
        btn.addActionListener(e -> {
            if (onAddClick != null) onAddClick.accept(phase, parent);
        });
        return btn;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(new FlatLineBorder(new Insets(1, 0, 0, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));
        footer.setPreferredSize(new Dimension(0, 28));

        footer.add(legendItem(STOPPED, "Parado"));
        footer.add(legendItem(RUNNING, "Executando"));
        footer.add(legendItem(MONITORING, "Monitorando"));

        JLabel hint = new JLabel("Ctrl+Scroll zoom · arraste o fundo para navegar");
        hint.putClientProperty(FlatClientProperties.STYLE,
                "font: 9; foreground: $Label.disabledForeground");
        footer.add(hint);

        return footer;
    }

    private JPanel legendItem(Color dot, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);
        p.add(new DotComponent(dot, 5));
        JLabel lbl = new JLabel(text);
        lbl.putClientProperty(FlatClientProperties.STYLE,
                "font: 9; foreground: $Label.disabledForeground");
        p.add(lbl);
        return p;
    }

    private JPanel buildArrow() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Component.borderColor"));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int y = 34;
                g2.drawLine(4, y, getWidth() - 8, y);
                int ax = getWidth() - 8;
                g2.fillPolygon(new int[]{ax - 5, ax, ax - 5}, new int[]{y - 4, y, y + 4}, 3);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(36, 20));
        p.setMinimumSize(new Dimension(36, 20));
        p.setMaximumSize(new Dimension(36, Integer.MAX_VALUE));
        return p;
    }

    private JLabel phaseLabel(String text, Color color) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setForeground(color);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 9f));
        lbl.setBorder(new EmptyBorder(0, 2, 7, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        JLabel icon = new JLabel(emptyPipelineIcon());
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Nenhum processo selecionado");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.putClientProperty(FlatClientProperties.STYLE,
                "font: bold 14; foreground: $Label.disabledForeground");

        JLabel sub = new JLabel("Selecione um item na barra lateral para visualizar");
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.putClientProperty(FlatClientProperties.STYLE,
                "font: 12; foreground: $Label.disabledForeground");

        card.add(icon);
        card.add(Box.createVerticalStrut(12));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);

        empty.add(card);
        return empty;
    }

    void fireNodeClick(ProcessNodeModel def) { if (onNodeClick != null) onNodeClick.accept(def); }
    void fireNodeEdit(ProcessNodeModel def) { if (onNodeEdit != null) onNodeEdit.accept(def); }
    void fireNodeDelete(ProcessNodeModel def) { if (onNodeDelete != null) onNodeDelete.accept(def); }
    void fireDragEnd(List<ProcessNodeModel> newOrder, ProcessNodeModel def) { if (onDragEnd != null) onDragEnd.accept(def, newOrder); }

    static final Color BLUE = new Color(0x378ADD);
    static final Color GREEN = new Color(0x3B6D11);
    static final Color AMBER = new Color(0x854F0B);
    static final Color STOPPED = new Color(0x888780);
    static final Color RUNNING = new Color(0x52C474);
    static final Color MONITORING = new Color(0x4E9AFA);

    private Icon pipelineIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Component.accentColor"));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRect(x, y + 4, 4, 7);
                g2.drawRect(x + 5, y, 4, 15);
                g2.drawRect(x + 10, y + 4, 4, 7);
                g2.dispose();
            }
            public int getIconWidth() { return 15; }
            public int getIconHeight() { return 16; }
        };
    }

    private Icon addSmallIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(UIManager.getColor("Label.disabledForeground"));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 5, y + 1, x + 5, y + 9);
                g2.drawLine(x + 1, y + 5, x + 9, y + 5);
                g2.dispose();
            }
            public int getIconWidth() { return 11; }
            public int getIconHeight() { return 11; }
        };
    }

    private Icon emptyPipelineIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color col = UIManager.getColor("Label.disabledForeground");
                if (col == null) col = Color.GRAY;
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 80));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(x + 8, y + 12, 32, 24, 6, 6);
                g2.drawPolyline(
                        new int[]{x + 8, x + 8, x + 16, x + 20, x + 40},
                        new int[]{y + 18, y + 12, y + 12, y + 8, y + 8}, 5);
                g2.drawLine(x + 18, y + 24, x + 30, y + 24);
                g2.dispose();
            }
            public int getIconWidth()  { return 48; }
            public int getIconHeight() { return 44; }
        };
    }
}