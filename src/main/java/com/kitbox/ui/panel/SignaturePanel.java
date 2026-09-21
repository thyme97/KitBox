package com.kitbox.ui.panel;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.SignatureService;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.crypto.model.HmacAlgorithm;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyPickerField;
import com.kitbox.ui.components.TextIOPane;
import com.kitbox.util.KeyCodec;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.EnumSet;

/**
 * 加签 / 验签面板：RSA 系列、SM3withSM2、HMAC 系列。
 * 验签时从输出区读取签名值。
 */
public class SignaturePanel extends JPanel {

    private enum SigAlgorithm {
        MD5_RSA("MD5withRSA", Kind.RSA),
        SHA1_RSA("SHA1withRSA", Kind.RSA),
        SHA256_RSA("SHA256withRSA", Kind.RSA),
        SHA512_RSA("SHA512withRSA", Kind.RSA),
        SM3_SM2("SM3withSM2", Kind.SM2),
        HMAC_SHA256("HmacSHA256", Kind.HMAC),
        HMAC_SM3("HmacSM3", Kind.HMAC),
        HMAC_MD5("HmacMD5", Kind.HMAC);

        enum Kind { RSA, SM2, HMAC }

        private final String jcaName;
        private final Kind kind;

        SigAlgorithm(String jcaName, Kind kind) {
            this.jcaName = jcaName;
            this.kind = kind;
        }

        String jcaName() {
            return jcaName;
        }

        Kind kind() {
            return kind;
        }
    }

    private final JComboBox<SigAlgorithm> algoCombo = new JComboBox<>(SigAlgorithm.values());
    private final JComboBox<DataEncoding> sigEncodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    /** 原文格式：文本 或 Hex（对二进制数据加签时选 Hex） */
    private final JComboBox<ContentFormat> contentFormatCombo = new JComboBox<>(new ContentFormat[]{
            ContentFormat.TEXT, ContentFormat.HEX});
    private final JTextArea publicKeyArea = new JTextArea(2, 26);
    private final JTextArea privateKeyArea = new JTextArea(2, 26);
    private final JComboBox<KeyFormat> pubFormatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.BASE64, KeyFormat.HEX, KeyFormat.PEM});
    private final JComboBox<KeyFormat> privFormatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.BASE64, KeyFormat.HEX, KeyFormat.PEM});
    private final JTextField sm2IdField = new JTextField(SignatureService.DEFAULT_SM2_ID, 12);
    private final KeyPickerField hmacKeyField = new KeyPickerField(
            EnumSet.of(KeyEntryType.Kind.HMAC), null, true);
    private final CardLayout keyCards = new CardLayout();
    /**
     * 差异项卡片（SM2→签名者ID / HMAC→密钥 / RSA→空）。
     * 首选尺寸按当前可见卡片计算：否则最宽的 HMAC 卡片会撑大整行，
     * 导致「算法 / 原文格式 / 签名值编码 / 签名者ID」一行放不下。
     */
    private final JPanel keyCardPanel = new JPanel(keyCards) {
        @Override
        public Dimension getPreferredSize() {
            for (Component c : getComponents()) {
                if (c.isVisible()) {
                    return c.getPreferredSize();
                }
            }
            return super.getPreferredSize();
        }
    };
    private final JLabel verifyResult = new JLabel(" ");
    private final TextIOPane io = new TextIOPane("原文输入（验签时签名值取自下方结果区）", "签名值 / 验签结果");

    public SignaturePanel() {
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(buildParamForm(), BorderLayout.NORTH);

        JButton sign = new JButton("加签");
        JButton verify = new JButton("验签");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(sign);
        SwingUtils.styleSecondary(verify);
        sign.addActionListener(e -> SwingUtils.runWithCatch(this, this::sign));
        verify.addActionListener(e -> SwingUtils.runWithCatch(this, this::verify));
        clear.addActionListener(e -> {
            io.clearAll();
            verifyResult.setText(" ");
        });
        verifyResult.setFont(verifyResult.getFont().deriveFont(Font.BOLD, 14f));

        JPanel center = new JPanel(new BorderLayout());
        center.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{sign, verify},
                new javax.swing.JComponent[]{clear}), BorderLayout.NORTH);
        center.add(verifyResult, BorderLayout.SOUTH);
        JPanel south = new JPanel(new BorderLayout());
        south.add(io, BorderLayout.CENTER);
        center.add(south, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        algoCombo.addActionListener(e -> refreshKind());
        refreshKind();
    }

    private JPanel buildParamForm() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：算法 / 原文格式 / 签名值编码 + 差异项（SM2→签名者ID、HMAC→密钥、RSA→无）
        // WrapLayout：窗口宽度不足时整行折行且高度按实际行数计算，避免组件被裁成一条缝
        JPanel paramRow = new JPanel(new com.kitbox.ui.components.WrapLayout(FlowLayout.LEFT, 6, 4));
        paramRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        paramRow.add(new JLabel("算法："));
        paramRow.add(algoCombo);
        paramRow.add(new JLabel("原文格式："));
        paramRow.add(contentFormatCombo);
        paramRow.add(new JLabel("签名值编码："));
        paramRow.add(sigEncodingCombo);
        JPanel sm2IdCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        sm2IdCard.setOpaque(false);
        sm2IdCard.add(new JLabel("签名者ID："));
        sm2IdCard.add(sm2IdField);
        keyCardPanel.add(sm2IdCard, SigAlgorithm.Kind.SM2.name());
        keyCardPanel.add(buildHmacCard(), SigAlgorithm.Kind.HMAC.name());
        keyCardPanel.add(new JPanel(), SigAlgorithm.Kind.RSA.name());
        paramRow.add(keyCardPanel);
        outer.gridy = 0;
        form.add(paramRow, outer);

        // 公钥 / 私钥行：行隔离，输入区拉伸、格式与密钥库按钮靠右
        publicKeyArea.setLineWrap(true);
        publicKeyArea.setRows(3);
        publicKeyArea.setFont(SwingUtils.monoFont(publicKeyArea.getFont().getSize()));
        privateKeyArea.setLineWrap(true);
        privateKeyArea.setRows(3);
        privateKeyArea.setFont(SwingUtils.monoFont(privateKeyArea.getFont().getSize()));

        JPanel pubRow = new JPanel(new BorderLayout(6, 0));
        pubRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        pubRow.add(new JLabel("公钥（验签）："), BorderLayout.WEST);
        pubRow.add(new JScrollPane(publicKeyArea), BorderLayout.CENTER);
        pubRow.add(pubControls(), BorderLayout.EAST);
        JPanel privRow = new JPanel(new BorderLayout(6, 0));
        privRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        privRow.add(new JLabel("私钥（加签）："), BorderLayout.WEST);
        privRow.add(new JScrollPane(privateKeyArea), BorderLayout.CENTER);
        privRow.add(privControls(), BorderLayout.EAST);
        outer.gridy = 1;
        form.add(pubRow, outer);
        outer.gridy = 2;
        form.add(privRow, outer);
        return form;
    }

    private JPanel buildHmacCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        card.add(new JLabel("HMAC 密钥："));
        card.add(hmacKeyField);
        return card;
    }

    private JPanel pubControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(pubFormatCombo);
        JButton pickPub = new JButton("密钥库…");
        pickPub.addActionListener(e -> pickKey(true));
        row.add(pickPub);
        return row;
    }

    private JPanel privControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(privFormatCombo);
        JButton pickPriv = new JButton("密钥库…");
        pickPriv.addActionListener(e -> pickKey(false));
        row.add(pickPriv);
        return row;
    }

    private void pickKey(boolean isPublic) {
        if (!com.kitbox.ui.components.KeyStoreDialogs.ensureUnlocked(this)) {
            return;
        }
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        javax.swing.JFrame owner = w instanceof javax.swing.JFrame ? (javax.swing.JFrame) w : null;
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

    private void refreshKind() {
        SigAlgorithm algo = (SigAlgorithm) algoCombo.getSelectedItem();
        keyCards.show(keyCardPanel, algo.kind().name());
    }

    private void sign() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入原文");
        }
        SigAlgorithm algo = (SigAlgorithm) algoCombo.getSelectedItem();
        ContentFormat contentFormat = (ContentFormat) contentFormatCombo.getSelectedItem();
        byte[] data;
        try {
            data = contentFormat.decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("原文不是合法的 " + contentFormat.getDisplay() + " 格式");
        }
        byte[] sig;
        switch (algo.kind()) {
            case RSA: {
                String privText = privateKeyArea.getText().trim();
                if (privText.isEmpty()) {
                    throw new CryptoException("请填写私钥");
                }
                PrivateKey priv = KeyCodec.parseRsaPrivateKey(privText, (KeyFormat) privFormatCombo.getSelectedItem());
                sig = SignatureService.rsaSign(data, priv, algo.jcaName());
                break;
            }
            case SM2: {
                String privText = privateKeyArea.getText().trim();
                if (privText.isEmpty()) {
                    throw new CryptoException("请填写私钥");
                }
                org.bouncycastle.crypto.params.ECPrivateKeyParameters priv =
                        KeyCodec.parseSm2PrivateKey(privText, (KeyFormat) privFormatCombo.getSelectedItem());
                sig = SignatureService.sm2Sign(data, priv, sm2IdField.getText().trim());
                break;
            }
            case HMAC:
            default: {
                String keyText = hmacKeyField.getKeyText();
                if (keyText.isEmpty()) {
                    throw new CryptoException("请填写 HMAC 密钥");
                }
                byte[] key = hmacKeyField.getKeyFormat().decode(keyText);
                HmacAlgorithm hmac = toHmac(algo);
                sig = SignatureService.hmacSign(data, key, hmac);
                break;
            }
        }
        DataEncoding enc = (DataEncoding) sigEncodingCombo.getSelectedItem();
        io.setOutput(enc.encode(sig));
        io.note("加签成功");
    }

    private void verify() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入原文");
        }
        String sigText = io.getOutput().trim();
        if (sigText.isEmpty()) {
            throw new CryptoException("签名值为空：请先加签（结果区），或粘贴签名值到结果区");
        }
        SigAlgorithm algo = (SigAlgorithm) algoCombo.getSelectedItem();
        DataEncoding enc = (DataEncoding) sigEncodingCombo.getSelectedItem();
        ContentFormat contentFormat = (ContentFormat) contentFormatCombo.getSelectedItem();
        byte[] data;
        try {
            data = contentFormat.decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("原文不是合法的 " + contentFormat.getDisplay() + " 格式");
        }
        byte[] sig;
        try {
            sig = enc.decode(sigText);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("签名值不是合法的 " + enc.getDisplay() + " 内容");
        }

        boolean ok;
        switch (algo.kind()) {
            case RSA: {
                String pubText = publicKeyArea.getText().trim();
                if (pubText.isEmpty()) {
                    throw new CryptoException("请填写公钥");
                }
                PublicKey pub = KeyCodec.parseRsaPublicKey(pubText, (KeyFormat) pubFormatCombo.getSelectedItem());
                ok = SignatureService.rsaVerify(data, sig, pub, algo.jcaName());
                break;
            }
            case SM2: {
                String pubText = publicKeyArea.getText().trim();
                if (pubText.isEmpty()) {
                    throw new CryptoException("请填写公钥");
                }
                org.bouncycastle.crypto.params.ECPublicKeyParameters pub =
                        KeyCodec.parseSm2PublicKey(pubText, (KeyFormat) pubFormatCombo.getSelectedItem());
                ok = SignatureService.sm2Verify(data, sig, pub, sm2IdField.getText().trim());
                break;
            }
            case HMAC:
            default: {
                String keyText = hmacKeyField.getKeyText();
                if (keyText.isEmpty()) {
                    throw new CryptoException("请填写 HMAC 密钥");
                }
                byte[] key = hmacKeyField.getKeyFormat().decode(keyText);
                ok = SignatureService.hmacVerify(data, sig, key, toHmac(algo));
                break;
            }
        }
        verifyResult.setText(ok ? "✓ 验签通过" : "✗ 验签失败");
        verifyResult.setForeground(ok ? new Color(46, 125, 50) : new Color(198, 40, 40));
        io.note(ok ? "验签通过" : "验签失败");
    }

    private HmacAlgorithm toHmac(SigAlgorithm algo) throws CryptoException {
        switch (algo) {
            case HMAC_SHA256:
                return HmacAlgorithm.HMAC_SHA256;
            case HMAC_SM3:
                return HmacAlgorithm.HMAC_SM3;
            case HMAC_MD5:
                return HmacAlgorithm.HMAC_MD5;
            default:
                throw new CryptoException("该算法不支持 HMAC");
        }
    }
}
