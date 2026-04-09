package dtm.plugins.views.remote.components.form;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.plugins.models.remote.connection.ProcessOrchestratorRemoteConnection;
import dtm.stools.component.inputfields.filepicker.FileSelectionMode;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.inputfields.textfield.PathTextField;
import dtm.stools.component.panels.BlockingPanel;
import dtm.stools.component.panels.filepicker.FilePickerInputPanel;
import dtm.stools.context.popup.PopupBuilder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.function.Consumer;

public class ConnectionFormPanel extends BlockingPanel {

    private static final int FIELD_HEIGHT = 40;
    private static final String FIELD_STYLE = "arc: 8; margin: 6,12,6,12";

    private final MaskedTextField txtName;
    private final MaskedTextField txtAddress;
    private final PathTextField txtKeyPath;

    private Consumer<ProcessOrchestratorRemoteConnection> onSave;
    private Runnable onCancel;

    public ConnectionFormPanel(ProcessOrchestratorRemoteConnection existing) {
        boolean isNew = existing == null;

        setLayout(new BorderLayout());

        txtName = createField("Ex: Produção");
        txtAddress = createField("Ex: 192.168.1.50:59090 ou api.meusite.com");
        txtKeyPath = createPathField("Nenhum arquivo selecionado");
        txtKeyPath.setEditable(false);
        txtKeyPath.setReadonly(true);

        if (existing != null) {
            txtName.setText(existing.getConnectionName());
            txtAddress.setText(existing.getUrl());
            txtKeyPath.setText(existing.getPrivateKeyConnection());
        }

        add(buildCenter(isNew), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private MaskedTextField createField(String placeholder) {
        MaskedTextField f = new MaskedTextField();
        f.putClientProperty(FlatClientProperties.STYLE, FIELD_STYLE);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return f;
    }

    private PathTextField createPathField(String placeholder) {
        PathTextField f = new PathTextField(FileSystems.getDefault().getSeparator());
        f.putClientProperty(FlatClientProperties.STYLE, FIELD_STYLE);
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        return f;
    }

    private JComponent buildCenter(boolean isNew) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(36, 40, 36, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;

        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 4, 0);
        JLabel lblTitle = new JLabel(isNew ? "Nova conexão" : "Editar conexão");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold +6");
        page.add(lblTitle, gc);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 36, 0);
        JLabel lblSub = new JLabel("Configure o acesso seguro ao servidor remoto.");
        lblSub.putClientProperty(FlatClientProperties.STYLE, "foreground: $Label.disabledForeground");
        page.add(lblSub, gc);

        JPanel card = buildCard();

        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        page.add(card, gc);

        gc.gridy = 3;
        gc.weighty = 1.0;
        page.add(Box.createGlue(), gc);

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.putClientProperty(FlatClientProperties.STYLE,
                "arc: 12; background: darken($Panel.background, 2%)");
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridwidth = GridBagConstraints.REMAINDER;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.WEST;
        int row = 0;

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        page(card, gc, createLabel("Nome da conexão"));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 24, 0);
        page(card, gc, sizedField(txtName));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        page(card, gc, createLabel("Endereço do servidor"));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 4, 0);
        page(card, gc, sizedField(txtAddress));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 24, 0);
        page(card, gc, createHint("Host ou IP com porta de conexão"));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 6, 0);
        page(card, gc, createLabel("Chave privada"));

        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 4, 0);
        page(card, gc, buildKeyRow());

        gc.gridy = row;
        gc.insets = new Insets(0, 0, 0, 0);
        page(card, gc, createHint("Arquivo .pem ou .key para autenticação no servidor"));

        return card;
    }

    private void page(JPanel target, GridBagConstraints gc, JComponent comp) {
        target.add(comp, gc);
    }

    private JPanel sizedField(MaskedTextField field) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        wrap.add(field, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildKeyRow() {
        JButton btn = new JButton("Procurar...");
        btn.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btn.setPreferredSize(new Dimension(110, FIELD_HEIGHT));
        btn.addActionListener(e -> openFilePicker(txtKeyPath));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, FIELD_HEIGHT));
        row.add(txtKeyPath, BorderLayout.CENTER);
        row.add(btn, BorderLayout.EAST);
        return row;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font: bold");
        return lbl;
    }

    private JLabel createHint(String text) {
        JLabel lbl = new JLabel(text);
        lbl.putClientProperty(FlatClientProperties.STYLE,
                "font: -2; foreground: $Label.disabledForeground");
        return lbl;
    }

    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
                new EmptyBorder(12, 24, 12, 24)
        ));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
        btnCancel.setPreferredSize(new Dimension(100, 36));
        btnCancel.addActionListener(e -> { if (onCancel != null) onCancel.run(); });

        JButton btnSave = new JButton("Salvar conexão");
        btnSave.putClientProperty(FlatClientProperties.STYLE,
                "font: bold; arc: 8; background: $Component.accentColor; foreground: #fff");
        btnSave.setPreferredSize(new Dimension(150, 36));
        btnSave.addActionListener(e -> { if (onSave != null) onSave.accept(buildResult()); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(btnCancel);
        buttons.add(btnSave);

        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private void openFilePicker(MaskedTextField target) {
        FilePickerInputPanel picker = new FilePickerInputPanel();
        picker.setAllowNewFileInput(false);
        picker.setFileSelectionMode(FileSelectionMode.FILES_ONLY);
        picker.setRequired(true);
        picker.setMultiSelectionEnabled(false);
        picker.setFileFilter(new FileNameExtensionFilter(".key, .pem", "pem", "key"));
        picker.setOnEndSelection(p -> {
            File file = p.getSelectedFile();
            SwingUtilities.invokeLater(() -> target.setText(file.getAbsolutePath()));
        });
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        PopupBuilder.create(parentWindow)
                .title("Selecionar chave privada")
                .size(900, 600)
                .modal(true)
                .centerOn(this)
                .content(picker)
                .showAndWait();
    }

    private ProcessOrchestratorRemoteConnection buildResult() {
        return ProcessOrchestratorRemoteConnection.builder()
                .connectionName(txtName.getText().trim())
                .url(txtAddress.getText().trim())
                .privateKeyConnection(txtKeyPath.getText().trim())
                .build();
    }

    public ConnectionFormPanel onSave(Consumer<ProcessOrchestratorRemoteConnection> callback) {
        this.onSave = callback;
        return this;
    }

    public ConnectionFormPanel onCancel(Runnable callback) {
        this.onCancel = callback;
        return this;
    }

    public static class Id {
        public static final String TXT_NAME = "txtName";
        public static final String TXT_ADDRESS = "txtAddress";
        public static final String TXT_KEY_PATH = "txtKeyPath";
    }
}