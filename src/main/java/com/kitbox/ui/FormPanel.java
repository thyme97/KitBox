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

    /** 添加一行「标签 + 控件」，控件横向拉伸占满剩余宽度（适合文本框）。 */
    public void addField(String label, JComponent field) {
        addField(label, field, true);
    }

    /**
     * 添加一行「标签 + 控件」。stretch=false 时不拉伸，
     * 适合下拉框、数字框等固定宽度的控件，避免被拉成通栏长条。
     */
    public void addField(String label, JComponent field, boolean stretch) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 4, 8);
        add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = stretch ? 1 : 0;
        gbc.fill = stretch ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 0, 4, 8);
        add(field, gbc);
        // 行尾弹性列：整行都是固定宽度控件时吸收多余空间，保证内容左对齐
        addFiller(row, stretch ? 0 : 0.001);
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
        addFiller(row, 0.001);
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
        addFiller(row, 0.001);
        row++;
    }

    /** 隐形弹性列（零尺寸、极小权重），仅在有需要时吸收行尾空间。 */
    private void addFiller(int gridy, double weightx) {
        java.awt.Component filler = new javax.swing.Box.Filler(
                new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0));
        gbc.gridx = 2;
        gbc.gridy = gridy;
        gbc.gridwidth = 1;
        gbc.weightx = weightx;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(filler, gbc);
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
