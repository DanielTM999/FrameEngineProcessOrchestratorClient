package dtm.plugins.views.notification;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.plugins.models.remote.res.ServerInitialStateEvent;
import dtm.stools.activity.NotificationActivity;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
public class ServerStatusNotification extends NotificationActivity {

    private final ServerInitialStateEvent event;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    protected void onDrawing() {
        super.onDrawing();

        String state = event.getState() != null ? event.getState().toUpperCase() : "UNKNOWN";
        boolean isConnected = "CONNECTED".equals(state);

        String stateColor;
        String iconKey;
        String borderColor;

        if (isConnected) {
            stateColor = "$Component.accentColor";
            iconKey = "OptionPane.informationIcon";
            borderColor = "$Component.borderColor";
        } else {
            stateColor = "$Error.color";
            iconKey = "OptionPane.errorIcon";
            borderColor = "$Error.color";
        }

        setLayout(new BorderLayout(0, 0));

        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBorder(new EmptyBorder(8, 0, 8, 8));

        String style = String.format(
                "background: $Panel.secondaryBackground; border: 1,1,1,1, %s; arc: 12",
                borderColor
        );
        rootPanel.putClientProperty(FlatClientProperties.STYLE, style);


        JPanel statusStrip = new JPanel();
        statusStrip.setPreferredSize(new Dimension(6, 0));
        statusStrip.putClientProperty(FlatClientProperties.STYLE,
                "background: " + stateColor + "; arc: 2");

        rootPanel.add(statusStrip, BorderLayout.WEST);


        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();


        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 10);

        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(UIManager.getIcon(iconKey));
        iconLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: " + stateColor);
        contentPanel.add(iconLabel, gbc);


        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(2, 0, 0, 0);


        String timeStr = event.getTimestamp() != null
                ? event.getTimestamp().format(TIME_FORMATTER)
                : "--:--";

        String titleText = String.format("<html><b>Status: %s</b> <span style='color:gray; font-size:9px'>[%s]</span></html>",
                state,
                timeStr);

        JLabel lblTitle = new JLabel(titleText);
        contentPanel.add(lblTitle, gbc);

        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 2, 0);

        String msg = String.format("Sessão: %s (%s)",
                truncate(event.getSessionId(), 15),
                event.getTimestampLocal() != null ? event.getTimestampLocal() : "Global");

        JLabel lblMessage = new JLabel(msg);
        lblMessage.putClientProperty(FlatClientProperties.STYLE, "font: small; foreground: $Label.disabledForeground");
        contentPanel.add(lblMessage, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 5, 0, 0);

        JButton btnClose = new JButton("✕");
        btnClose.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btnClose.putClientProperty(FlatClientProperties.STYLE, "font: bold small");
        btnClose.setFocusable(false);
        btnClose.addActionListener(e -> dispose());

        contentPanel.add(btnClose, gbc);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        add(rootPanel);
    }

    private String truncate(String str, int len) {
        if (str == null) return "-";
        if (str.length() <= len) return str;
        return str.substring(0, len) + "...";
    }

    @Override
    protected void onClick() {
        dispose();
    }
}
