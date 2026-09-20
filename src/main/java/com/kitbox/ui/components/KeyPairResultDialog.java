package com.kitbox.ui.components;

import com.kitbox.AppContext;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.keystore.KeyStoreException;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;
import com.kitbox.util.KeyCodec;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 生成密钥对结果对话框：展示公私钥、导出 PEM、保存到密钥库。
 */
public final class KeyPairResultDialog extends JDialog {

    private final KeyEntryType keyPairType;
    private final String publicBase64;
    private final String privateBase64;

    public KeyPairResultDialog(JFrame owner, String title, KeyEntryType keyPairType,
                               java.security.PublicKey publicKey, java.security.PrivateKey privateKey) {
        this(owner, title, keyPairType,
                java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded()),
                java.util.Base64.getEncoder().encodeToString(privateKey.getEncoded()));
    }

    public KeyPairResultDialog(JFrame owner, String title, KeyEntryType keyPairType,
                               String publicBase64, String privateBase64) {
        super(owner, title, true);
        this.keyPairType = keyPairType;
        this.publicBase64 = publicBase64;
        this.privateBase64 = privateBase64;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        FormPanel form = new FormPanel();
        JTextArea pubArea = readOnly(publicBase64, 4);
        JTextArea privArea = readOnly(privateBase64, 6);
        form.addField("公钥(Base64)：", wrap(pubArea));
        form.addField("私钥(Base64)：", wrap(privArea));
        form.addHint("私钥务必妥善保管；建议保存到密钥库加密存储。");
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton copyPub = new JButton("复制公钥");
        JButton copyPriv = new JButton("复制私钥");
        JButton exportPem = new JButton("导出 PEM…");
        JButton save = new JButton("保存到密钥库…");
        JButton close = new JButton("关闭");
        buttons.add(copyPub);
        buttons.add(copyPriv);
        buttons.add(exportPem);
        buttons.add(save);
        buttons.add(close);
        add(buttons, BorderLayout.SOUTH);

        copyPub.addActionListener(e -> SwingUtils.copyToClipboard(publicBase64));
        copyPriv.addActionListener(e -> SwingUtils.copyToClipboard(privateBase64));
        exportPem.addActionListener(e -> exportPem());
        save.addActionListener(e -> saveToStore());
        close.addActionListener(e -> dispose());

        setSize(640, 460);
        setLocationRelativeTo(owner);
    }

    private JTextArea readOnly(String text, int rows) {
        JTextArea area = new JTextArea(text, rows, 40);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(SwingUtils.monoFont(area.getFont().getSize()));
        return area;
    }

    private JScrollPane wrap(JTextArea area) {
        return new JScrollPane(area);
    }

    private void exportPem() {
        try {
            File pubFile = choose("保存公钥 PEM");
            if (pubFile != null) {
                Files.write(pubFile.toPath(),
                        KeyCodec.pemFromPublicBase64(publicBase64).getBytes(StandardCharsets.UTF_8));
            }
            File privFile = choose("保存私钥 PEM");
            if (privFile != null) {
                Files.write(privFile.toPath(),
                        KeyCodec.pemFromPrivateBase64(privateBase64).getBytes(StandardCharsets.UTF_8));
            }
            SwingUtils.info(this, "PEM 导出完成");
        } catch (Exception e) {
            SwingUtils.error(this, "导出失败：" + e.getMessage());
        }
    }

    private File choose(String title) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle(title);
        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private void saveToStore() {
        if (!KeyStoreDialogs.ensureUnlocked(this)) {
            return;
        }
        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        JLabel nameLabel = new JLabel("名称：");
        javax.swing.JTextField nameField = new javax.swing.JTextField(16);
        JComboBox<String> scenarioCombo = new JComboBox<>(
                AppContext.keyStore.getScenarios().toArray(new String[0]));
        JLabel remarkLabel = new JLabel("备注：");
        javax.swing.JTextField remarkField = new javax.swing.JTextField(16);

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 8, 4, 8);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("场景："), gbc);
        gbc.gridx = 1;
        panel.add(scenarioCombo, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(remarkLabel, gbc);
        gbc.gridx = 1;
        panel.add(remarkField, gbc);

        while (true) {
            int option = javax.swing.JOptionPane.showConfirmDialog(this, panel, "保存到密钥库",
                    javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.PLAIN_MESSAGE);
            if (option != javax.swing.JOptionPane.OK_OPTION) {
                return;
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                SwingUtils.error(this, "名称不能为空");
                continue;
            }
            try {
                AppContext.keyStore.addEntry((String) scenarioCombo.getSelectedItem(), name,
                        keyPairType, KeyStoreCodec.buildKeyPairValue(publicBase64, privateBase64),
                        remarkField.getText().trim());
                SwingUtils.info(this, "已保存到密钥库：" + scenarioCombo.getSelectedItem() + " / " + name);
                dispose();
                return;
            } catch (KeyStoreException e) {
                SwingUtils.error(this, e.getMessage());
            }
        }
    }
}
