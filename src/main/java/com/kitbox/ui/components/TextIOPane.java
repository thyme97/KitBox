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
        inputPanel.setBorder(BorderFactory.createTitledBorder(inputTitle));
        inputPanel.add(buildToolbar(inputArea, true), BorderLayout.NORTH);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(outputTitle));
        outputPanel.add(buildToolbar(outputArea, false), BorderLayout.NORTH);
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        status.setFont(status.getFont().deriveFont(Font.PLAIN, status.getFont().getSize2D() - 1f));
        status.setForeground(new Color(120, 120, 120));

        add(inputPanel);
        add(Box.createVerticalStrut(6));
        add(outputPanel);
        add(status);
    }

    private JPanel buildToolbar(JTextArea area, boolean isInput) {
        JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 2));
        if (isInput) {
            JButton paste = new JButton("粘贴");
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
        JButton copy = new JButton("复制");
        copy.addActionListener(e -> {
            if (!area.getText().isEmpty()) {
                SwingUtils.copyToClipboard(area.getText());
                note("已复制到剪贴板");
            }
        });
        bar.add(copy);
        JButton clear = new JButton("清空");
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
