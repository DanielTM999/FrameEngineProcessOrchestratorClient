package dtm.plugins.views.components.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import dtm.manager.process.enums.RestartPolicy;
import dtm.plugins.models.ProcessNodeModel;
import dtm.stools.component.inputfields.filepicker.FileSelectionMode;
import dtm.stools.component.inputfields.selectfield.DropdownField;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.inputfields.textfield.PathTextField;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.filepicker.FilePickerInputPanel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ProcessNodeFormView extends BlockingPanel {

    public enum Mode { CREATE, EDIT, VIEW }

    private static final int LABEL_W = 120;

    private static final List<String> PROCESS_TYPES = List.of(
            "shell", "java", "node", "python", "bash", "powershell"
    );

    private final Mode mode;
    private final ProcessNodeModel source;

    private MaskedTextField          fieldId;
    private MaskedTextField          fieldName;
    private PathTextField            fieldEnv;
    private PathTextField            fieldExecPath;
    private DropdownField            fieldType;
    private MaskedTextField          fieldTimeout;
    private SyntaxCommandPanel       fieldCommand;
    private JTextArea                fieldDescription;
    private ExecutionConditionPanel  fieldCondition;
    private JComboBox<RestartPolicy> fieldRestart;
    private ArgsListPanel            argsPanel;

    private Consumer<ProcessNodeModel> onSave;
    private Runnable onCancel;

    public ProcessNodeFormView(ProcessNodeModel model) {
        this.source = model;
        this.mode = model == null ? Mode.CREATE : Mode.EDIT;
        build();
    }

    public ProcessNodeFormView(ProcessNodeModel model, boolean viewOnly) {
        this.source = model;
        this.mode   = viewOnly ? Mode.VIEW : (model == null ? Mode.CREATE : Mode.EDIT);
        build();
    }

    public ProcessNodeFormView onSave(Consumer<ProcessNodeModel> cb) { this.onSave   = cb; return this; }
    public ProcessNodeFormView onCancel(Runnable cb) { this.onCancel = cb; return this; }

    private void build() {
        setLayout(new BorderLayout());
        setOpaque(false);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildScroll(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(new FlatLineBorder(new Insets(0, 0, 1, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));
        bar.setPreferredSize(new Dimension(0, 38));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel title = new JLabel(modeTitle());
        title.putClientProperty(FlatClientProperties.STYLE, "font: bold 12");
        left.add(title);

        if (source != null) {
            JLabel badge = new JLabel(source.getProcessId());
            badge.putClientProperty(FlatClientProperties.STYLE,
                    "font: $mono.font; foreground: $Label.disabledForeground");
            left.add(badge);
        }

        bar.add(left, BorderLayout.CENTER);
        return bar;
    }


    private JScrollPane buildScroll() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(14, 14, 14, 14));

        fieldId = field(source != null ? source.getProcessId(): "");
        fieldName= field(source != null ? source.getProcessName(): "");
        fieldEnv = pathField(source != null ? source.getExecutionEnvironment(): "");
        fieldExecPath = pathField(source != null ? source.getExecutablePath(): "");
        fieldTimeout = field(source != null ? String.valueOf(source.getStartTimeout()) : "0");
        fieldCommand = new SyntaxCommandPanel(source != null ? source.getCommand(): "");

        fieldType = new DropdownField(PROCESS_TYPES.toArray());
        fieldType.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        String currentType = source != null && source.getType() != null ? source.getType().toLowerCase() : "";
        if (!currentType.isBlank() && PROCESS_TYPES.contains(currentType)) {
            fieldType.select(currentType);
        } else {
            fieldType.select("shell");
        }

        fieldRestart = new JComboBox<>(RestartPolicy.values());
        fieldRestart.setSelectedItem(
                source != null && source.getRestartPolicy() != null
                        ? source.getRestartPolicy()
                        : RestartPolicy.NEVER
        );
        fieldRestart.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        fieldCondition = new ExecutionConditionPanel( source != null ? source.getExecutionCondition() : null);
        fieldDescription = textArea("", 3);
        argsPanel = new ArgsListPanel(source != null ? source.getArgs() : new ArrayList<>());

        boolean view = mode == Mode.VIEW;

        fieldId.setReadonly(true);
        applyReadonly(fieldName, view);
        fieldEnv.setReadonly(view);
        fieldExecPath.setReadonly(view);
        applyReadonly(fieldTimeout, view);
        fieldType.setEnabled(!view);
        fieldCommand.setEditable(!view);
        fieldCondition.setEditable(!view);
        fieldDescription.setEditable(!view);
        fieldRestart.setEnabled(!view);
        argsPanel.setEditable(!view);

        form.add(sectionCard("Identificação",
                row("ID do processo", fieldId),
                row("Nome",           fieldName),
                row("Tipo",           fieldType)
        ));
        form.add(vgap(8));

        form.add(sectionCard("Execução",
                pathRow("Ambiente",    fieldEnv,      false),
                pathRow("Executável",  fieldExecPath, true),
                row("Timeout (ms)",    fieldTimeout),
                row("Restart policy",  fieldRestart)
        ));
        form.add(vgap(8));

        form.add(sectionCard("Comando",              fieldCommand));
        form.add(vgap(8));
        form.add(sectionCard("Argumentos",           argsPanel));
        form.add(vgap(8));
        form.add(sectionCard("Condição de execução", fieldCondition));
        form.add(vgap(8));
        form.add(sectionCard("Descrição",            scrollArea(fieldDescription)));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        return scroll;
    }

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(new FlatLineBorder(new Insets(1, 0, 0, 0),
                UIManager.getColor("Component.borderColor"), 1, 0));
        bar.setPreferredSize(new Dimension(0, 40));

        if (mode == Mode.VIEW) {
            JButton edit = new JButton("Editar");
            edit.putClientProperty(FlatClientProperties.STYLE, "arc: 6");
            edit.addActionListener(e -> switchToEdit());
            bar.add(edit);
        } else {
            JButton cancel = new JButton("Cancelar");
            cancel.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 6; borderColor: $Component.borderColor");
            cancel.addActionListener(e -> { if (onCancel != null) onCancel.run(); });

            JButton save = new JButton(mode == Mode.CREATE ? "Criar" : "Salvar");
            save.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 6; background: $Button.default.background");
            save.addActionListener(e -> { if (onSave != null) onSave.accept(collect()); });

            bar.add(cancel);
            bar.add(save);
        }

        return bar;
    }

    private void switchToEdit() {
        removeAll();
        ProcessNodeFormView editable = new ProcessNodeFormView(source, false)
                .onSave(onSave)
                .onCancel(onCancel);
        setLayout(new BorderLayout());
        add(editable, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public ProcessNodeModel collect() {
        ProcessNodeModel m = source != null ? source : new ProcessNodeModel();
        m.setProcessId(fieldId.getText().strip());
        m.setProcessName(fieldName.getText().strip());
        m.setExecutionEnvironment(normalize(fieldEnv.getText().strip()));
        m.setExecutablePath(normalize(fieldExecPath.getText().strip()));
        m.setType(fieldType.toString());
        m.setCommand(fieldCommand.getValue());
        m.setExecutionCondition(fieldCondition.getValue());
        m.setRestartPolicy((RestartPolicy) fieldRestart.getSelectedItem());
        m.setArgs(argsPanel.getArgs());
        try {
            m.setStartTimeout(Long.parseLong(fieldTimeout.getText().strip()));
        } catch (NumberFormatException ignored) { m.setStartTimeout(0); }

        return m;
    }

    private JPanel sectionCard(String title, JComponent... children) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(true);
        card.setBackground(UIManager.getColor("Panel.background"));
        card.setBorder(BorderFactory.createCompoundBorder(
                new FlatLineBorder(new Insets(1, 1, 1, 1),
                        UIManager.getColor("Component.borderColor"), 1, 8),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setForeground(UIManager.getColor("Component.accentColor"));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 9f));
        lbl.setBorder(new EmptyBorder(0, 0, 8, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        card.add(lbl);

        for (JComponent child : children) {
            child.setAlignmentX(LEFT_ALIGNMENT);
            card.add(child);
        }

        return card;
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 8));
        p.setBorder(new EmptyBorder(0, 0, 5, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.weightx = 0; p.add(makeLabel(label), g);
        g.gridx = 1; g.weightx = 1; p.add(field, g);
        return p;
    }

    private JPanel pathRow(String label, PathTextField field, boolean filesOnly) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setBorder(new EmptyBorder(0, 0, 5, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height + 8));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0; g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;

        JButton browse = new JButton(folderIcon());
        browse.setPreferredSize(new Dimension(24, 22));
        browse.setMinimumSize(new Dimension(24, 22));
        browse.setMaximumSize(new Dimension(24, 22));
        browse.setFocusable(false);
        browse.putClientProperty(FlatClientProperties.STYLE,
                "arc: 5; borderWidth: 1; borderColor: $Component.borderColor");
        browse.addActionListener(e -> openFilePicker(field, filesOnly));
        browse.setEnabled(mode != Mode.VIEW);

        g.gridx = 0; g.weightx = 0;                                    p.add(makeLabel(label), g);
        g.gridx = 1; g.weightx = 1;                                    p.add(field,  g);
        g.gridx = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 4, 0, 0);                             p.add(browse, g);
        return p;
    }

    private void openFilePicker(PathTextField target, boolean filesOnly) {
        String current = target.getText();
        Path startPath;
        try {
            File f = new File(current);
            startPath = f.exists()
                    ? (f.isFile() ? f.getParentFile().toPath() : f.toPath())
                    : Path.of(System.getProperty("user.home"));
        } catch (Exception ex) {
            startPath = Path.of(System.getProperty("user.home"));
        }

        FilePickerInputPanel picker = new FilePickerInputPanel(startPath);
        picker.setFileSelectionMode(filesOnly
                ? FileSelectionMode.FILES_ONLY
                : FileSelectionMode.FILES_AND_DIRECTORIES);
        picker.setMultiSelectionEnabled(false);
        if (!current.isBlank()) picker.setSelectedFile(current);

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Selecionar arquivo",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(820, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.add(picker, BorderLayout.CENTER);

        picker.setOnEndSelection(p -> {
            File selected = p.getSelectedFile();
            if (selected != null) target.setText(selected.getAbsolutePath());
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(UIManager.getColor("Label.disabledForeground"));
        lbl.setFont(lbl.getFont().deriveFont(10f));
        lbl.setPreferredSize(new Dimension(LABEL_W, lbl.getPreferredSize().height));
        lbl.setMinimumSize(new Dimension(LABEL_W, 0));
        return lbl;
    }

    private MaskedTextField field(String value) {
        MaskedTextField f = new MaskedTextField(value != null ? value : "");
        f.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return f;
    }

    private PathTextField pathField(String value) {
        PathTextField f = new PathTextField("/");
        f.setText(normalize(value));
        f.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return f;
    }

    private JTextArea textArea(String value, int rows) {
        JTextArea a = new JTextArea(value != null ? value : "");
        a.setRows(rows);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setOpaque(true);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return a;
    }

    private JScrollPane scrollArea(JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(UIManager.getBorder("TextField.border"));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return sp;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.replace('\\', '/');
    }

    private Component vgap(int h) { return Box.createVerticalStrut(h); }

    private void applyReadonly(MaskedTextField f, boolean readonly) { f.setReadonly(readonly); }

    private String modeTitle() {
        return switch (mode) {
            case CREATE -> "Novo processo";
            case EDIT   -> "Editar processo";
            case VIEW   -> "Detalhes do processo";
        };
    }

    private Icon folderIcon() {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIManager.getColor("Label.foreground"));
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(x + 1, y + 3, 11, 8, 2, 2);
                g2.drawLine(x + 1, y + 5, x + 5, y + 5);
                g2.drawLine(x + 5, y + 5, x + 6, y + 3);
                g2.drawLine(x + 6, y + 3, x + 12, y + 3);
                g2.dispose();
            }
            public int getIconWidth()  { return 14; }
            public int getIconHeight() { return 14; }
        };
    }

    static RSyntaxTextArea buildSyntaxArea(String syntax, String value, int rows) {
        RSyntaxTextArea area = new RSyntaxTextArea(rows, 0);
        area.setSyntaxEditingStyle(syntax);
        area.setCodeFoldingEnabled(true);
        area.setAntiAliasingEnabled(true);
        area.setTabSize(4);
        area.setTabsEmulated(true);
        area.setText(value != null ? value : "");
        area.setCaretPosition(0);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        boolean dark = UIManager.getBoolean("laf.dark");
        String themeFile = dark
                ? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
                : "/org/fife/ui/rsyntaxtextarea/themes/idea.xml";
        try (InputStream in = RSyntaxTextArea.class.getResourceAsStream(themeFile)) {
            if (in != null) Theme.load(in).apply(area);
        } catch (IOException ignored) {}
        return area;
    }

    static RTextScrollPane buildSyntaxScroll(RSyntaxTextArea area) {
        RTextScrollPane sp = new RTextScrollPane(area);
        sp.setLineNumbersEnabled(true);
        sp.setBorder(UIManager.getBorder("TextField.border"));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        sp.getGutter().setBackground(UIManager.getColor("Panel.background"));
        sp.getGutter().setBorderColor(UIManager.getColor("Component.borderColor"));
        return sp;
    }


    private static class SyntaxCommandPanel extends JPanel {

        private final RSyntaxTextArea syntaxArea;

        SyntaxCommandPanel(String value) {
            setLayout(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            syntaxArea = buildSyntaxArea(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL, value, 4);
            RTextScrollPane scroll = buildSyntaxScroll(syntaxArea);
            scroll.setPreferredSize(new Dimension(0, 100));
            add(scroll, BorderLayout.CENTER);
        }

        void setEditable(boolean editable) {
            syntaxArea.setEditable(editable);
        }

        String getValue() { return syntaxArea.getText().strip(); }
    }


    private static class ExecutionConditionPanel extends JPanel {

        private static final String CARD_SIMPLE = "simple";
        private static final String CARD_SCRIPT = "script";

        private final JCheckBox       chkUseScript;
        private final JCheckBox       chkAlwaysRun;
        private final RSyntaxTextArea scriptArea;
        private final CardLayout      cards;
        private final JPanel          cardPanel;

        ExecutionConditionPanel(Object initialValue) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);

            boolean hasScript = initialValue instanceof String s && !s.isBlank();
            boolean alwaysRun = initialValue instanceof Boolean b ? b : !hasScript;

            chkUseScript = new JCheckBox("Usar Script Condicional");
            chkUseScript.setOpaque(false);
            chkUseScript.setSelected(hasScript);
            chkUseScript.setAlignmentX(LEFT_ALIGNMENT);
            chkUseScript.putClientProperty(FlatClientProperties.STYLE, "font: 11");

            chkAlwaysRun = new JCheckBox("Habilitar Execução (Executar sempre)");
            chkAlwaysRun.setOpaque(false);
            chkAlwaysRun.setSelected(alwaysRun);
            chkAlwaysRun.setAlignmentX(LEFT_ALIGNMENT);
            chkAlwaysRun.putClientProperty(FlatClientProperties.STYLE, "font: 11");

            scriptArea = buildSyntaxArea(SyntaxConstants.SYNTAX_STYLE_JAVA,
                    hasScript ? (String)initialValue : "", 8);
            RTextScrollPane scriptScroll = buildSyntaxScroll(scriptArea);
            scriptScroll.setPreferredSize(new Dimension(0, 180));

            JPanel simplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            simplePanel.setOpaque(false);
            simplePanel.add(chkAlwaysRun);

            JPanel scriptPanel = new JPanel(new BorderLayout());
            scriptPanel.setOpaque(false);
            scriptPanel.add(scriptScroll, BorderLayout.CENTER);

            cards = new CardLayout();
            cardPanel = new JPanel(cards);
            cardPanel.setOpaque(false);
            cardPanel.setAlignmentX(LEFT_ALIGNMENT);
            cardPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            cardPanel.add(simplePanel, CARD_SIMPLE);
            cardPanel.add(scriptPanel, CARD_SCRIPT);

            add(chkUseScript);
            add(Box.createVerticalStrut(6));
            add(cardPanel);

            cards.show(cardPanel, hasScript ? CARD_SCRIPT : CARD_SIMPLE);

            Runnable applyState = () -> {
                boolean useScript = chkUseScript.isSelected();
                cards.show(cardPanel, useScript ? CARD_SCRIPT : CARD_SIMPLE);
                if (useScript && scriptArea.getText().isBlank()) {
                    scriptArea.setText("(Environment env) -> {\n    return true;\n}");
                    scriptArea.setCaretPosition(0);
                }
                revalidate();
                repaint();
            };

            chkUseScript.addActionListener(e -> applyState.run());
            applyState.run();
        }

        void setEditable(boolean editable) {
            chkUseScript.setEnabled(editable);
            chkAlwaysRun.setEnabled(editable);
            scriptArea.setEditable(editable);
        }

        Object getValue() {
            if (chkUseScript.isSelected()) return scriptArea.getText().strip();
            if(chkAlwaysRun.isSelected()) return true;
            return false;
        }
    }


    private static class ArgsListPanel extends JPanel {

        private final List<String> args;
        private boolean editable;
        private JPanel listPanel;

        ArgsListPanel(List<String> args) {
            this.args = new ArrayList<>(args != null ? args : List.of());
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            rebuild();
        }

        void setEditable(boolean editable) {
            this.editable = editable;
            rebuild();
        }

        List<String> getArgs() {
            List<String> result = new ArrayList<>();
            for (Component c : listPanel.getComponents()) {
                if (c instanceof ArgRow r) {
                    String v = r.getValue().strip();
                    if (!v.isEmpty()) result.add(v);
                }
            }
            return result;
        }

        private void rebuild() {
            removeAll();
            listPanel = new JPanel();
            listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
            listPanel.setOpaque(false);
            listPanel.setAlignmentX(LEFT_ALIGNMENT);

            for (String arg : args) addRow(arg);
            add(listPanel);

            if (editable) {
                JButton addBtn = new JButton("Adicionar argumento");
                addBtn.setAlignmentX(LEFT_ALIGNMENT);
                addBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, addBtn.getPreferredSize().height));
                addBtn.setFocusable(false);
                addBtn.putClientProperty(FlatClientProperties.STYLE,
                        "background: null; foreground: $Label.disabledForeground;" +
                        "borderWidth: 1; borderColor: $Component.borderColor; arc: 6");
                addBtn.addActionListener(e -> { addRow(""); revalidate(); repaint(); });
                add(Box.createVerticalStrut(4));
                add(addBtn);
            } else if (args.isEmpty()) {
                JLabel empty = new JLabel("Nenhum argumento");
                empty.putClientProperty(FlatClientProperties.STYLE,
                        "font: 10; foreground: $Label.disabledForeground");
                empty.setAlignmentX(LEFT_ALIGNMENT);
                add(empty);
            }

            revalidate();
            repaint();
        }

        private void addRow(String value) {
            ArgRow row = new ArgRow(value, editable, () -> {
                Component toRemove = findRow(value);
                if (toRemove != null) listPanel.remove(toRemove);
                revalidate();
                repaint();
            });
            listPanel.add(row);
        }

        private Component findRow(String value) {
            for (Component c : listPanel.getComponents()) {
                if (c instanceof ArgRow r && r.getValue().equals(value)) return r;
            }
            return null;
        }
    }


    private static class ArgRow extends JPanel {

        private final JTextField field;

        ArgRow(String value, boolean editable, Runnable onRemove) {
            setLayout(new BorderLayout(4, 0));
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            setBorder(new EmptyBorder(0, 0, 4, 0));

            field = new JTextField(value);
            field.setEditable(editable);
            field.putClientProperty(FlatClientProperties.STYLE,
                    "font: $mono.font; fontSize: 11" +
                            (editable ? "" : "; background: $Panel.background"));

            add(field, BorderLayout.CENTER);

            if (editable) {
                JButton del = new JButton(deleteIcon());
                del.setPreferredSize(new Dimension(24, 24));
                del.setFocusable(false);
                del.putClientProperty(FlatClientProperties.STYLE,
                        "background: null; borderWidth: 0; arc: 4");
                del.addActionListener(e -> {
                    Container parent = getParent();
                    if (parent != null) { parent.remove(this); parent.revalidate(); parent.repaint(); }
                    onRemove.run();
                });
                add(del, BorderLayout.EAST);
            }
        }

        String getValue() { return field.getText(); }

        private static Icon deleteIcon() {
            return new Icon() {
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color red = UIManager.getColor("Actions.Red");
                    g2.setColor(red != null ? red : new Color(0xE24B4A));
                    g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 3,  y + 4,  x + 3,  y + 11);
                    g2.drawLine(x + 10, y + 4,  x + 10, y + 11);
                    g2.drawLine(x + 3,  y + 11, x + 10, y + 11);
                    g2.drawLine(x + 1,  y + 3,  x + 12, y + 3);
                    g2.drawLine(x + 5,  y + 3,  x + 5,  y + 1);
                    g2.drawLine(x + 5,  y + 1,  x + 8,  y + 1);
                    g2.drawLine(x + 8,  y + 1,  x + 8,  y + 3);
                    g2.dispose();
                }
                public int getIconWidth()  { return 13; }
                public int getIconHeight() { return 13; }
            };
        }
    }
}