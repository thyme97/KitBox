package com.kitbox.ui.panel;

import com.kitbox.crypto.AsymmetricService;
import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.model.AsymmetricAlgorithm;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.crypto.model.RsaPadding;
import com.kitbox.crypto.model.Sm2CipherMode;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyPairResultDialog;
import com.kitbox.ui.components.TextIOPane;
import com.kitbox.util.KeyCodec;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.EnumSet;

/**
 * 非对称加解密面板：RSA / SM2，超长文本自动分段（RSA）。
 */
public class AsymmetricPanel extends JPanel {

    private final JComboBox<AsymmetricAlgorithm> algoCombo = new JComboBox<>(AsymmetricAlgorithm.values());
    private final JComboBox<RsaPadding> rsaPaddingCombo = new JComboBox<>(new RsaPadding[]{
            RsaPadding.PKCS1, RsaPadding.OAEP_SHA1, RsaPadding.OAEP_SHA256});
    private final JComboBox<Sm2CipherMode> sm2ModeCombo = new JComboBox<>(new Sm2CipherMode[]{
            Sm2CipherMode.C1C3C2, Sm2CipherMode.C1C2C3});
    /** 明文内容格式：文本 或 Hex */
    private final JComboBox<ContentFormat> contentFormatCombo = new JComboBox<>(new ContentFormat[]{
            ContentFormat.TEXT, ContentFormat.HEX});
    /** 密文编码：加密输出 / 解密输入 */
    private final JComboBox<DataEncoding> cipherEncodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    private final JTextArea publicKeyArea = new JTextArea(3, 30);
    private final JTextArea privateKeyArea = new JTextArea(3, 30);
    private final JComboBox<KeyFormat> pubFormatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.BASE64, KeyFormat.HEX, KeyFormat.PEM});
    private final JComboBox<KeyFormat> privFormatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.BASE64, KeyFormat.HEX, KeyFormat.PEM});
    private final TextIOPane io = new TextIOPane("明文 / 密文输入（文本按 UTF-8 处理）", "结果");

    public AsymmetricPanel() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(buildParamForm(), BorderLayout.NORTH);

        JButton encrypt = new JButton("加密（公钥）");
        JButton decrypt = new JButton("解密（私钥）");
        JButton generate = new JButton("生成密钥对…");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::encrypt));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::decrypt));
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generate));
        clear.addActionListener(e -> io.clearAll());

        JPanel buttonBar = SwingUtils.actionBar(
                new javax.swing.JComponent[]{encrypt, decrypt},
                new javax.swing.JComponent[]{generate, clear});
        JPanel center = new JPanel(new BorderLayout());
        center.add(buttonBar, BorderLayout.NORTH);
        JPanel south = new JPanel(new BorderLayout());
        south.add(io, BorderLayout.CENTER);
        center.add(south, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        algoCombo.addActionListener(e -> refreshAlgoState());
        refreshAlgoState();
    }

    private JPanel buildParamForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;

        // 算法 + 编码（各一行，独立面板避免列宽拉扯）
        JPanel algoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        algoRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        algoRow.add(SwingUtils.groupLabel("算法"));
        algoRow.add(algoCombo);
        algoRow.add(new javax.swing.JLabel("RSA 填充："));
        algoRow.add(rsaPaddingCombo);
        algoRow.add(new javax.swing.JLabel("SM2 排列："));
        algoRow.add(sm2ModeCombo);
        JPanel encRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        encRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        encRow.add(SwingUtils.groupLabel("编码"));
        encRow.add(new javax.swing.JLabel("内容格式："));
        encRow.add(contentFormatCombo);
        encRow.add(new javax.swing.JLabel("密文编码："));
        encRow.add(cipherEncodingCombo);
        outer.gridy = 0;
        form.add(algoRow, outer);
        outer.gridy = 1;
        form.add(encRow, outer);

        // 公钥 / 私钥（标题 + 输入区 + 格式/密钥库按钮）
        publicKeyArea.setLineWrap(true);
        publicKeyArea.setFont(SwingUtils.monoFont(publicKeyArea.getFont().getSize()));
        privateKeyArea.setLineWrap(true);
        privateKeyArea.setFont(SwingUtils.monoFont(privateKeyArea.getFont().getSize()));

        JPanel pubRow = new JPanel(new BorderLayout(6, 0));
        pubRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        pubRow.add(SwingUtils.groupLabel("公钥"), BorderLayout.WEST);
        pubRow.add(new JScrollPane(publicKeyArea), BorderLayout.CENTER);
        pubRow.add(pubControls(), BorderLayout.EAST);
        JPanel privRow = new JPanel(new BorderLayout(6, 0));
        privRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        privRow.add(SwingUtils.groupLabel("私钥"), BorderLayout.WEST);
        privRow.add(new JScrollPane(privateKeyArea), BorderLayout.CENTER);
        privRow.add(privControls(), BorderLayout.EAST);
        outer.gridy = 2;
        form.add(pubRow, outer);
        outer.gridy = 3;
        form.add(privRow, outer);

        return form;
    }

    private JPanel pubControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(pubFormatCombo);
        JButton pick = new JButton("密钥库…");
        pick.addActionListener(e -> pickKey(true));
        row.add(pick);
        return row;
    }

    private JPanel privControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(privFormatCombo);
        JButton pick = new JButton("密钥库…");
        pick.addActionListener(e -> pickKey(false));
        row.add(pick);
        return row;
    }

    private void pickKey(boolean isPublic) {
        if (!com.kitbox.ui.components.KeyStoreDialogs.ensureUnlocked(this)) {
            return;
        }
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        JFrame owner = w instanceof JFrame ? (JFrame) w : null;
        AsymmetricAlgorithm algo = (AsymmetricAlgorithm) algoCombo.getSelectedItem();
        if (isPublic) {
            EnumSet<KeyEntryType.Kind> kinds = EnumSet.of(KeyEntryType.Kind.PUBLIC, KeyEntryType.Kind.KEYPAIR);
            com.kitbox.ui.components.KeyPickerDialog.pick(owner, kinds, entry -> {
                publicKeyArea.setText(com.kitbox.keystore.KeyStoreCodec.partValue(entry, KeyEntryType.Kind.PUBLIC));
                pubFormatCombo.setSelectedItem(KeyFormat.BASE64);
            });
        } else {
            EnumSet<KeyEntryType.Kind> kinds = EnumSet.of(KeyEntryType.Kind.PRIVATE, KeyEntryType.Kind.KEYPAIR);
            com.kitbox.ui.components.KeyPickerDialog.pick(owner, kinds, entry -> {
                privateKeyArea.setText(com.kitbox.keystore.KeyStoreCodec.partValue(entry, KeyEntryType.Kind.PRIVATE));
                privFormatCombo.setSelectedItem(KeyFormat.BASE64);
            });
        }
    }

    private void refreshAlgoState() {
        boolean isRsa = algoCombo.getSelectedItem() == AsymmetricAlgorithm.RSA;
        rsaPaddingCombo.setEnabled(isRsa);
        sm2ModeCombo.setEnabled(!isRsa);
    }

    private void encrypt() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入明文");
        }
        String pubText = publicKeyArea.getText().trim();
        if (pubText.isEmpty()) {
            throw new CryptoException("请填写公钥");
        }
        KeyFormat fmt = (KeyFormat) pubFormatCombo.getSelectedItem();
        ContentFormat contentFormat = (ContentFormat) contentFormatCombo.getSelectedItem();
        byte[] plain;
        try {
            plain = contentFormat.decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("内容不是合法的 " + contentFormat.getDisplay() + " 格式");
        }
        byte[] cipher;
        if (algoCombo.getSelectedItem() == AsymmetricAlgorithm.RSA) {
            PublicKey pub = KeyCodec.parseRsaPublicKey(pubText, fmt);
            cipher = AsymmetricService.rsaEncrypt(plain, pub,
                    (RsaPadding) rsaPaddingCombo.getSelectedItem());
        } else {
            org.bouncycastle.crypto.params.ECPublicKeyParameters pub =
                    KeyCodec.parseSm2PublicKey(pubText, fmt);
            cipher = AsymmetricService.sm2Encrypt(plain, pub,
                    (Sm2CipherMode) sm2ModeCombo.getSelectedItem());
        }
        DataEncoding outEncoding = (DataEncoding) cipherEncodingCombo.getSelectedItem();
        io.setOutput(outEncoding.encode(cipher));
        io.note("加密成功（" + outEncoding.getDisplay() + "）");
    }

    private void decrypt() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入密文");
        }
        String privText = privateKeyArea.getText().trim();
        if (privText.isEmpty()) {
            throw new CryptoException("请填写私钥");
        }
        KeyFormat fmt = (KeyFormat) privFormatCombo.getSelectedItem();
        DataEncoding inEncoding = (DataEncoding) cipherEncodingCombo.getSelectedItem();
        byte[] cipher;
        try {
            cipher = inEncoding.decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密文不是合法的 " + inEncoding.getDisplay() + " 内容");
        }
        byte[] plain;
        if (algoCombo.getSelectedItem() == AsymmetricAlgorithm.RSA) {
            PrivateKey priv = KeyCodec.parseRsaPrivateKey(privText, fmt);
            plain = AsymmetricService.rsaDecrypt(cipher, priv,
                    (RsaPadding) rsaPaddingCombo.getSelectedItem());
        } else {
            org.bouncycastle.crypto.params.ECPrivateKeyParameters priv =
                    KeyCodec.parseSm2PrivateKey(privText, fmt);
            plain = AsymmetricService.sm2Decrypt(cipher, priv,
                    (Sm2CipherMode) sm2ModeCombo.getSelectedItem());
        }
        io.setOutput(((ContentFormat) contentFormatCombo.getSelectedItem()).encode(plain));
        io.note("解密成功");
    }

    private void generate() throws CryptoException {
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        JFrame owner = w instanceof JFrame ? (JFrame) w : null;
        if (algoCombo.getSelectedItem() == AsymmetricAlgorithm.RSA) {
            Integer bits = (Integer) javax.swing.JOptionPane.showInputDialog(this, "选择 RSA 密钥长度：",
                    "生成 RSA 密钥对", javax.swing.JOptionPane.PLAIN_MESSAGE, null,
                    new Integer[]{1024, 2048, 3072, 4096}, 2048);
            if (bits == null) {
                return;
            }
            java.security.KeyPair kp = KeyCodec.generateRsaKeyPair(bits);
            new KeyPairResultDialog(owner, "RSA-" + bits + " 密钥对",
                    KeyEntryType.RSA_KEYPAIR, kp.getPublic(), kp.getPrivate()).setVisible(true);
        } else {
            java.security.KeyPair kp = KeyCodec.generateSm2KeyPair();
            new KeyPairResultDialog(owner, "SM2 密钥对",
                    KeyEntryType.SM2_KEYPAIR, kp.getPublic(), kp.getPrivate()).setVisible(true);
        }
    }
}
