package com.kitbox.ui.panel;

import com.kitbox.AppContext;
import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.DigestService;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.DigestAlgorithm;
import com.kitbox.crypto.model.HmacAlgorithm;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyPickerField;
import com.kitbox.ui.components.TextIOPane;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
import java.util.EnumSet;

/**
 * 摘要与 HMAC 面板。
 */
public class DigestPanel extends JPanel {

    private final JComboBox<DigestAlgorithm> algoCombo = new JComboBox<>(DigestAlgorithm.values());
    private final JCheckBox hmacCheck = new JCheckBox("HMAC 模式");
    private final JComboBox<HmacAlgorithm> hmacCombo = new JComboBox<>(HmacAlgorithm.values());
    private final KeyPickerField keyField = new KeyPickerField(
            EnumSet.of(KeyEntryType.Kind.HMAC), null, true);
    private final JComboBox<String> outputFormatCombo = new JComboBox<>(new String[]{"Hex 小写", "Hex 大写", "Base64"});
    /** 原文格式：文本 或 Hex（对二进制数据计算摘要时选 Hex） */
    private final JComboBox<ContentFormat> contentFormatCombo = new JComboBox<>(new ContentFormat[]{
            ContentFormat.TEXT, ContentFormat.HEX});
    private final JTextField compareField = new JTextField(40);
    private final JLabel compareResult = new JLabel(" ");
    private final TextIOPane io = new TextIOPane("原文输入（文本按 UTF-8 处理）", "摘要 / HMAC 结果");

    public DigestPanel() {
        setLayout(new BorderLayout());

        com.kitbox.ui.FormPanel form = new com.kitbox.ui.FormPanel();
        form.addField("算法：", rowOf(algoCombo, hmacCheck));
        form.addField("HMAC 算法：", rowOf(hmacCombo, keyField));
        form.addField("原文格式：", contentFormatCombo);
        form.addField("输出格式：", outputFormatCombo);
        form.addHint("HMAC 模式需要密钥：支持明文 / Base64 / Hex，或从密钥库选择。");
        form.addGlue();

        JPanel comparePanel = new JPanel(new BorderLayout(6, 0));
        comparePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("摘要比对（可选）"));
        comparePanel.add(compareField, BorderLayout.CENTER);
        compareResult.setFont(compareResult.getFont().deriveFont(Font.BOLD));
        comparePanel.add(compareResult, BorderLayout.EAST);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton compute = new JButton("计算摘要");
        JButton clear = new JButton("全部清空");
        buttonBar.add(compute);
        buttonBar.add(clear);
        compute.addActionListener(e -> SwingUtils.runWithCatch(this, this::compute));
        clear.addActionListener(e -> {
            io.clearAll();
            compareResult.setText(" ");
        });

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.CENTER);
        north.add(comparePanel, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(buttonBar, BorderLayout.NORTH);
        JPanel south = new JPanel(new BorderLayout());
        south.add(io, BorderLayout.CENTER);
        center.add(south, BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        updateKeyState();
        hmacCheck.addActionListener(e -> updateKeyState());
        outputFormatCombo.setSelectedIndex(AppContext.config.isDigestUppercase() ? 1 : 0);
    }

    private JPanel rowOf(javax.swing.JComponent a, javax.swing.JComponent b) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(a);
        row.add(b);
        return row;
    }

    private void updateKeyState() {
        boolean hmac = hmacCheck.isSelected();
        hmacCombo.setEnabled(hmac);
        keyField.setEnabled(hmac);
    }

    private void compute() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入原文");
        }
        ContentFormat contentFormat = (ContentFormat) contentFormatCombo.getSelectedItem();
        byte[] data;
        try {
            data = contentFormat.decode(input);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("原文不是合法的 " + contentFormat.getDisplay() + " 格式");
        }
        byte[] digest;
        if (hmacCheck.isSelected()) {
            String keyText = keyField.getKeyText();
            if (keyText.isEmpty()) {
                throw new CryptoException("HMAC 模式需要密钥");
            }
            byte[] key;
            try {
                key = keyField.getKeyFormat().decode(keyText);
            } catch (IllegalArgumentException e) {
                throw new CryptoException("密钥格式不正确：" + e.getMessage());
            }
            digest = DigestService.hmac(data, key, (HmacAlgorithm) hmacCombo.getSelectedItem());
        } else {
            digest = DigestService.digest(data, (DigestAlgorithm) algoCombo.getSelectedItem());
        }

        String format = (String) outputFormatCombo.getSelectedItem();
        String output;
        if ("Base64".equals(format)) {
            output = DataEncoding.BASE64.encode(digest);
        } else {
            output = DataEncoding.HEX.encode(digest);
            if ("Hex 大写".equals(format)) {
                output = output.toUpperCase();
            }
        }
        io.setOutput(output);
        io.note("计算完成");

        String expect = compareField.getText().trim();
        if (!expect.isEmpty()) {
            boolean match = expect.equalsIgnoreCase(output);
            compareResult.setText(match ? "✓ 一致" : "✗ 不一致");
            compareResult.setForeground(match ? new Color(46, 125, 50) : new Color(198, 40, 40));
        } else {
            compareResult.setText(" ");
        }
    }
}
