package com.kitbox.ui.panel;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.JsonFieldCryptoService;
import com.kitbox.crypto.SymmetricService;
import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyPickerField;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 字段级加解密面板。
 * 字段值格式：Base64(随机IV(12B) || AES/SM4-GCM 密文)。
 */
public class JsonFieldPanel extends JPanel {

    private final JTextArea jsonArea = new JTextArea(12, 30);
    private final JTextArea pathsArea = new JTextArea(12, 16);
    private final JComboBox<SymmetricAlgorithm> algoCombo = new JComboBox<>(new SymmetricAlgorithm[]{
            SymmetricAlgorithm.AES, SymmetricAlgorithm.SM4});
    private final KeyPickerField keyField = new KeyPickerField(
            java.util.EnumSet.of(KeyEntryType.Kind.SYMMETRIC), null, true);
    private final JTextArea outputArea = new JTextArea(10, 40);

    public JsonFieldPanel() {
        setLayout(new BorderLayout());

        jsonArea.setFont(SwingUtils.monoFont(jsonArea.getFont().getSize()));
        pathsArea.setFont(SwingUtils.monoFont(pathsArea.getFont().getSize()));
        outputArea.setFont(SwingUtils.monoFont(outputArea.getFont().getSize()));
        outputArea.setEditable(false);

        JPanel editorPanel = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        javax.swing.JScrollPane jsonScroll = new javax.swing.JScrollPane(jsonArea);
        jsonScroll.setBorder(SwingUtils.cardBorder("JSON 输入"));
        javax.swing.JScrollPane pathsScroll = new javax.swing.JScrollPane(pathsArea);
        pathsScroll.setBorder(SwingUtils.cardBorder(
                "字段路径（每行一条，如 $.data.idCard、$.list[*].phone）"));
        editorPanel.add(jsonScroll);
        editorPanel.add(pathsScroll);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("算法："), gbc);
        gbc.gridx = 1;
        form.add(algoCombo, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("密钥："), gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(keyField, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;



        JButton encrypt = new JButton("加密字段");
        JButton decrypt = new JButton("解密字段");
        JButton copy = new JButton("复制结果");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, () -> process(true)));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, () -> process(false)));
        copy.addActionListener(e -> {
            if (!outputArea.getText().isEmpty()) {
                SwingUtils.copyToClipboard(outputArea.getText());
            }
        });
        clear.addActionListener(e -> {
            jsonArea.setText("");
            pathsArea.setText("");
            outputArea.setText("");
        });

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(SwingUtils.cardBorder("结果"));
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.add(form, BorderLayout.NORTH);
        JPanel north = new JPanel(new BorderLayout());
        north.add(editorPanel, BorderLayout.CENTER);
        north.add(formWrap, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        JPanel buttonBar = SwingUtils.actionBar(
                new javax.swing.JComponent[]{encrypt, decrypt},
                new javax.swing.JComponent[]{copy, clear});
        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonBar, BorderLayout.NORTH);
        south.add(outputPanel, BorderLayout.CENTER);
        add(south, BorderLayout.CENTER);
    }

    private void process(boolean encrypt) throws CryptoException {
        String json = jsonArea.getText();
        if (json.trim().isEmpty()) {
            throw new CryptoException("请输入 JSON");
        }
        List<String> paths = new ArrayList<>();
        for (String line : pathsArea.getText().split("\\r?\\n")) {
            if (!line.trim().isEmpty()) {
                paths.add(line.trim());
            }
        }
        if (paths.isEmpty()) {
            throw new CryptoException("请填写字段路径");
        }
        String keyText = keyField.getKeyText();
        if (keyText.isEmpty()) {
            throw new CryptoException("请输入密钥或从密钥库选择");
        }
        SymmetricAlgorithm algo = (SymmetricAlgorithm) algoCombo.getSelectedItem();
        byte[] key = keyField.getKeyFormat().decode(keyText);
        if (!algo.isValidKeyLength(key.length)) {
            throw new CryptoException(algo.getDisplay() + " 密钥长度必须为 " + algo.keyLengthHint()
                    + "（当前 " + key.length * 8 + " 位）");
        }
        JsonFieldCryptoService.FieldValueCipher cipher = new FieldCipherImpl(algo, key);
        String result = JsonFieldCryptoService.process(json, paths, encrypt, cipher,
                com.kitbox.AppContext.config.isPrettyJson());
        outputArea.setText(result);
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(result);
        }
    }

    /** 字段值：Base64(iv ‖ GCM 密文)。 */
    private static class FieldCipherImpl implements JsonFieldCryptoService.FieldValueCipher {
        private final SymmetricAlgorithm algo;
        private final byte[] key;

        FieldCipherImpl(SymmetricAlgorithm algo, byte[] key) {
            this.algo = algo;
            this.key = key;
        }

        @Override
        public String encrypt(String plain) throws CryptoException {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            byte[] ct = SymmetricService.encrypt(DataEncoding.utf8(plain), key, algo, CryptoMode.GCM,
                    Padding.NO_PADDING, iv, SymmetricService.DEFAULT_GCM_TAG_BITS);
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return DataEncoding.BASE64.encode(out);
        }

        @Override
        public String decrypt(String cipherText) throws CryptoException {
            byte[] packed = DataEncoding.BASE64.decode(cipherText.trim());
            if (packed.length <= 12) {
                throw new CryptoException("字段密文格式不正确（长度不足）");
            }
            byte[] iv = new byte[12];
            byte[] ct = new byte[packed.length - 12];
            System.arraycopy(packed, 0, iv, 0, 12);
            System.arraycopy(packed, 12, ct, 0, ct.length);
            return DataEncoding.utf8(SymmetricService.decrypt(ct, key, algo, CryptoMode.GCM,
                    Padding.NO_PADDING, iv, SymmetricService.DEFAULT_GCM_TAG_BITS));
        }
    }
}
