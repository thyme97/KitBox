package com.kitbox.ui.components;

import com.kitbox.AppContext;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.keystore.KeyStoreException;
import com.kitbox.ui.SwingUtils;
import com.kitbox.util.KeyCodec;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.KeyPair;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 生成密钥对话框：选择类型与参数 → 生成预览 → 保存到密钥库。
 */
public final class KeyGenDialog extends JDialog {

    private final Runnable onSaved;
    private final JComboBox<String> scenarioCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField(16);
    private final JTextField remarkField = new JTextField(16);
    private final JComboBox<KeyEntryType> typeCombo = new JComboBox<>(new KeyEntryType[]{
            KeyEntryType.SYM_AES, KeyEntryType.SYM_DES, KeyEntryType.SYM_DESEDE, KeyEntryType.SYM_SM4,
            KeyEntryType.HMAC, KeyEntryType.RSA_KEYPAIR, KeyEntryType.SM2_KEYPAIR});
    private final JComboBox<Integer> paramCombo = new JComboBox<>();
    private final JTextArea previewArea = new JTextArea(7, 36);
    private final JButton saveButton = new JButton("保存");

    private String pendingValue;
    private KeyEntryType pendingType;

    public KeyGenDialog(JFrame owner, Runnable onSaved) {
        super(owner, "生成密钥", true);
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("名称："), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(nameField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("场景："), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        for (String s : AppContext.keyStore.getScenarios()) {
            scenarioCombo.addItem(s);
        }
        form.add(scenarioCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("类型："), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(typeCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("参数："), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(paramCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        form.add(new JLabel("备注："), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(remarkField, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setFont(SwingUtils.monoFont(previewArea.getFont().getSize()));
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("生成结果预览"));

        JButton generate = new JButton("生成");
        saveButton.setEnabled(false);
        JButton cancel = new JButton("取消");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(generate);
        buttons.add(saveButton);
        buttons.add(cancel);

        add(form, BorderLayout.NORTH);
        add(previewScroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        typeCombo.addActionListener(e -> {
            refreshParams();
            // 类型切换后之前生成的值已失效，重置待保存状态
            pendingValue = null;
            pendingType = null;
            previewArea.setText("");
            saveButton.setEnabled(false);
        });
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generate));
        saveButton.addActionListener(e -> SwingUtils.runWithCatch(this, this::save));
        cancel.addActionListener(e -> dispose());

        refreshParams();
        setSize(560, 420);
        setLocationRelativeTo(owner);
    }

    private void refreshParams() {
        paramCombo.removeAllItems();
        KeyEntryType type = (KeyEntryType) typeCombo.getSelectedItem();
        Integer[] options;
        switch (type) {
            case SYM_AES:
                options = new Integer[]{128, 192, 256};
                break;
            case RSA_KEYPAIR:
                options = new Integer[]{1024, 2048, 3072, 4096};
                break;
            case SYM_DES:
            case SYM_DESEDE:
            case SYM_SM4:
            case HMAC:
            case SM2_KEYPAIR:
            default:
                options = new Integer[]{};
                break;
        }
        for (Integer o : options) {
            paramCombo.addItem(o);
        }
        paramCombo.setEnabled(options.length > 0);
    }

    private void generate() throws Exception {
        KeyEntryType type = (KeyEntryType) typeCombo.getSelectedItem();
        Integer param = (Integer) paramCombo.getSelectedItem();
        StringBuilder preview = new StringBuilder();
        String value;
        switch (type) {
            case SYM_AES:
            case SYM_DES:
            case SYM_DESEDE:
            case SYM_SM4: {
                com.kitbox.crypto.model.SymmetricAlgorithm algo = KeyStoreCodec.toSymAlgo(type);
                int len = param != null ? param / 8 : algo.getKeyLengths()[0];
                byte[] key = com.kitbox.crypto.SymmetricService.randomKey(algo, len);
                value = java.util.Base64.getEncoder().encodeToString(key);
                preview.append("随机密钥（Base64，").append(len * 8).append(" 位）：\n").append(value);
                break;
            }
            case HMAC: {
                byte[] key = new byte[32];
                new java.security.SecureRandom().nextBytes(key);
                value = java.util.Base64.getEncoder().encodeToString(key);
                preview.append("HMAC 密钥（Base64，256 位）：\n").append(value);
                break;
            }
            case RSA_KEYPAIR: {
                int bits = param != null ? param : 2048;
                KeyPair kp = KeyCodec.generateRsaKeyPair(bits);
                value = KeyStoreCodec.buildKeyPairValue(kp.getPublic(), kp.getPrivate());
                preview.append("RSA-").append(bits).append(" 密钥对已生成。\n")
                        .append("公钥(Base64)：\n")
                        .append(KeyStoreCodec.partValueJson(value, "publicKey"))
                        .append("\n私钥(Base64)：已生成（保存后可在密钥库查看）");
                break;
            }
            case SM2_KEYPAIR: {
                KeyPair kp = KeyCodec.generateSm2KeyPair();
                value = KeyStoreCodec.buildKeyPairValue(kp.getPublic(), kp.getPrivate());
                preview.append("SM2 密钥对已生成。\n")
                        .append("公钥(Base64)：\n")
                        .append(KeyStoreCodec.partValueJson(value, "publicKey"))
                        .append("\n私钥(Base64)：已生成（保存后可在密钥库查看）");
                break;
            }
            default:
                throw new KeyStoreException("不支持的类型");
        }
        pendingValue = value;
        pendingType = type;
        previewArea.setText(preview.toString());
        previewArea.setCaretPosition(0);
        saveButton.setEnabled(true);
    }

    private void save() throws Exception {
        if (pendingValue == null || pendingType == null) {
            throw new KeyStoreException("请先点击『生成』再保存");
        }
        String scenario = (String) scenarioCombo.getSelectedItem();
        if (scenario == null) {
            throw new KeyStoreException("没有可选场景，请先在密钥库中新建场景");
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new KeyStoreException("名称不能为空");
        }
        AppContext.keyStore.addEntry(scenario, name,
                pendingType, pendingValue, remarkField.getText().trim());
        SwingUtils.info(this, "已保存到密钥库");
        if (onSaved != null) {
            onSaved.run();
        }
        dispose();
    }
}
