package dtm.plugins.views.components.pipeline;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.plugins.models.ProcessNodeModel;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;

public class ProcessNodeCard extends JPanel {

    private static final int MIN_W = 160;
    private static final int ARC = 8;

    @Getter private final boolean isMain;
    @Getter private final ProcessNodeModel data;

    private final ProcessPipelineView root;

    private DotComponent statusDot;
    private JLabel statusLabel;

    private boolean running    = false;
    private boolean monitoring = false;

    private boolean selected;
    private Point dragOrigin;
    private boolean dragging;

    @Setter
    private CollapsiblePanel collapsible;

    private JButton expandBtn;

    public ProcessNodeCard(ProcessNodeModel data,
                           boolean isMain,
                           boolean selected,
                           ProcessPipelineView root) {
        this.data = data;
        this.isMain = isMain;
        this.selected = selected;
        this.root = root;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        add(buildHead());

        if (isMain) {
            add(buildPropsStrip());
        }

        setMinimumSize(new Dimension(MIN_W, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        installMouseHandlers(this);
    }

    public void setSelected(boolean sel) {
        this.selected = sel;
        repaint();
    }

    public void setRunning(boolean running) {
        this.running = running;
        updateStatusRow();
        repaint();
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
        updateStatusRow();
        repaint();
    }


    private void updateStatusRow() {
        if (statusDot == null || statusLabel == null) return;
        SwingUtilities.invokeLater(() -> {
            if (monitoring) {
                statusDot.setColor(ProcessPipelineView.MONITORING);
                statusLabel.setText("Monitorando");
            } else if (running) {
                statusDot.setColor(ProcessPipelineView.RUNNING);
                statusLabel.setText("Executando");
            } else {
                statusDot.setColor(ProcessPipelineView.STOPPED);
                statusLabel.setText("Parado");
            }
        });
    }

    private void installMouseHandlers(Component target) {
        if (target instanceof JButton) return;

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point inCard = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), ProcessNodeCard.this);
                MouseEvent converted = new MouseEvent(
                        ProcessNodeCard.this, e.getID(), e.getWhen(),
                        e.getModifiersEx(), inCard.x, inCard.y,
                        e.getClickCount(), e.isPopupTrigger(), e.getButton());
                handlePressed(converted);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point inCard = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), ProcessNodeCard.this);
                MouseEvent converted = new MouseEvent(
                        ProcessNodeCard.this, e.getID(), e.getWhen(),
                        e.getModifiersEx(), inCard.x, inCard.y,
                        e.getClickCount(), e.isPopupTrigger(), e.getButton());
                handleReleased(converted);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point inCard = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), ProcessNodeCard.this);
                MouseEvent converted = new MouseEvent(
                        ProcessNodeCard.this, e.getID(), e.getWhen(),
                        e.getModifiersEx(), inCard.x, inCard.y,
                        e.getClickCount(), e.isPopupTrigger(), e.getButton());
                handleDragged(converted);
            }
        };

        target.addMouseListener(ma);
        target.addMouseMotionListener(ma);

        if (target instanceof Container ct) {
            for (Component child : ct.getComponents()) {
                installMouseHandlers(child);
            }
        }
    }

    private void handlePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            showContextMenu(ProcessNodeCard.this, e.getX(), e.getY());
            return;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragOrigin = SwingUtilities.convertPoint(
                    ProcessNodeCard.this, e.getPoint(),
                    ProcessNodeCard.this.getParent());
            dragging = false;
        }
    }

    private void handleReleased(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        if (dragging) {
            dragging = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            java.util.List<ProcessNodeModel> newOrder = collectOrderFromParent();
            root.fireDragEnd(newOrder, data);
        } else {
            root.selectCard(ProcessNodeCard.this);
            if (e.getClickCount() == 2) {
                root.fireNodeClick(data);
            }
        }
    }

    private java.util.List<ProcessNodeModel> collectOrderFromParent() {
        java.util.List<ProcessNodeModel> order = new ArrayList<>();
        Container col = getParent();
        if (col != null) {
            for (Component c : col.getComponents()) {
                if (c instanceof ProcessNodeCard pc) {
                    order.add(pc.getData());
                }
            }
        }
        return order;
    }

    private void handleDragged(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        if (dragOrigin == null) return;

        Point current = SwingUtilities.convertPoint(
                ProcessNodeCard.this, e.getPoint(),
                ProcessNodeCard.this.getParent());

        if (!dragging && Math.abs(current.y - dragOrigin.y) > 6) {
            dragging = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        if (dragging) {
            reorderInParent(current.y);
        }
    }

    private void reorderInParent(int dragY) {
        Container col = getParent();
        if (col == null) return;

        java.util.List<ProcessNodeCard> cards = new java.util.ArrayList<>();
        for (Component c : col.getComponents()) {
            if (c instanceof ProcessNodeCard pc) cards.add(pc);
        }

        int myPos = cards.indexOf(this);
        if (myPos < 0 || cards.size() < 2) return;

        if (myPos > 0) {
            ProcessNodeCard above = cards.get(myPos - 1);
            int mid = above.getY() + above.getHeight() / 2;
            if (dragY < mid) {
                swapInContainer(col, this, above);
                return;
            }
        }

        if (myPos < cards.size() - 1) {
            ProcessNodeCard below = cards.get(myPos + 1);
            int mid = below.getY() + below.getHeight() / 2;
            if (dragY > mid) {
                swapInContainer(col, this, below);
                return;
            }
        }
    }

    private void swapInContainer(Container col, Component a, Component b) {
        Component[] comps = col.getComponents();
        int idxA = -1, idxB = -1;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == a) idxA = i;
            if (comps[i] == b) idxB = i;
        }
        if (idxA < 0 || idxB < 0) return;

        int lo = Math.min(idxA, idxB);
        int hi = Math.max(idxA, idxB);

        int endLo = blockEnd(comps, lo);
        int endHi = blockEnd(comps, hi);

        java.util.List<Component> blockHi = new ArrayList<>(Arrays.asList(comps).subList(hi, endHi + 1));
        java.util.List<Component> blockLo = new ArrayList<>(Arrays.asList(comps).subList(lo, endLo + 1));

        for (int i = endHi; i >= lo; i--) col.remove(i);

        int pos = lo;
        for (Component c : blockHi) col.add(c, pos++);
        for (Component c : blockLo) col.add(c, pos++);

        col.revalidate();
        col.repaint();
    }

    private int blockEnd(Component[] comps, int cardIdx) {
        for (int i = cardIdx + 1; i < comps.length; i++) {
            if (comps[i] instanceof ProcessNodeCard || comps[i] instanceof JButton) {
                return i - 1;
            }
        }
        return comps.length - 1;
    }

    private void showContextMenu(Component invoker, int x, int y) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem edit = new JMenuItem("Editar processo");
        edit.setIcon(editIcon());
        edit.addActionListener(e -> {
            root.selectCard(ProcessNodeCard.this);
            root.fireNodeEdit(data);
        });
        menu.add(edit);

        menu.addSeparator();

        JMenuItem delete = new JMenuItem("Excluir");
        delete.setIcon(deleteIcon());
        Color red = UIManager.getColor("Actions.Red");
        if (red != null) delete.setForeground(red);
        delete.addActionListener(e -> root.fireNodeDelete(data));
        menu.add(delete);

        menu.show(invoker, x, y);
    }

    private JPanel buildHead() {
        JPanel head = new JPanel(new BorderLayout(8, 0));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(8, 10, 8, 10));
        head.setAlignmentX(LEFT_ALIGNMENT);

        head.add(buildAvatar(), BorderLayout.WEST);
        head.add(buildInfo(), BorderLayout.CENTER);
        expandBtn = buildExpandBtn();
        head.add(expandBtn, BorderLayout.EAST);
        return head;
    }

    private JButton buildExpandBtn() {
        JButton btn = new JButton(chevronIcon(true));
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMinimumSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        btn.setBackground(null);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.addActionListener(e -> toggleCollapse());
        return btn;
    }

    private void toggleCollapse() {
        if (collapsible == null) return;
        collapsible.toggle();
        if (expandBtn != null) {
            expandBtn.setIcon(chevronIcon(!collapsible.isCollapsed()));
        }
    }

    private JComponent buildAvatar() {
        int hue = ((data.getProcessName().charAt(0) * 53) + (data.getProcessName().length() * 17)) % 360;
        Color bg = Color.getHSBColor(hue / 360f, 0.38f, 0.22f);
        Color fg = Color.getHSBColor(hue / 360f, 0.55f, 0.76f);
        String letter = data.getProcessName().substring(0, 1).toUpperCase();

        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, 28, 28, 7, 7);
                g2.setColor(fg);
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter,
                        (28 - fm.stringWidth(letter)) / 2,
                        (28 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(28, 28));
        avatar.setMinimumSize(new Dimension(28, 28));
        avatar.setMaximumSize(new Dimension(28, 28));
        return avatar;
    }

    private JPanel buildInfo() {
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(data.getProcessName());
        name.putClientProperty(FlatClientProperties.STYLE, "font: bold 11");
        name.setAlignmentX(LEFT_ALIGNMENT);
        info.add(name);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        statusDot   = new DotComponent(ProcessPipelineView.STOPPED, 5);
        statusLabel = new JLabel("Parado");
        statusLabel.putClientProperty(FlatClientProperties.STYLE,
                "font: 9; foreground: $Label.disabledForeground");

        row.add(statusDot);
        row.add(statusLabel);

        String idText = data.getProcessId();
        if (idText != null && idText.length() > 8) idText = idText.substring(0, 8);
        JLabel id = new JLabel(idText != null ? idText : "");
        id.setFont(UIManager.getFont("monospaced.font") != null
                ? UIManager.getFont("monospaced.font").deriveFont(9f)
                : id.getFont().deriveFont(9f));
        id.setForeground(UIManager.getColor("Label.disabledForeground"));
        row.add(id);

        info.add(row);
        return info;
    }

    private JPanel buildPropsStrip() {
        JPanel props = new JPanel(new GridLayout(0, 2, 0, 0));
        props.setOpaque(false);
        props.setBorder(new FlatStripBorder());
        props.setAlignmentX(LEFT_ALIGNMENT);

        addProp(props, "command", truncate(data.getCommand(), 22));
        addProp(props, "type", data.getType());
        addProp(props, "restartPolicy", data.getRestartPolicy() != null ? data.getRestartPolicy().name() : "null");
        addProp(props, "timeout", data.getStartTimeout() > 0 ? data.getStartTimeout() + "ms" : "null");
        addProp(props, "env", data.getExecutionEnvironment());
        addProp(props, "condition", truncate(data.getExecutionCondition() != null ? data.getExecutionCondition() : "null", 22));
        return props;
    }

    private void addProp(JPanel panel, String key, Object value) {
        String display;
        boolean nil;

        if (value instanceof String s && !s.isBlank() && !s.equals("null")) {
            display = truncate(s, 22);
            nil = false;
        } else {
            display = "null";
            nil = true;
        }

        JLabel k = new JLabel(key);
        k.setFont(k.getFont().deriveFont(9f));
        k.setForeground(UIManager.getColor("Label.disabledForeground"));
        k.setBorder(new EmptyBorder(2, 10, 2, 4));

        Font monoFont = UIManager.getFont("monospaced.font");
        JLabel v = new JLabel(display);
        v.setFont(monoFont != null ? monoFont.deriveFont(9f) : v.getFont().deriveFont(9f));
        v.setForeground(UIManager.getColor(nil ? "Label.disabledForeground" : "Label.foreground"));
        v.setBorder(new EmptyBorder(2, 0, 2, 10));

        panel.add(k);
        panel.add(v);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = selected
                ? UIManager.getColor("List.selectionBackground")
                : UIManager.getColor("Panel.background");

        Color border = selected
                ? new Color(0x378ADD)
                : monitoring
                  ? ProcessPipelineView.MONITORING
                  : running
                    ? ProcessPipelineView.RUNNING
                    : UIManager.getColor("Component.borderColor");

        float bw = (selected || running || monitoring) ? 1.5f : 0.5f;

        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC));
        g2.setColor(border);
        g2.setStroke(new BasicStroke(bw));
        g2.draw(new RoundRectangle2D.Float(bw / 2, bw / 2, getWidth() - bw, getHeight() - bw, ARC, ARC));

        g2.dispose();
    }

    private String truncate(Object o, int max) {
        if (o instanceof Boolean b) return b.toString();
        if (!(o instanceof String s)) return "null";
        if (s.isBlank() || s.equals("null")) return "null";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private Icon chevronIcon(boolean open) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.disabledForeground"));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (open) {
                    g2.drawLine(x + 2, y + 6, x + 5, y + 3);
                    g2.drawLine(x + 5, y + 3, x + 8, y + 6);
                } else {
                    g2.drawLine(x + 2, y + 3, x + 5, y + 6);
                    g2.drawLine(x + 5, y + 6, x + 8, y + 3);
                }
                g2.dispose();
            }
            public int getIconWidth() { return 10; }
            public int getIconHeight() { return 10; }
        };
    }

    private Icon editIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 2, y + 11, x + 4, y + 7);
                g2.drawLine(x + 4, y + 7, x + 9, y + 2);
                g2.drawLine(x + 9, y + 2, x + 11, y + 4);
                g2.drawLine(x + 11, y + 4, x + 6, y + 9);
                g2.drawLine(x + 6, y + 9, x + 2, y + 11);
                g2.dispose();
            }
            public int getIconWidth() { return 13; }
            public int getIconHeight() { return 13; }
        };
    }

    private Icon deleteIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color red = UIManager.getColor("Actions.Red");
                g2.setColor(red != null ? red : new Color(0xE24B4A));
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 3, y + 4, x + 3, y + 11);
                g2.drawLine(x + 10, y + 4, x + 10, y + 11);
                g2.drawLine(x + 3, y + 11, x + 10, y + 11);
                g2.drawLine(x + 1, y + 3, x + 12, y + 3);
                g2.drawLine(x + 5, y + 3, x + 5, y + 1);
                g2.drawLine(x + 5, y + 1, x + 8, y + 1);
                g2.drawLine(x + 8, y + 1, x + 8, y + 3);
                g2.dispose();
            }
            public int getIconWidth() { return 13; }
            public int getIconHeight() { return 13; }
        };
    }

    private static class FlatStripBorder extends EmptyBorder {
        FlatStripBorder() { super(6, 0, 6, 0); }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(UIManager.getColor("Component.borderColor"));
            g.drawLine(x, y, x + w, y);
        }
    }
}