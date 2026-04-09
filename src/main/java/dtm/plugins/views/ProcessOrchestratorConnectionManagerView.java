package dtm.plugins.views;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.plugins.controller.ProcessOrchestratorConnectionManagerController;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;
import dtm.plugins.views.remote.components.form.ConnectionFormPanel;
import dtm.stools.component.delegated.DelegatedBlockingPanel;
import dtm.stools.component.panels.KeyPanel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public class ProcessOrchestratorConnectionManagerView extends DelegatedBlockingPanel<ProcessOrchestratorConnectionManagerController> {

    private final ProcessOrchestratorConnectionManagerController controller;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private JPanel serverListContainer;

    public static final String VIEW_ENVIRONMENTS = "viewEnvironments";
    public static final String VIEW_SETTINGS = "viewSettings";

    @Override
    protected ProcessOrchestratorConnectionManagerController newController() {
        return controller;
    }

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout());
        cardPanel.add(createEnvironmentsPanel(), VIEW_ENVIRONMENTS);
        cardPanel.add(createSettingsPanel(), VIEW_SETTINGS);
        cardLayout.show(cardPanel, VIEW_ENVIRONMENTS);
        add(createSidebar(), BorderLayout.WEST);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")));
        sidebar.setBackground(UIManager.getColor("Panel.background"));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 25));
        header.setOpaque(false);
        JLabel lblLogo = new JLabel("Orchestrator");
        lblLogo.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");
        header.add(lblLogo);
        sidebar.add(header, BorderLayout.NORTH);

        JPanel menuContainer = new JPanel();
        menuContainer.setLayout(new BoxLayout(menuContainer, BoxLayout.Y_AXIS));
        menuContainer.setOpaque(false);
        menuContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        ButtonGroup menuGroup = new ButtonGroup();

        JToggleButton btnEnv = createMenuButton("Ambientes", "FileView.computerIcon");
        btnEnv.setSelected(true);
        btnEnv.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED)
                cardLayout.show(cardPanel, VIEW_ENVIRONMENTS);
        });
        menuGroup.add(btnEnv);
        menuContainer.add(btnEnv);
        menuContainer.add(Box.createVerticalStrut(5));

        JToggleButton btnConf = createMenuButton("Configurações", "FileView.directoryIcon");
        btnConf.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED)
                cardLayout.show(cardPanel, VIEW_SETTINGS);
        });
        menuGroup.add(btnConf);
        menuContainer.add(btnConf);

        sidebar.add(menuContainer, BorderLayout.CENTER);

        JLabel lblVersion = new JLabel("v1.0.0");
        lblVersion.setBorder(new EmptyBorder(15, 20, 15, 20));
        lblVersion.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground; font: -2");
        sidebar.add(lblVersion, BorderLayout.SOUTH);

        return sidebar;
    }

    private JToggleButton createMenuButton(String text, String iconKey) {
        JToggleButton btn = new JToggleButton(text);
        btn.setIcon(UIManager.getIcon(iconKey));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.STYLE,
                "font: bold;" +
                        "arc: 10;" +
                        "margin: 8,15,8,15;" +
                        "background: null;" +
                        "borderWidth: 0;" +
                        "focusWidth: 0;" +
                        "innerFocusWidth: 0;" +
                        "selectedBackground: $Component.accentColor;" +
                        "selectedForeground: #ffffff");
        return btn;
    }

    private JPanel createEnvironmentsPanel() {
        JPanel scrollContent = new JPanel(new GridBagLayout());
        scrollContent.setBorder(new EmptyBorder(30, 40, 30, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 20, 0);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Selecionar Ambiente");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: +6 bold");
        JLabel lblSub = new JLabel("Escolha onde os processos de automação serão executados.");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "font: +0; foreground: $Label.disabledForeground");
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);
        scrollContent.add(headerPanel, gbc);

        gbc.gridy++;
        scrollContent.add(new JSeparator(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(10, 0, 10, 0);
        JLabel lblLocal = new JLabel("Ambiente Local");
        lblLocal.putClientProperty(FlatClientProperties.STYLE, "font: +2 bold");
        scrollContent.add(lblLocal, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 30, 0);
        scrollContent.add(createLocalArea(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 15, 0);
        JPanel remoteHeader = new JPanel(new BorderLayout());
        remoteHeader.setOpaque(false);
        JLabel lblRemote = new JLabel("Conexões Remotas");
        lblRemote.putClientProperty(FlatClientProperties.STYLE, "font: +2 bold");
        JButton btnAdd = new JButton("Nova Conexão");
        btnAdd.addActionListener(l -> showConnectionForm(null));
        btnAdd.setName(Id.BTN_ADD_SERVER);
        btnAdd.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        remoteHeader.add(lblRemote, BorderLayout.WEST);
        remoteHeader.add(btnAdd, BorderLayout.EAST);
        scrollContent.add(remoteHeader, gbc);

        gbc.gridy++;
        serverListContainer = new JPanel(new GridLayout(0, 2, 20, 20));
        serverListContainer.setName(Id.PNL_SERVER_LIST);
        serverListContainer.setOpaque(false);
        scrollContent.add(serverListContainer, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        scrollContent.add(new JPanel(), gbc);

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private Component createLocalArea() {
        JButton btnLocal = new JButton();
        btnLocal.setName(Id.BTN_LOCAL_CONNECT);
        btnLocal.setLayout(new BorderLayout());
        btnLocal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLocal.setPreferredSize(new Dimension(0, 90));
        btnLocal.putClientProperty(FlatClientProperties.STYLE,
                "arc: 15;" +
                        "margin: 10,20,10,20;" +
                        "background: $Panel.background;" +
                        "borderWidth: 1;" +
                        "borderColor: $Component.borderColor");

        JLabel iconLabel = new JLabel(UIManager.getIcon("FileView.computerIcon"));
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 20));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel lblName = new JLabel("Este Computador (Localhost)");
        lblName.putClientProperty(FlatClientProperties.STYLE, "font: bold +2");

        JLabel lblStatus = new JLabel("Execução direta usando recursos locais");
        lblStatus.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");

        textPanel.add(lblName);
        textPanel.add(lblStatus);

        btnLocal.add(iconLabel, BorderLayout.WEST);
        btnLocal.add(textPanel, BorderLayout.CENTER);

        btnLocal.addActionListener(l -> controller.onLocalConnectionClick());
        return btnLocal;
    }

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));
        panel.setName(Id.PNL_SETTINGS_CONTENT);
        panel.add(new JLabel("Configurações (Em desenvolvimento)"));
        return panel;
    }

    public void setServerListLoading(boolean loading) {
        serverListContainer.removeAll();
        if (loading) {
            serverListContainer.setLayout(new FlowLayout(FlowLayout.CENTER));

            JPanel loadingPanel = new JPanel();
            loadingPanel.setLayout(new BoxLayout(loadingPanel, BoxLayout.Y_AXIS));
            loadingPanel.setOpaque(false);

            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            Dimension size = new Dimension(200, 4);
            progressBar.setPreferredSize(size);
            progressBar.setMaximumSize(size);
            progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblLoading = new JLabel("Carregando conexões...");
            lblLoading.setAlignmentX(Component.CENTER_ALIGNMENT);
            lblLoading.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");

            loadingPanel.add(progressBar);
            loadingPanel.add(Box.createVerticalStrut(10));
            loadingPanel.add(lblLoading);
            serverListContainer.add(loadingPanel);
        } else {
            serverListContainer.setLayout(new GridLayout(0, 2, 20, 20));
        }
        serverListContainer.revalidate();
        serverListContainer.repaint();
    }

    public void renderRemoteConnections(
            Collection<ProcessOrchestratorRemoteConnection> connections,
            BiConsumer<ProcessOrchestratorRemoteConnection, MouseEvent> onClick,
            Consumer<ProcessOrchestratorRemoteConnection> onEdit,
            Consumer<ProcessOrchestratorRemoteConnection> onDelete
    ) {
        setServerListLoading(false);
        serverListContainer.removeAll();

        if (connections == null || connections.isEmpty()) {
            serverListContainer.setLayout(new FlowLayout(FlowLayout.CENTER));
            JLabel emptyLabel = new JLabel("Nenhuma conexão remota configurada.");
            emptyLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground; font: +1");
            serverListContainer.add(emptyLabel);
        } else {
            serverListContainer.setLayout(new GridLayout(0, 2, 20, 20));

            for (ProcessOrchestratorRemoteConnection conn : connections) {

                JPanel card = new JPanel(new BorderLayout());
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                        new EmptyBorder(10, 10, 10, 10)
                ));
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));

                JPanel content = buildServerCardPanel(conn.getConnectionName(), conn.getUrl(), true);
                content.setOpaque(false);

                JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
                actions.setOpaque(false);

                JButton editBtn = new JButton("✏");
                JButton deleteBtn = new JButton("🗑");

                editBtn.setFocusable(false);
                deleteBtn.setFocusable(false);

                // Clique no card inteiro (exceto nos botões de ação)
                MouseAdapter cardClick = new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (onClick != null) onClick.accept(conn, e);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(UIManager.getColor("Component.focusColor"), 1, true),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        card.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                                new EmptyBorder(10, 10, 10, 10)
                        ));
                    }
                };

                card.addMouseListener(cardClick);
                content.addMouseListener(cardClick);

                editBtn.addActionListener(e -> {
                    if (onEdit != null) onEdit.accept(conn);
                });

                deleteBtn.addActionListener(e -> {
                    if (onDelete != null) onDelete.accept(conn);
                });

                actions.add(editBtn);
                actions.add(deleteBtn);

                card.add(content, BorderLayout.CENTER);
                card.add(actions, BorderLayout.SOUTH);

                serverListContainer.add(card);
            }
        }

        serverListContainer.revalidate();
        serverListContainer.repaint();
    }

    public void showConnectionForm(ProcessOrchestratorRemoteConnection existing) {
        String cardKey = "viewConnectionForm";

        ConnectionFormPanel form = new ConnectionFormPanel(existing)
                .onCancel(() -> cardLayout.show(cardPanel, VIEW_ENVIRONMENTS))
                .onSave(conn -> {
                    controller.onSaveConnection(conn, existing);
                    cardLayout.show(cardPanel, VIEW_ENVIRONMENTS);
                });

        cardPanel.add(form, cardKey);
        cardLayout.show(cardPanel, cardKey);
    }

    private JPanel buildServerCardPanel(String name, String url, boolean connected) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(name);
        nameLabel.putClientProperty(FlatClientProperties.STYLE, "font: bold +1");

        JLabel urlLabel = new JLabel(url);
        urlLabel.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");

        JLabel statusDot = new JLabel("●");
        statusDot.setForeground(connected ? new Color(0x4CAF50) : Color.GRAY);
        statusDot.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(urlLabel);

        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(statusDot, BorderLayout.EAST);

        return panel;
    }

    public static class Id {
        public static final String BTN_LOCAL_CONNECT = "btnLocalConnect";
        public static final String BTN_ADD_SERVER = "btnAddServer";
        public static final String PNL_SERVER_LIST = "pnlServerList";
        public static final String PNL_SETTINGS_CONTENT = "pnlSettingsContent";
    }
}