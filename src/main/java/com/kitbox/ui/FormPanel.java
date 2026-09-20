package com.kitbox.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;

/**
 * 简易表单构建器（GridBagLayout）：标签在左、控件在右。
 */
public class FormPanel extends JPanel {

    private final GridBagConstraints gbc = new GridBagConstraints();
    private int row = 0;

    public FormPanel() {
        super(new GridBagLayout());
    }

    /** 添加一行「标签 + 控件」。 */
    public void addField(String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 8);
        add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 8);
        add(field, gbc);
        row++;
    }

    /** 添加跨两列的控件。 */
    public void addFull(JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 8);
        add(field, gbc);
        row++;
    }

    /** 添加灰色提示文字。 */
    public void addHint(String text) {
        JLabel hint = new JLabel(text);
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, hint.getFont().getSize2D() - 1f));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 8, 4, 8);
        add(hint, gbc);
        row++;
    }

    /** 结尾弹性填充。 */
    public void addGlue() {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(new JPanel(), gbc);
        row++;
    }
}
