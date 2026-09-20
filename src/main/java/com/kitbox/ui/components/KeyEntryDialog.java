package com.kitbox.ui.components;

import com.kitbox.AppContext;
import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.keystore.KeyEntry;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.keystore.KeyStoreException;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 密钥导入 / 编辑对话框：
 * - 按所选类型切换输入项：单密钥（一个值输入框）或密钥对（公钥 / 私钥两个独立输入框）
 * - 动态填写指引（放在表单之外，随对话框宽度自动换行）
 */
public final class KeyEntryDialog extends JDialog {

    private final KeyEntry existing;
    private final Runnable onSaved;
    private final JComboBox<String> scenarioCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField(20);
    private final JTextField remarkField = new JTextField(16);
    private final JComboBox<KeyEntryType> typeCombo = new JComboBox<>(KeyEntryType.values());
    private final JComboBox<KeyFormat> formatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.BASE64, KeyFormat.HEX, KeyFormat.PLAIN, KeyFormat.PEM});
    private final JTextArea valueArea = new JTextArea(7, 30);
    private final JTextArea publicKeyArea = new JTextArea(4, 30);
    private final JTextArea privateKeyArea = new JTextArea(4, 30);
    private final CardLayout valueCards = new CardLayout();
    private final JPanel valueCardPanel = new JPanel(valueCards);
    private final JTextArea guidanceArea = SwingUtils.hintArea(" ");

    /** 导入模式。 */
    public KeyEntryDialog(JFrame owner, Runnable onSaved) {
        this(owner, null, onSaved);
    }

    /** 编辑模式（existing != null）。 */
    public KeyEntryDialog(JFrame owner, KeyEntry existing, Runnable onSaved) {
        super(owner, existing == null ? "导入密钥" : "编辑密钥", true);
        this.existing = existing;
        this.onSaved = onSaved;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("名称："), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(nameField, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 3;
        form.add(new JLabel("场景："), gbc);
        gbc.gridx = 4;
        for (String s : AppContext.keyStore.getScenarios()) {
            scenarioCombo.addItem(s);
        }
        form.add(scenarioCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("类型："), gbc);
        gbc.gridx = 1;
        form.add(typeCombo, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("输入格式："), gbc);
        gbc.gridx = 3;
        form.add(formatCombo, gbc);
        gbc.gridx = 4;
        form.add(new JLabel("备注："), gbc);
        gbc.gridx = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(remarkField, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // 值输入卡片：单密钥 / 密钥对（公私钥分开）
        valueArea.setFont(SwingUtils.monoFont(valueArea.getFont().getSize()));
        valueArea.setLineWrap(true);
        JScrollPane singleScroll = new JScrollPane(valueArea);
        singleScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("密钥值（按下方指引填写）"));

        publicKeyArea.setFont(SwingUtils.monoFont(publicKeyArea.getFont().getSize()));
        publicKeyArea.setLineWrap(true);
        privateKeyArea.setFont(SwingUtils.monoFont(privateKeyArea.getFont().getSize()));
        privateKeyArea.setLineWrap(true);
        JPanel pairPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pgbc = new GridBagConstraints();
        pgbc.gridx = 0;
        pgbc.gridy = 0;
        pgbc.weightx = 1;
        pgbc.weighty = 1;
        pgbc.fill = GridBagConstraints.BOTH;
        pgbc.insets = new Insets(2, 4, 2, 4);
        JScrollPane pubScroll = new JScrollPane(publicKeyArea);
        pubScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("公钥"));
        pairPanel.add(pubScroll, pgbc);
        pgbc.gridy = 1;
        JScrollPane privScroll = new JScrollPane(privateKeyArea);
        privScroll.setBorder(javax.swing.BorderFactory.createTitledBorder("私钥"));
        pairPanel.add(privScroll, pgbc);

        valueCardPanel.add(singleScroll, "single");
        valueCardPanel.add(pairPanel, "pair");

        JButton save = new JButton("保存");
        JButton cancel = new JButton("取消");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.NORTH);
        north.add(guidanceArea, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(valueCardPanel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        if (existing != null) {
            nameField.setText(existing.getName());
            scenarioCombo.setSelectedItem(existing.getScenario());
            typeCombo.setSelectedItem(existing.getType());
            typeCombo.setEnabled(false);
            remarkField.setText(existing.getRemark());
            formatCombo.setSelectedItem(KeyFormat.BASE64);
        }

        save.addActionListener(e -> SwingUtils.runWithCatch(this, this::save));
        cancel.addActionListener(e -> dispose());

        typeCombo.addActionListener(e -> refreshValueCard());
        formatCombo.addActionListener(e -> refreshGuidance());
        refreshValueCard();
        setSize(640, 600);
        setLocationRelativeTo(owner);
    }

    private boolean isPairType() {
        KeyEntryType type = (KeyEntryType) typeCombo.getSelectedItem();
        return type != null && type.getKind() == KeyEntryType.Kind.KEYPAIR;
    }

    /** 按类型切换单值 / 公私钥双输入框，并刷新指引。 */
    private void refreshValueCard() {
        boolean pair = isPairType();
        valueCards.show(valueCardPanel, pair ? "pair" : "single");
        if (pair && existing != null && existing.getValue() != null) {
            // 编辑模式：预填公私钥
            try {
                publicKeyArea.setText(KeyStoreCodec.partValueJson(existing.getValue(), "publicKey"));
                privateKeyArea.setText(KeyStoreCodec.partValueJson(existing.getValue(), "privateKey"));
            } catch (Exception ignored) {
            }
        }
        refreshGuidance();
    }

    private void refreshGuidance() {
        KeyEntryType type = (KeyEntryType) typeCombo.getSelectedItem();
        KeyFormat fmt = (KeyFormat) formatCombo.getSelectedItem();
        String byType;
        if (type == null) {
            byType = "";
        } else {
            switch (type) {
                case SYM_AES:
                    byType = "AES 密钥（16/24/32 字节 = 128/192/256 位）";
                    break;
                case SYM_DES:
                    byType = "DES 密钥（固定 8 字节 = 64 位）";
                    break;
                case SYM_DESEDE:
                    byType = "3DES 密钥（16/24 字节 = 128/192 位）";
                    break;
                case SYM_SM4:
                    byType = "SM4 密钥（固定 16 字节 = 128 位）";
                    break;
                case HMAC:
                    byType = "HMAC 密钥（任意长度）";
                    break;
                case RSA_PUBLIC:
                    byType = "RSA 公钥。可粘贴：① PEM（-----BEGIN PUBLIC KEY-----）② X.509 DER 的 Base64 ③ PKCS1 PEM（-----BEGIN RSA PUBLIC KEY-----）";
                    break;
                case RSA_PRIVATE:
                    byType = "RSA 私钥。可粘贴：① PEM（-----BEGIN PRIVATE KEY-----）② PKCS8 DER 的 Base64 ③ PKCS1 PEM（-----BEGIN RSA PRIVATE KEY-----）";
                    break;
                case SM2_PUBLIC:
                    byType = "SM2 公钥。可粘贴：① X.509 的 Base64/PEM ② 裸点 04|X|Y（65 字节）或仅 X|Y（64 字节）的 Base64/Hex";
                    break;
                case SM2_PRIVATE:
                    byType = "SM2 私钥。可粘贴：① PKCS8 的 Base64/PEM ② 32 字节裸私钥的 Base64/Hex";
                    break;
                case RSA_KEYPAIR:
                    byType = "RSA 密钥对：在「公钥」「私钥」两个输入框分别粘贴公钥与私钥（格式同上，公钥 X.509/PKCS1，私钥 PKCS8/PKCS1）";
                    break;
                case SM2_KEYPAIR:
                    byType = "SM2 密钥对：在「公钥」「私钥」两个输入框分别粘贴公钥与私钥（公钥 X.509/裸点，私钥 PKCS8/32 字节裸私钥）";
                    break;
                default:
                    byType = "";
            }
        }
        String byFormat;
        if (fmt == KeyFormat.PLAIN) {
            byFormat = "当前格式『明文』：输入框里的字符直接按 UTF-8 取字节（字节数须满足上面的长度要求）";
        } else if (fmt == KeyFormat.HEX) {
            byFormat = "当前格式『Hex』：形如 fd23a066… 的十六进制串（大小写均可，忽略空格）";
        } else if (fmt == KeyFormat.PEM) {
            byFormat = "当前格式『PEM』：包含 -----BEGIN…----- 头尾的完整文本；无头尾时按裸 Base64 解析";
        } else {
            byFormat = "当前格式『Base64』：标准 Base64 字符串（忽略空白）";
        }
        guidanceArea.setText("【" + (type == null ? "" : type.getDisplay()) + "】" + byType
                + "\n" + byFormat + "\n保存时校验并统一规范化为 Base64 存储；密钥库面板中可随时查看 Base64/Hex/明文 多种格式。");
        guidanceArea.setCaretPosition(0);
    }

    private void save() throws Exception {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            throw new KeyStoreException("名称不能为空");
        }
        String scenario = (String) scenarioCombo.getSelectedItem();
        if (scenario == null) {
            throw new KeyStoreException("没有可选场景，请先新建场景");
        }
        KeyEntryType type = (KeyEntryType) typeCombo.getSelectedItem();
        KeyFormat fmt = (KeyFormat) formatCombo.getSelectedItem();
        String value;
        if (isPairType()) {
            value = buildPairValue(type, fmt);
        } else {
            value = KeyStoreCodec.normalize(type, valueArea.getText(), fmt);
        }
        String remark = remarkField.getText().trim();
        if (existing == null) {
            AppContext.keyStore.addEntry(scenario, name, type, value, remark);
        } else {
            existing.setScenario(scenario);
            existing.setName(name);
            existing.setRemark(remark);
            existing.setValue(value);
            AppContext.keyStore.updateEntry(existing);
        }
        SwingUtils.info(this, existing == null ? "导入成功" : "已更新");
        if (onSaved != null) {
            onSaved.run();
        }
        dispose();
    }

    /** 从两个独立输入框解析并组装密钥对条目值。 */
    private String buildPairValue(KeyEntryType type, KeyFormat fmt) throws CryptoException {
        String pub = publicKeyArea.getText().trim();
        String priv = privateKeyArea.getText().trim();
        if (pub.isEmpty()) {
            throw new CryptoException("请填写公钥");
        }
        if (priv.isEmpty()) {
            throw new CryptoException("请填写私钥");
        }
        String pubB64;
        String privB64;
        if (type == KeyEntryType.RSA_KEYPAIR) {
            pubB64 = KeyCodec.toBase64(KeyCodec.parseRsaPublicKey(pub, fmt));
            privB64 = KeyCodec.toBase64(KeyCodec.parseRsaPrivateKey(priv, fmt));
        } else {
            pubB64 = KeyCodec.toBase64JcaPublic(KeyCodec.parseSm2PublicKey(pub, fmt));
            privB64 = KeyCodec.toBase64JcaPrivate(KeyCodec.parseSm2PrivateKey(priv, fmt));
        }
        return KeyStoreCodec.buildKeyPairValue(pubB64, privB64);
    }
}
