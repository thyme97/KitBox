package com.kitbox.ui.panel;

import com.kitbox.AppContext;
import com.kitbox.config.MessageTemplate;
import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.MessageFormatService;
import com.kitbox.crypto.SymmetricService;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.SymmetricParamsForm;
import com.kitbox.ui.components.TextIOPane;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 固定报文格式加解密面板：前缀 + 密文(Base64/Hex) + 后缀。
 */
public class MessageFormatPanel extends JPanel {

    private static final String CUSTOM = "（自定义）";

    private final JComboBox<String> templateCombo = new JComboBox<>();
    private final JTextField prefixField = new JTextField(12);
    private final JTextField suffixField = new JTextField(12);
    private final JComboBox<DataEncoding> encodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    private final SymmetricParamsForm paramsForm = new SymmetricParamsForm();
    private final TextIOPane io = new TextIOPane("明文 / 报文输入", "结果");

    public MessageFormatPanel() {
        setLayout(new BorderLayout());

        templateCombo.addActionListener(e -> applyTemplate());
        refreshTemplates();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(SwingUtils.cardBorder(
                "报文格式：前缀 + 密文 + 后缀（前后缀可留空，留空即为裸密文）"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // 第 1 行：模板
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("模板："), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(templateCombo, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // 第 2 行：前缀 / 后缀 / 内容编码（输入框可随窗口伸展）
        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("前缀："), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(prefixField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("后缀："), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(suffixField, gbc);
        gbc.gridx = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        form.add(new JLabel("内容编码："), gbc);
        gbc.gridx = 5;
        form.add(encodingCombo, gbc);

        // 第 3 行：对称算法参数
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        form.add(paramsForm, gbc);
        gbc.gridwidth = 1;

        JButton encrypt = new JButton("加密成报文");
        JButton decrypt = new JButton("解密报文");
        JButton saveTemplate = new JButton("保存为模板…");
        JButton deleteTemplate = new JButton("删除模板");
        JButton clear = new JButton("全部清空");
        SwingUtils.stylePrimary(encrypt);
        SwingUtils.styleSecondary(decrypt);
        SwingUtils.styleDanger(deleteTemplate);
        encrypt.addActionListener(e -> SwingUtils.runWithCatch(this, () -> process(true)));
        decrypt.addActionListener(e -> SwingUtils.runWithCatch(this, () -> process(false)));
        saveTemplate.addActionListener(e -> SwingUtils.runWithCatch(this, this::saveTemplate));
        deleteTemplate.addActionListener(e -> SwingUtils.runWithCatch(this, this::deleteTemplate));
        clear.addActionListener(e -> io.clearAll());

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.NORTH);
        north.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{encrypt, decrypt, saveTemplate},
                new javax.swing.JComponent[]{deleteTemplate, clear}), BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(io, BorderLayout.CENTER);
    }

    private void refreshTemplates() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement(CUSTOM);
        for (MessageTemplate t : AppContext.config.getTemplates()) {
            model.addElement(t.getName());
        }
        templateCombo.setModel(model);
        templateCombo.setSelectedItem(CUSTOM);
    }

    private void applyTemplate() {
        Object selected = templateCombo.getSelectedItem();
        if (selected == null || CUSTOM.equals(selected)) {
            return;
        }
        for (MessageTemplate t : AppContext.config.getTemplates()) {
            if (t.getName().equals(selected)) {
                prefixField.setText(t.getPrefix());
                suffixField.setText(t.getSuffix());
                try {
                    encodingCombo.setSelectedItem(DataEncoding.valueOf(t.getContentEncoding()));
                } catch (Exception ignored) {
                }
                return;
            }
        }
    }

    private void saveTemplate() {
        String name = javax.swing.JOptionPane.showInputDialog(this, "模板名称：", "保存模板",
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        name = name.trim();
        for (MessageTemplate t : AppContext.config.getTemplates()) {
            if (t.getName().equals(name)) {
                if (!SwingUtils.confirm(this, "模板已存在：" + name + "，是否用当前前缀/后缀/编码覆盖？")) {
                    return;
                }
                t.setPrefix(prefixField.getText());
                t.setSuffix(suffixField.getText());
                t.setContentEncoding(((DataEncoding) encodingCombo.getSelectedItem()).name());
                AppContext.saveConfig();
                refreshTemplates();
                templateCombo.setSelectedItem(name);
                SwingUtils.info(this, "模板已覆盖：" + name);
                return;
            }
        }
        AppContext.config.getTemplates().add(new MessageTemplate(name,
                prefixField.getText(), suffixField.getText(),
                ((DataEncoding) encodingCombo.getSelectedItem()).name()));
        AppContext.saveConfig();
        refreshTemplates();
        templateCombo.setSelectedItem(name);
        SwingUtils.info(this, "模板已保存：" + name);
    }

    private void deleteTemplate() {
        Object selected = templateCombo.getSelectedItem();
        if (selected == null || CUSTOM.equals(selected)) {
            SwingUtils.info(this, "请先选择要删除的模板");
            return;
        }
        if (!SwingUtils.confirm(this, "确定删除模板：" + selected + " ？")) {
            return;
        }
        AppContext.config.getTemplates().removeIf(t -> t.getName().equals(selected));
        AppContext.saveConfig();
        refreshTemplates();
    }

    private MessageFormatService.TextCipher cipher() throws CryptoException {
        SymmetricParamsForm.Params p = paramsForm.readParams();
        return new MessageFormatService.TextCipher() {
            @Override
            public byte[] encrypt(byte[] plain) throws CryptoException {
                return SymmetricService.encrypt(plain, p.key, p.algo, p.mode, p.padding, p.iv, p.tagBits);
            }

            @Override
            public byte[] decrypt(byte[] cipherBytes) throws CryptoException {
                return SymmetricService.decrypt(cipherBytes, p.key, p.algo, p.mode, p.padding, p.iv, p.tagBits);
            }
        };
    }

    private void process(boolean encrypt) throws CryptoException {
        MessageFormatService.TextCipher cipher = cipher();
        DataEncoding encoding = (DataEncoding) encodingCombo.getSelectedItem();
        String prefix = prefixField.getText();
        String suffix = suffixField.getText();
        if (encrypt) {
            String input = io.getInput();
            io.setOutput(MessageFormatService.encrypt(input, prefix, suffix, encoding, cipher));
            io.note("加密成功");
        } else {
            String input = io.getInput();
            io.setOutput(MessageFormatService.decrypt(input, prefix, suffix, encoding, cipher));
            io.note("解密成功");
        }
    }
}
