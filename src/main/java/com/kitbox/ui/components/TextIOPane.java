package com.kitbox.ui.components;

import com.kitbox.ui.SwingUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * 输入 / 输出文本区组件：自带复制、清空、粘贴与「结果回填输入」按钮。
 */
public class TextIOPane extends JPanel {

    private final JTextArea inputArea = new JTextArea(7, 40);
    private final JTextArea outputArea = new JTextArea(7, 40);
    private final JLabel status = new JLabel(" ");

    public TextIOPane(String inputTitle, String outputTitle) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        Font mono = SwingUtils.monoFont(inputArea.getFont().getSize());
        inputArea.setFont(mono);
        outputArea.setFont(mono);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(SwingUtils.cardLineBorder());
        inputPanel.add(captionRow(inputTitle, buildToolbar(inputArea, true)), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(SwingUtils.cardLineBorder());
        outputPanel.add(captionRow(outputTitle, buildToolbar(outputArea, false)), BorderLayout.NORTH);
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        status.setFont(status.getFont().deriveFont(Font.PLAIN, status.getFont().getSize2D() - 1f));
        Color hintColor = UIManager.getColor("Label.disabledForeground");
        status.setForeground(hintColor != null ? hintColor : new Color(120, 120, 120));

        add(inputPanel);
        add(Box.createVerticalStrut(6));
        add(outputPanel);
        add(status);
    }

    /** 卡片内标题行：标题居左、工具条按钮居右，省去独立的一行按钮。 */
    private JPanel captionRow(String title, JPanel toolbar) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 10, 0, 6));
        row.add(SwingUtils.groupLabel(title), BorderLayout.WEST);
        row.add(toolbar, BorderLayout.EAST);
        return row;
    }

    private JPanel buildToolbar(JTextArea area, boolean isInput) {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 2, 0));
        if (isInput) {
            JButton paste = SwingUtils.iconButton("clipboard-paste", "粘贴", "粘贴");
            paste.addActionListener(e -> {
                try {
                    String text = (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (text != null) {
                        area.setText(text);
                    }
                } catch (Exception ignored) {
                }
            });
            bar.add(paste);
        }
        JButton copy = SwingUtils.iconButton("copy", "复制", "复制");
        copy.addActionListener(e -> {
            if (!area.getText().isEmpty()) {
                SwingUtils.copyToClipboard(area.getText());
                note("已复制到剪贴板");
            }
        });
        bar.add(copy);
        JButton clear = SwingUtils.iconButton("trash-2", "清空", "清空");
        clear.addActionListener(e -> {
            area.setText("");
            note(" ");
        });
        bar.add(clear);
        if (!isInput) {
            JButton swap = new JButton("↑ 用作输入");
            swap.addActionListener(e -> {
                inputArea.setText(outputArea.getText());
                note("已将结果回填到输入区");
            });
            bar.add(swap);
        }
        return bar;
    }

    public String getInput() {
        return inputArea.getText();
    }

    public void setInput(String text) {
        inputArea.setText(text);
    }

    public String getOutput() {
        return outputArea.getText();
    }

    public void setOutput(String text) {
        outputArea.setText(text);
        if (com.kitbox.AppContext.config.isAutoCopyResult() && text != null && !text.isEmpty()) {
            SwingUtils.copyToClipboard(text);
            note("已自动复制结果");
        }
    }

    public void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        note(" ");
    }

    public JTextArea outputArea() {
        return outputArea;
    }

    public void note(String text) {
        status.setText(text == null || text.isEmpty() ? " " : text);
    }
}
