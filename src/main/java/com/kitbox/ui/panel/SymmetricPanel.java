package com.kitbox.ui.panel;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.FileCryptoService;
import com.kitbox.crypto.JasyptService;
import com.kitbox.crypto.SymmetricService;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.JasyptAlgorithm;
import com.kitbox.crypto.model.JasyptIvGenerator;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyPickerField;
import com.kitbox.ui.components.SymmetricParamsForm;
import com.kitbox.ui.components.TextIOPane;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

/**
 * 对称加解密面板：文本加解密 + 文件加解密。
 */
public class SymmetricPanel extends JPanel {

    private final SymmetricParamsForm paramsForm = new SymmetricParamsForm();
    private final JComboBox<DataEncoding> cipherEncodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    /** 明文的输入/输出格式：文本 或 Hex（处理二进制数据时选 Hex） */
    private final JComboBox<ContentFormat> contentFormatCombo = new JComboBox<>(new ContentFormat[]{
            ContentFormat.TEXT, ContentFormat.HEX});
    private final TextIOPane io = new TextIOPane("明文 / 密文输入（文本按 UTF-8 处理）", "结果");

    // ---------------- Jasypt 配置加解密 ----------------

    private final JComboBox<JasyptAlgorithm> jasyptAlgoCombo = new JComboBox<>(JasyptAlgorithm.values());
    private final JComboBox<JasyptIvGenerator> jasyptIvCombo = new JComboBox<>(JasyptIvGenerator.values());
    private final JTextField jasyptIterationsField = new JTextField("1000", 6);
    private final JCheckBox jasyptWrapBox = new JCheckBox("结果用 ENC() 包裹", true);
    private final JTextField jasyptPwdField = new JTextField();
    private final TextIOPane jasyptIo = new TextIOPane("明文 / 密文输入（密文可带 ENC() 包裹）", "结果");

    public SymmetricPanel() {
        setLayout(new BorderLayout());
        try {
            cipherEncodingCombo.setSelectedItem(
                    DataEncoding.valueOf(com.kitbox.AppContext.config.getDefaultOutputEncoding()));
        } catch (Exception ignored) {
        }
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("文本", buildTextTab());
        tabs.addTab("文件", buildFileTab());
        tabs.addTab("Jasypt 配置", buildJasyptTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ---------------- 文本 ----------------

    private JPanel buildTextTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel encodingRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        encodingRow.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        encodingRow.add(new JLabel("密文编码："));
        encodingRow.add(cipherEncodingCombo);
        encodingRow.add(new JLabel("    内容格式："));
        encodingRow.add(contentFormatCombo);
        JLabel contentHint = new JLabel("（处理二进制数据时选 Hex）");
        contentHint.setFont(contentHint.getFont().deriveFont(java.awt.Font.PLAIN,
                contentHint.getFont().getSize2D() - 1f));
        encodingRow.add(contentHint);

        JButton encrypt = new JButton("加密");
        JButton decrypt = new JButton("解密");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::doEncrypt));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::doDecrypt));
        clear.addActionListener(e -> io.clearAll());

        // 参数、编码与操作按钮固定在顶部，输入/输出区吃剩余空间，避免按钮被挤没
        JPanel north = new JPanel(new BorderLayout());
        north.add(paramsForm, BorderLayout.NORTH);
        north.add(encodingRow, BorderLayout.CENTER);
        north.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{encrypt, decrypt},
                new javax.swing.JComponent[]{clear}), BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(io, BorderLayout.CENTER);
        return panel;
    }

    private void doEncrypt() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入明文");
        }
        SymmetricParamsForm.Params p = paramsForm.readParams();
        byte[] plain;
        try {
            plain = contentFormat().decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("内容不是合法的 " + contentFormat().getDisplay() + " 格式");
        }
        byte[] cipher = SymmetricService.encrypt(plain, p.key, p.algo, p.mode, p.padding,
                p.iv, p.tagBits);
        io.setOutput(cipherEncoding(cipher));
        io.note("加密成功");
    }

    private void doDecrypt() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入密文");
        }
        SymmetricParamsForm.Params p = paramsForm.readParams();
        byte[] cipher;
        try {
            cipher = cipherEncoding().decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密文不是合法的 " + cipherEncoding().getDisplay() + " 内容");
        }
        byte[] plain = SymmetricService.decrypt(cipher, p.key, p.algo, p.mode, p.padding, p.iv, p.tagBits);
        io.setOutput(contentFormat().encode(plain));
        io.note("解密成功");
    }

    private ContentFormat contentFormat() {
        return (ContentFormat) contentFormatCombo.getSelectedItem();
    }

    private DataEncoding cipherEncoding() {
        return (DataEncoding) cipherEncodingCombo.getSelectedItem();
    }

    private String cipherEncoding(byte[] data) {
        return cipherEncoding().encode(data);
    }

    // ---------------- Jasypt ----------------

    private JPanel buildJasyptTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel algoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        algoRow.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        algoRow.add(SwingUtils.groupLabel("算法"));
        jasyptAlgoCombo.setPreferredSize(new java.awt.Dimension(250, jasyptAlgoCombo.getPreferredSize().height));
        algoRow.add(jasyptAlgoCombo);
        algoRow.add(new JLabel("迭代次数："));
        algoRow.add(jasyptIterationsField);
        algoRow.add(jasyptWrapBox);

        JPanel pwdRow = new JPanel(new BorderLayout(6, 0));
        pwdRow.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        pwdRow.add(SwingUtils.groupLabel("口令"), BorderLayout.WEST);
        pwdRow.add(jasyptPwdField, BorderLayout.CENTER);

        // IV 生成器仅对旧算法（PBEWithMD5AndDES）有意义；AES_256 布局固定携带随机 IV
        JPanel ivRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ivRow.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        ivRow.add(SwingUtils.groupLabel("IV 生成器"));
        jasyptIvCombo.setPreferredSize(new java.awt.Dimension(250, jasyptIvCombo.getPreferredSize().height));
        jasyptIvCombo.setToolTipText("仅旧算法生效：旧默认不生成独立 IV；RandomIvGenerator 为博客同款配置。"
                + "解密时自动尝试新旧三种布局，无需手选");
        ivRow.add(jasyptIvCombo);
        jasyptAlgoCombo.addActionListener(e ->
                jasyptIvCombo.setEnabled(jasyptAlgoCombo.getSelectedItem() == JasyptAlgorithm.MD5_DES));
        jasyptIvCombo.setEnabled(jasyptAlgoCombo.getSelectedItem() == JasyptAlgorithm.MD5_DES);

        JPanel rows = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        rows.add(algoRow, gc);
        gc.gridy = 1;
        rows.add(pwdRow, gc);
        gc.gridy = 2;
        rows.add(ivRow, gc);

        JButton encrypt = new JButton("加密");
        JButton decrypt = new JButton("解密");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::jasyptEncrypt));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::jasyptDecrypt));
        clear.addActionListener(e -> jasyptIo.clearAll());

        JPanel north = new JPanel(new BorderLayout());
        north.add(rows, BorderLayout.NORTH);
        north.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{encrypt, decrypt},
                new javax.swing.JComponent[]{clear}), BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(jasyptIo, BorderLayout.CENTER);
        return panel;
    }

    private void jasyptEncrypt() throws CryptoException {
        String input = jasyptIo.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入明文");
        }
        JasyptAlgorithm algo = (JasyptAlgorithm) jasyptAlgoCombo.getSelectedItem();
        JasyptIvGenerator iv = (JasyptIvGenerator) jasyptIvCombo.getSelectedItem();
        String result = JasyptService.encrypt(input, jasyptPassword(), algo, iv,
                jasyptIterations(), jasyptWrapBox.isSelected());
        jasyptIo.setOutput(result);
        if (algo == JasyptAlgorithm.MD5_DES && iv == JasyptIvGenerator.RANDOM) {
            jasyptIo.note("加密成功（随机盐与 IV 已嵌入密文，解密端自动识别新旧布局）");
        } else {
            jasyptIo.note("加密成功（随机" + (algo.getIvLength() > 0 ? "盐与 IV" : "盐") + "已嵌入密文）");
        }
    }

    private void jasyptDecrypt() throws CryptoException {
        String input = jasyptIo.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入密文");
        }
        JasyptAlgorithm algo = (JasyptAlgorithm) jasyptAlgoCombo.getSelectedItem();
        String result = JasyptService.decrypt(input, jasyptPassword(), algo, jasyptIterations());
        jasyptIo.setOutput(result);
        jasyptIo.note("解密成功");
    }

    private char[] jasyptPassword() throws CryptoException {
        String text = jasyptPwdField.getText();
        if (text.isEmpty()) {
            throw new CryptoException("请输入口令（jasypt.encryptor.password）");
        }
        return text.toCharArray();
    }

    private int jasyptIterations() throws CryptoException {
        String text = jasyptIterationsField.getText().trim();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new CryptoException("迭代次数必须是数字（Jasypt 默认 1000）");
        }
    }

    // ---------------- 文件 ----------------

    private JPanel buildFileTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JComboBox<SymmetricAlgorithm> algoCombo = new JComboBox<>(new SymmetricAlgorithm[]{
                SymmetricAlgorithm.AES, SymmetricAlgorithm.SM4});
        KeyPickerField keyField = new KeyPickerField(EnumSet.of(KeyEntryType.Kind.SYMMETRIC), null, true);

        JTextField sourceField = new JTextField(30);
        JTextField targetField = new JTextField(30);
        JButton browseSource = new JButton("浏览…");
        JButton browseTarget = new JButton("浏览…");
        JLabel status = new JLabel(" ");
        status.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));

        com.kitbox.ui.FormPanel form = new com.kitbox.ui.FormPanel();
        form.addField("算法：", algoCombo);
        form.addField("密钥：", keyField);
        form.addField("源文件：", rowWithButton(sourceField, browseSource));
        form.addField("目标文件：", rowWithButton(targetField, browseTarget));
        form.addGlue();

        browseSource.addActionListener(e -> chooseFile(sourceField, false));
        browseTarget.addActionListener(e -> chooseFile(targetField, true));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton encrypt = new JButton("加密文件");
        JButton decrypt = new JButton("解密文件");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        buttonBar.add(encrypt);
        buttonBar.add(decrypt);
        buttonBar.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.CENTER);
        north.add(buttonBar, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(status, BorderLayout.SOUTH);

        encrypt.addActionListener(e -> runFileOp(this, sourceField, targetField, status,
                (SymmetricAlgorithm) algoCombo.getSelectedItem(), keyField, true));
        decrypt.addActionListener(e -> runFileOp(this, sourceField, targetField, status,
                (SymmetricAlgorithm) algoCombo.getSelectedItem(), keyField, false));
        return panel;
    }

    private JPanel rowWithButton(JTextField field, JButton button) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.add(field, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return row;
    }

    private void chooseFile(JTextField field, boolean save) {
        JFileChooser chooser = new JFileChooser();
        if (save ? chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                : chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void runFileOp(JPanel parent, JTextField sourceField, JTextField targetField,
                                  JLabel status, SymmetricAlgorithm algo, KeyPickerField keyField,
                                  boolean encrypt) {
        SwingUtils.runWithCatch(parent, () -> {
            String source = sourceField.getText().trim();
            String target = targetField.getText().trim();
            if (source.isEmpty()) {
                throw new CryptoException("请选择源文件");
            }
            if (target.isEmpty()) {
                throw new CryptoException("请选择目标文件");
            }
            File sourceFile = new File(source);
            if (!sourceFile.isFile()) {
                throw new CryptoException("源文件不存在：" + source);
            }
            String keyText = keyField.getKeyText();
            if (keyText.isEmpty()) {
                throw new CryptoException("请输入密钥或从密钥库选择");
            }
            byte[] key = keyField.getKeyFormat().decode(keyText);
            if (!algo.isValidKeyLength(key.length)) {
                throw new CryptoException(algo.getDisplay() + " 密钥长度必须为 " + algo.keyLengthHint()
                        + "（当前 " + key.length * 8 + " 位）");
            }
            if (encrypt && sourceFile.length() > 100L * 1024 * 1024) {
                if (!SwingUtils.confirm(parent, "文件超过 100MB，整体加密可能占用大量内存，是否继续？")) {
                    return;
                }
            }
            status.setText("处理中…");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    byte[] fileBytes = Files.readAllBytes(sourceFile.toPath());
                    byte[] result = encrypt
                            ? FileCryptoService.encryptFile(fileBytes, key, algo)
                            : FileCryptoService.decryptFile(fileBytes, key);
                    Files.write(new File(target).toPath(), result);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        status.setText((encrypt ? "加密" : "解密") + "完成：" + target);
                        SwingUtils.info(parent, (encrypt ? "加密" : "解密") + "完成");
                    } catch (Exception e) {
                        status.setText(" ");
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        SwingUtils.error(parent, cause.getMessage());
                    }
                }
            }.execute();
        });
    }

    /** 简单的「标签+控件」行容器。 */
    private static class FormRow extends JPanel {
        FormRow(String label, javax.swing.JComponent component) {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            add(new JLabel(label));
            add(component);
        }
    }
}
