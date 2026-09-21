package com.kitbox.ui;

import com.kitbox.crypto.CryptoException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
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

    /** 现代卡片分组边框（替代 TitledBorder）。 */
    public static javax.swing.border.Border cardBorder(String title) {
        return new CardTitleBorder(title);
    }

    /** 无标题的圆角线框卡片边框（标题行由内容自绘，可放操作按钮）。 */
    public static javax.swing.border.Border cardLineBorder() {
        return new CardTitleBorder("");
    }

    /** 品牌主色（与 UiTheme 的 @accentColor 保持一致）。 */
    private static final String ACCENT_HEX = "#446CF5";

    /** 主操作按钮：品牌蓝实心。 */
    public static void stylePrimary(javax.swing.AbstractButton button) {
        button.putClientProperty("FlatLaf.style",
                "background: " + ACCENT_HEX + "; foreground: #FFFFFF; borderColor: " + ACCENT_HEX);
    }

    /** 次级按钮：品牌蓝描边。 */
    public static void styleSecondary(javax.swing.AbstractButton button) {
        button.putClientProperty("FlatLaf.style",
                "foreground: " + ACCENT_HEX + "; borderColor: " + ACCENT_HEX);
    }

    /** 危险操作按钮：保留默认按钮外观（有边框），文字改红色以示警示。 */
    public static void styleDanger(JButton button) {
        Color red = UIManager.getColor("Actions.Red");
        button.putClientProperty("FlatLaf.style",
                "foreground: " + (red != null ? String.format("#%06X", red.getRGB() & 0xFFFFFF) : "#C62828"));
    }

    /** 工具条小按钮：紧凑无边界。 */
    public static void styleToolbar(JComponent button) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
    }

    /** 分节小标题：加粗、缩小、灰色，用于在卡片/表单内划分控件组。 */
    public static javax.swing.JLabel groupLabel(String text) {
        javax.swing.JLabel label = new javax.swing.JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, Math.max(11f, label.getFont().getSize2D() - 1f)));
        Color color = UIManager.getColor("Label.disabledForeground");
        label.setForeground(color != null ? color : new Color(120, 120, 120));
        return label;
    }

    /**
     * Lucide 线条 SVG 图标（resources/icons/ 下，ISC 许可）。
     * 统一重着色为当前主题前景色，深浅色自适应；加载失败返回 null（调用方回退纯文本按钮）。
     */
    public static javax.swing.Icon svgIcon(String name, int size) {
        try {
            // 注意：FlatSVGIcon 走 ClassLoader 查资源，路径不能以 / 开头
            com.formdev.flatlaf.extras.FlatSVGIcon icon =
                    new com.formdev.flatlaf.extras.FlatSVGIcon("icons/" + name + ".svg", size, size);
            // Lucide 的线条颜色统一重着色为当前主题前景色，深浅色自适应
            icon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(
                    (java.util.function.Function<Color, Color>) color -> {
                        Color fg = UIManager.getColor("Label.foreground");
                        return fg != null ? fg : color;
                    }));
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    /** 图标按钮：仅图标 + 悬停提示；图标缺失时回退为文字按钮。 */
    public static JButton iconButton(String iconName, String fallbackText, String tooltip) {
        JButton button = new JButton();
        javax.swing.Icon icon = svgIcon(iconName, 15);
        if (icon != null) {
            button.setIcon(icon);
        } else {
            button.setText(fallbackText);
        }
        button.setToolTipText(tooltip);
        styleToolbar(button);
        return button;
    }

    /** 动作栏：主操作靠左、低频操作靠右，符合「主次分区」习惯。 */
    public static JPanel actionBar(JComponent[] leftItems, JComponent[] rightItems) {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        left.setOpaque(false);
        right.setOpaque(false);
        for (JComponent c : leftItems) {
            left.add(c);
        }
        for (JComponent c : rightItems) {
            right.add(c);
        }
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 12, 4, 12));
        return bar;
    }

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
