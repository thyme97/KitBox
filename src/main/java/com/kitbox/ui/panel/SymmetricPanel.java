package com.kitbox.ui.panel;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.FileCryptoService;
import com.kitbox.crypto.SymmetricService;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
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
        add(tabs, BorderLayout.CENTER);
    }

    // ---------------- 文本 ----------------

    private JPanel buildTextTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(paramsForm, BorderLayout.NORTH);
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
        top.add(encodingRow, BorderLayout.SOUTH);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton encrypt = new JButton("加密");
        JButton decrypt = new JButton("解密");
        JButton clear = new JButton("全部清空");
        buttonBar.add(encrypt);
        buttonBar.add(decrypt);
        buttonBar.add(clear);

        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::doEncrypt));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, this::doDecrypt));
        clear.addActionListener(e -> io.clearAll());

        panel.add(top, BorderLayout.NORTH);
        panel.add(buttonBar, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(io, BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);
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
        form.addHint("文件格式：CBFX 头 + 元数据 + GCM 密文（随机 IV 自动生成）。加密输出为本工具自有格式，仅能由本工具解密。");
        form.addGlue();

        browseSource.addActionListener(e -> chooseFile(sourceField, false));
        browseTarget.addActionListener(e -> chooseFile(targetField, true));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton encrypt = new JButton("加密文件");
        JButton decrypt = new JButton("解密文件");
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
