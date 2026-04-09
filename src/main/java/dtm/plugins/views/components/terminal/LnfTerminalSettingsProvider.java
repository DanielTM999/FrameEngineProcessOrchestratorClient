package dtm.plugins.views.components.terminal;


import com.jediterm.terminal.TerminalColor;
import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class LnfTerminalSettingsProvider extends DefaultSettingsProvider {

    @NotNull
    @Override
    @SuppressWarnings("deprecation")
    public TextStyle getDefaultStyle() {
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");

        if (bg == null) bg = Color.BLACK;
        if (fg == null) fg = Color.WHITE;

        bg = bg.darker();

        TerminalColor terminalFg = new TerminalColor(fg.getRed(), fg.getGreen(), fg.getBlue());
        TerminalColor terminalBg = new TerminalColor(bg.getRed(), bg.getGreen(), bg.getBlue());

        return new TextStyle(terminalFg, terminalBg);
    }

    @Override
    public Font getTerminalFont() {
        return new Font("Consolas", Font.PLAIN, 14);
    }

    @Override
    public boolean scrollToBottomOnTyping() {
        return true;
    }

    @Override
    public boolean DECCompatibilityMode() {
        return true;
    }



}
