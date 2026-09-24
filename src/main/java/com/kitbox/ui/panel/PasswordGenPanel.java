package com.kitbox.ui.panel;

import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.tools.PasswordGeneratorService;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.List;

/**
 * 密码 / 随机密钥生成器面板：
 * 密码按字符集策略生成（每个勾选类别至少一个字符），随机密钥按字节长度输出 Base64 / Hex。
 */
public class PasswordGenPanel extends JPanel {

    private enum Mode {
        PASSWORD("密码"), KEY("随机密钥");

        private final String display;

        Mode(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static final String CARD_PASSWORD = "password";
    private static final String CARD_KEY = "key";

    private final JComboBox<Mode> modeCombo = new JComboBox<>(Mode.values());
    private final SpinnerNumberModel countModel = new SpinnerNumberModel(5, 1, 100, 1);

    private final SpinnerNumberModel lengthModel = new SpinnerNumberModel(16, 4, 256, 1);
    private final JCheckBox upperCheck = new JCheckBox("大写字母", true);
    private final JCheckBox lowerCheck = new JCheckBox("小写字母", true);
    private final JCheckBox digitCheck = new JCheckBox("数字", true);
    private final JCheckBox symbolCheck = new JCheckBox("符号", true);
    private final JCheckBox noAmbiguousCheck = new JCheckBox("排除易混淆字符（0O1lI）");

    private final SpinnerNumberModel bytesModel = new SpinnerNumberModel(32, 8, 512, 8);
    private final JComboBox<DataEncoding> keyEncodingCombo =
            new JComboBox<>(new DataEncoding[]{DataEncoding.BASE64, DataEncoding.HEX});

    private final CardLayout cards = new CardLayout();
    private final JPanel paramCards = new JPanel(cards);
    private final JTextArea output = new JTextArea(12, 40);

    public PasswordGenPanel() {
        super(new BorderLayout());

        FormPanel common = new FormPanel();
        common.addField("模式", modeCombo, false);
        common.addField("数量", new JSpinner(countModel), false);

        FormPanel passwordForm = new FormPanel();
        passwordForm.addField("长度", new JSpinner(lengthModel), false);
        JPanel classRow = SwingUtils.row(upperCheck, lowerCheck, digitCheck, symbolCheck);
        passwordForm.addFull(classRow);
        passwordForm.addFull(noAmbiguousCheck);

        FormPanel keyForm = new FormPanel();
        keyForm.addField("字节数", new JSpinner(bytesModel), false);
        keyForm.addField("编码", keyEncodingCombo, false);

        JPanel passwordCard = new JPanel(new BorderLayout());
        passwordCard.add(passwordForm, BorderLayout.NORTH);
        JPanel keyCard = new JPanel(new BorderLayout());
        keyCard.add(keyForm, BorderLayout.NORTH);
        paramCards.add(passwordCard, CARD_PASSWORD);
        paramCards.add(keyCard, CARD_KEY);
        modeCombo.addActionListener(e -> cards.show(paramCards,
                modeCombo.getSelectedItem() == Mode.PASSWORD ? CARD_PASSWORD : CARD_KEY));

        JButton generate = new JButton("生成");
        JButton copy = new JButton("复制全部");
        JButton clear = new JButton("清空");
        SwingUtils.uniformSize(generate, copy, clear);
        SwingUtils.stylePrimary(generate);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generate));
        copy.addActionListener(e -> {
            if (!output.getText().isEmpty()) {
                SwingUtils.copyToClipboard(output.getText());
                SwingUtils.showToast(copy, "已复制到剪贴板");
            }
        });
        clear.addActionListener(e -> output.setText(""));
        JPanel buttonBar = SwingUtils.actionBar(
                new javax.swing.JComponent[]{generate, copy},
                new javax.swing.JComponent[]{clear});

        output.setEditable(false);
        output.setFont(mono());
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(SwingUtils.cardBorder("结果（每行一条）"));
        outputPanel.add(new JScrollPane(output), BorderLayout.CENTER);

        JPanel north = new JPanel(new BorderLayout());
        north.add(common, BorderLayout.NORTH);
        north.add(paramCards, BorderLayout.CENTER);
        north.add(buttonBar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(outputPanel, BorderLayout.CENTER);
    }

    private void generate() {
        int count = (Integer) countModel.getValue();
        List<String> results;
        if (modeCombo.getSelectedItem() == Mode.PASSWORD) {
            results = PasswordGeneratorService.generatePasswords(
                    (Integer) lengthModel.getValue(),
                    upperCheck.isSelected(), lowerCheck.isSelected(),
                    digitCheck.isSelected(), symbolCheck.isSelected(),
                    noAmbiguousCheck.isSelected(), count);
        } else {
            results = PasswordGeneratorService.generateKeys(
                    (Integer) bytesModel.getValue(),
                    (DataEncoding) keyEncodingCombo.getSelectedItem(), count);
        }
        StringBuilder sb = new StringBuilder();
        for (String line : results) {
            sb.append(line).append('\n');
        }
        output.setText(sb.toString());
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(output.getText().trim());
            SwingUtils.showToast(output, "结果已自动复制");
        }
    }

    private static Font mono() {
        return SwingUtils.monoFont(new JTextArea().getFont().getSize());
    }
}
