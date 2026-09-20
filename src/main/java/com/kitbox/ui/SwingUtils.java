package com.kitbox.ui;

import com.kitbox.crypto.CryptoException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;

/**
 * Swing 辅助：字体、弹窗、剪贴板、异常包装。
 */
public final class SwingUtils {

    /**
     * 等宽字体（输入输出区用）。
     * 使用逻辑字体 Monospaced：它是复合字体，中文字符可自动回退渲染；
     * 物理字体（如 Consolas）缺少中文字形时会显示为方块（乱码）。
     */
    public static Font monoFont(int size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    private SwingUtils() {
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "出错了", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "确认",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text == null ? "" : text), null);
    }

    /** 处理结果输出：按配置决定是否自动复制。 */
    public static void handleOutput(JComponent context, JTextArea outputArea) {
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            copyToClipboard(outputArea.getText());
        }
    }

    /** 统一异常包装执行。 */
    public static void runWithCatch(Component parent, CryptoRunnable runnable) {
        try {
            runnable.run();
        } catch (CryptoException e) {
            error(parent, e.getMessage());
        } catch (Exception e) {
            error(parent, "操作失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    public interface CryptoRunnable {
        void run() throws Exception;
    }

    /**
     * 可换行的灰色提示文字区。
     * 放在 BorderLayout 中会随容器宽度自动换行；
     * 不要把长提示放进 GridBagLayout 表单内——宽度不足时会把带权重的输入组件挤到最小宽度。
     */
    public static javax.swing.JTextArea hintArea(String text) {
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setFont(area.getFont().deriveFont(Font.PLAIN, area.getFont().getSize2D() - 1f));
        return area;
    }

    /** 水平排列一行控件。 */
    public static JPanel row(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel.setOpaque(false);
        for (JComponent c : components) {
            panel.add(c);
        }
        return panel;
    }

    /** 固定高度按钮尺寸风格。 */
    public static void uniformSize(JComponent... components) {
        Dimension max = new Dimension(0, 0);
        for (JComponent c : components) {
            Dimension d = c.getPreferredSize();
            if (d.width > max.width) {
                max.width = d.width;
            }
            if (d.height > max.height) {
                max.height = d.height;
            }
        }
        for (JComponent c : components) {
            c.setPreferredSize(max);
        }
    }
}
