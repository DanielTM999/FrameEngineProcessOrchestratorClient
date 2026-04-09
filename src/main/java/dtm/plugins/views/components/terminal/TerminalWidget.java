package dtm.plugins.views.components.terminal;

import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class TerminalWidget extends JediTermWidget {

    protected JScrollBar myScrollBar;

    public TerminalWidget(@NotNull SettingsProvider settingsProvider) {
        super(settingsProvider);
    }

    public TerminalWidget(int columns, int lines, SettingsProvider settingsProvider) {
        super(columns, lines, settingsProvider);
    }

    @Override
    protected JScrollBar createScrollBar() {
        myScrollBar = new JScrollBar();
        myScrollBar.setPreferredSize(new Dimension(10, 0));
        myScrollBar.setUI(new BasicScrollBarUI() {

            @Override
            protected void configureScrollBarColors() {
                thumbColor = UIManager.getColor("ScrollBar.thumb");
                trackColor = UIManager.getColor("ScrollBar.track");
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return emptyButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return emptyButton();
            }

            private JButton emptyButton() {
                JButton btn = new JButton();
                btn.setPreferredSize(new Dimension(0, 0));
                btn.setMinimumSize(new Dimension(0, 0));
                btn.setMaximumSize(new Dimension(0, 0));
                return btn;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Color color = UIManager.getColor("ScrollBar.thumb");
                if (color == null) color = new Color(80, 80, 80);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(r.x + 1, r.y, r.width - 2, r.height, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                Color color = UIManager.getColor("ScrollBar.track");
                if (color == null) return;
                g.setColor(color);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        });

        return myScrollBar;
    }

    public JScrollBar getScrollBar() {
        return myScrollBar;
    }

    public void setScrollBarWidth(int width) {
        myScrollBar.setPreferredSize(new Dimension(width, 0));
        myScrollBar.revalidate();
    }
}
