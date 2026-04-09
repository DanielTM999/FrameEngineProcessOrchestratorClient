package dtm.plugins.views.components.pipeline;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.manager.process.definition.ProcessDefinition;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ProcessSidebarItem extends JPanel {

    @Getter
    private final String processId;

    @Getter
    private final String processName;

    private final JLabel nameLabel;
    private final JLabel     statusLabel;
    private final StatusDot  statusDot;

    private boolean selected;
    private boolean running;
    private boolean monitoring;

    private static final Color ACCENT_RUNNING    = new Color(78, 154, 250);
    private static final Color ACCENT_MONITORING = new Color(82, 196, 106);
    private static final Color ACCENT_STOPPED    = new Color(120, 120, 130);

    public ProcessSidebarItem(ProcessDefinition processDefinition, boolean isRunning) {
        this.running = isRunning;
        this.processId = processDefinition.getProcessId();
        this.processName = processDefinition.getProcessName();
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(new EmptyBorder(8, 10, 8, 12));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        LetterAvatar avatar = new LetterAvatar(processDefinition.getProcessName());
        add(avatar, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 1));
        text.setOpaque(false);

        nameLabel = new JLabel(processDefinition.getProcessName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        statusRow.setOpaque(false);

        statusDot = new StatusDot();
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
        statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        statusRow.add(statusDot);
        statusRow.add(statusLabel);

        text.add(nameLabel,  BorderLayout.CENTER);
        text.add(statusRow,  BorderLayout.SOUTH);

        add(text, BorderLayout.CENTER);
        updateUI_status();
    }


    public void setRunning(boolean running) {
        this.running = running;
        if (!running) this.monitoring = false;
        updateUI_status();
    }

    public void setMonitoring(boolean monitoring) {
        this.monitoring = monitoring;
        updateUI_status();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;

        if (selected) {
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
            nameLabel.setForeground(UIManager.getColor("List.selectionForeground"));
            statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
            statusLabel.setForeground(UIManager.getColor("List.selectionForeground"));
        } else {
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
            nameLabel.setForeground(UIManager.getColor("Label.foreground"));
            statusLabel.setFont(statusLabel.getFont().deriveFont(10f));
            statusLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }

        repaint();
    }

    public void setProcessName(String name) {
        nameLabel.setText(name);
        setToolTipText(name);
        revalidate();
        repaint();
    }

    public boolean isSelected() { return selected; }


    @Override
    protected void paintComponent(Graphics g) {
        if (selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIManager.getColor("List.selectionBackground"));
            g2.fill(new RoundRectangle2D.Float(3, 1, getWidth() - 6, getHeight() - 2, 10, 10));
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private void updateUI_status() {
        Color accent;
        String text;
        if (running && monitoring) {
            accent = ACCENT_MONITORING;
            text   = "Monitorando";
        } else if (running) {
            accent = ACCENT_RUNNING;
            text   = "Executando";
        } else {
            accent = ACCENT_STOPPED;
            text   = "Parado";
        }
        statusDot.setColor(accent);
        statusLabel.setText(text);
        repaint();
    }


    private static class StatusDot extends JComponent {
        private Color color = ACCENT_STOPPED;

        StatusDot() {
            setPreferredSize(new Dimension(7, 7));
        }

        void setColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(0, 0, 7, 7);
            g2.dispose();
        }
    }

    private static class LetterAvatar extends JComponent {
        private final String letter;
        private static final int SIZE = 34;

        LetterAvatar(String name) {
            this.letter = (name != null && !name.isEmpty())
                    ? name.substring(0, 1).toUpperCase() : "?";
            setPreferredSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int hue = (letter.charAt(0) * 37) % 360;
            Color bg = Color.getHSBColor(hue / 360f, 0.25f, 0.22f);
            Color fg = Color.getHSBColor(hue / 360f, 0.30f, 0.75f);

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, SIZE, SIZE, 10, 10);

            g2.setColor(fg);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            FontMetrics fm = g2.getFontMetrics();
            int x = (SIZE - fm.stringWidth(letter)) / 2;
            int y = ((SIZE - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(letter, x, y);

            g2.dispose();
        }
    }
}