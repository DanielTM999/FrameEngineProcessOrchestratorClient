package dtm.plugins.views;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import dtm.manager.process.definition.ProcessDefinition;
import dtm.plugins.views.local.ProcessOrchestratorLocalClientView;
import dtm.plugins.views.components.pipeline.ProcessSidebarItem;
import dtm.stools.component.delegated.DelegatedBlockingPanel;
import dtm.stools.component.inputfields.textfield.SearchTextField;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.controllers.component.AbstractViewController;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public abstract class BaseProcessView<C extends AbstractViewController<BlockingPanel>> extends DelegatedBlockingPanel<C> {


    @Getter
    protected JButton btnBack;

    @Getter
    protected JButton btnRefresh;

    protected JToolBar toolbar;

    protected JPanel sidebarPanel;
    protected JPanel listContainer;

    @Getter
    protected JButton btnCreate;

    @Getter
    protected SearchTextField<ProcessSidebarItem> txtSearch;

    protected JPanel  mainContentPanel;
    protected JSplitPane splitPane;

    @Override
    protected void onDrawing() {
        super.onDrawing();
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildSidebar(), buildMainContent());
        splitPane.setDividerLocation(240);
        splitPane.setDividerSize(3);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);

        add(splitPane, BorderLayout.CENTER);
    }

    public ProcessSidebarItem addProcessSidebarItem(ProcessDefinition processDefinition) {
        return addProcessSidebarItem(processDefinition, false);
    }

    public ProcessSidebarItem addProcessSidebarItem(ProcessDefinition processDefinition, boolean isRunning) {
        ProcessSidebarItem item = new ProcessSidebarItem(processDefinition, isRunning);
        runOnUiTread(() -> addSidebarItem(item));
        return item;
    }

    public ProcessSidebarItem addProcessSidebarItem(ProcessDefinition processDefinition, boolean isRunning, boolean isMonitoring) {
        ProcessSidebarItem item = new ProcessSidebarItem(processDefinition, isRunning);
        item.setMonitoring(isMonitoring);
        addSidebarItem(item);
        return item;
    }

    private JToolBar buildToolbar() {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new FlatLineBorder(
                new Insets(0, 0, 1, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));

        btnBack = makeToolbarButton("Conexões", chevronLeftIcon());
        btnBack.setName(ProcessOrchestratorLocalClientView.Id.BTN_BACK);

        btnRefresh = makeToolbarButton("Atualizar", refreshIcon());
        btnRefresh.setName(ProcessOrchestratorLocalClientView.Id.BTN_REFRESH);

        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(btnBack);
        toolbar.add(Box.createHorizontalStrut(2));
        toolbar.add(btnRefresh);
        return toolbar;
    }

    private JButton makeToolbarButton(String text, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFocusable(false);
        b.setIconTextGap(6);
        b.putClientProperty(FlatClientProperties.STYLE,
                "background: null; borderWidth: 0; font: 12; foreground: $Label.foreground");
        return b;
    }

    private JPanel buildSidebar() {
        sidebarPanel = new JPanel(new BorderLayout());
        sidebarPanel.setMinimumSize(new Dimension(180, 0));
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBorder(new FlatLineBorder(
                new Insets(0, 0, 0, 1),
                UIManager.getColor("Component.borderColor"), 1, 0));

        sidebarPanel.add(buildSidebarHeader(), BorderLayout.NORTH);
        sidebarPanel.add(buildSidebarList(), BorderLayout.CENTER);
        return sidebarPanel;
    }

    private JPanel buildSidebarHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setBorder(new EmptyBorder(14, 14, 10, 14));
        header.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        JLabel lblExplorer = new JLabel("EXPLORER");
        lblExplorer.putClientProperty(FlatClientProperties.STYLE,
                "font: bold 10; foreground: $Label.disabledForeground; border: 0,0,0,2");

        btnCreate = new JButton();
        btnCreate.setName(ProcessOrchestratorLocalClientView.Id.BTN_CREATE);
        btnCreate.setToolTipText("Novo processo");
        btnCreate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCreate.setFocusable(false);
        btnCreate.setIcon(addIcon());
        btnCreate.setPreferredSize(new Dimension(26, 26));
        btnCreate.putClientProperty(FlatClientProperties.STYLE,
                "background: $Component.accentColor;" +
                        "foreground: #fff;" +
                        "borderWidth: 0;" +
                        "arc: 8;" +
                        "margin: 0,0,0,0;" +
                        "hoverBackground: lighten($Component.accentColor,5%);" +
                        "pressedBackground: darken($Component.accentColor,5%)");

        titleRow.add(lblExplorer, BorderLayout.WEST);
        titleRow.add(btnCreate, BorderLayout.EAST);

        txtSearch = new SearchTextField<>();
        txtSearch.setDisplayFunction(ProcessSidebarItem::getProcessName);
        txtSearch.addSearchOption(ProcessSidebarItem::getProcessName);
        txtSearch.setCaseSensitive(false);
        txtSearch.setName(ProcessOrchestratorLocalClientView.Id.TXT_SEARCH);
        txtSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Buscar processo...");
        txtSearch.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        txtSearch.setFont(txtSearch.getFont().deriveFont(12f));
        txtSearch.setPreferredSize(new Dimension(txtSearch.getPreferredSize().width, 30));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                txtSearch.getBorder(),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(txtSearch, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildSidebarList() {
        listContainer = new JPanel();
        listContainer.setName(ProcessOrchestratorLocalClientView.Id.LIST_CONTAINER);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBorder(new EmptyBorder(4, 8, 8, 8));
        listContainer.setOpaque(false);

        JScrollPane scroll = new JScrollPane(listContainer);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().putClientProperty(
                FlatClientProperties.STYLE, "width: 6; trackArc: 999; thumbArc: 999");
        return scroll;
    }

    private JComponent buildMainContent() {
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setName(ProcessOrchestratorLocalClientView.Id.MAIN_CONTENT);
        mainContentPanel.setMinimumSize(new Dimension(200, 0));
        showEmptyState();
        return mainContentPanel;
    }

    private void showEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);

        JLabel icon = new JLabel(emptyStateIcon());
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
        mainContentPanel.add(empty, BorderLayout.CENTER);
    }

    public void addSidebarItem(Component item) {
        if (listContainer != null) {
            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(2));
            listContainer.revalidate();
            listContainer.repaint();
        }
    }

    public void removeSidebarItem(Component item) {
        if (listContainer != null) {
            Component[] comps = listContainer.getComponents();
            for (int i = 0; i < comps.length; i++) {
                if (comps[i].equals(item)) {
                    listContainer.remove(i);
                    if (i < listContainer.getComponentCount()) {
                        listContainer.remove(i);
                    }
                    break;
                }
            }
            listContainer.revalidate();
            listContainer.repaint();
        }
    }

    public void clearSidebar() {
        if (listContainer != null) {
            listContainer.removeAll();
            listContainer.revalidate();
            listContainer.repaint();
        }
    }

    public void setDefinitionContent(JComponent component) {
        if (mainContentPanel == null) return;
        mainContentPanel.removeAll();
        if (component != null) {
            mainContentPanel.add(component, BorderLayout.CENTER);
        } else {
            showEmptyState();
        }
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private Icon addIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 6, y + 3, x + 6, y + 11);
                g2.drawLine(x + 2, y + 7, x + 10, y + 7);
                g2.dispose();
            }
            public int getIconWidth()  { return 13; }
            public int getIconHeight() { return 14; }
        };
    }

    private Icon chevronLeftIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 8, y + 2, x + 4, y + 7);
                g2.drawLine(x + 4, y + 7, x + 8, y + 12);
                g2.dispose();
            }
            public int getIconWidth()  { return 13; }
            public int getIconHeight() { return 14; }
        };
    }

    private Icon refreshIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x + 1, y + 1, 11, 11, 60, 270);
                g2.drawPolyline(new int[]{x + 10, x + 12, x + 8}, new int[]{y + 1, y + 4, y + 4}, 3);
                g2.dispose();
            }
            public int getIconWidth()  { return 13; }
            public int getIconHeight() { return 14; }
        };
    }

    private Icon emptyStateIcon() {
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

    public static class Id {
        public static final String BTN_BACK       = "btnBack";
        public static final String BTN_REFRESH    = "btnRefresh";
        public static final String BTN_CREATE     = "btnCreate";
        public static final String TXT_SEARCH     = "txtSearch";
        public static final String LIST_CONTAINER = "listContainer";
        public static final String MAIN_CONTENT   = "mainContent";
    }


}
