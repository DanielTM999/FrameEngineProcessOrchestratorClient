package dtm.plugins.views.notification;


import com.formdev.flatlaf.FlatClientProperties;
import dtm.stools.activity.NotificationActivity;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UserNotification extends NotificationActivity {

    private final String title;
    private final String content;
    private final Runnable onClick;

    public UserNotification(String title, String content, Runnable onClick) {
        this.title = title;
        this.content = content;
        this.onClick = onClick;
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();

        setLayout(new BorderLayout(0, 0));

        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        rootPanel.putClientProperty(FlatClientProperties.STYLE,
                "background: $Panel.secondaryBackground; border: 1,1,1,1, $Component.borderColor; arc: 12");

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(2, 0, 2, 0);

        JLabel lblTitle = new JLabel("<html><b>" + (title != null ? title : "Notificação") + "</b></html>");
        contentPanel.add(lblTitle, gbc);


        gbc.gridy = 1;
        JLabel lblContent = new JLabel((content != null ? content : ""));
        lblContent.putClientProperty(FlatClientProperties.STYLE, "font: small; foreground: $Label.disabledForeground");
        contentPanel.add(lblContent, gbc);

        gbc.gridx = 1;
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

    @Override
    protected void onClick() {
        if(onClick != null){
            onClick.run();
        }
        dispose();
    }
}
