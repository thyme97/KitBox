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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.io.File;

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

    private static javax.swing.JWindow activeToast;
    private static javax.swing.Timer activeToastTimer;

    /** 在锚点控件上方弹出深色圆角小提示（如「已复制」），约 1.2 秒后淡出消失。 */
    public static void showToast(java.awt.Component anchor, String text) {
        if (activeToast != null) {
            activeToast.dispose();
            activeToast = null;
        }
        if (activeToastTimer != null) {
            activeToastTimer.stop();
        }
        final javax.swing.JWindow window = new javax.swing.JWindow();
        window.setType(java.awt.Window.Type.POPUP);
        try {
            window.setFocusableWindowState(false);
        } catch (Exception ignored) {
        }
        javax.swing.JPanel chip = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 40, 44, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setLayout(new java.awt.GridBagLayout());
        javax.swing.JLabel label = new javax.swing.JLabel(text, javax.swing.SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(java.awt.Font.PLAIN));
        chip.setBorder(javax.swing.BorderFactory.createEmptyBorder(7, 16, 8, 16));
        chip.add(label);
        window.setBackground(new Color(0, 0, 0, 0));
        window.add(chip);
        window.pack();
        try {
            java.awt.Point loc = anchor.getLocationOnScreen();
            java.awt.Rectangle screen = anchor.getGraphicsConfiguration().getBounds();
            int x = loc.x + (anchor.getWidth() - window.getWidth()) / 2;
            int y = loc.y - window.getHeight() - 8;
            if (y < screen.y + 4) {
                y = loc.y + anchor.getHeight() + 8;
            }
            x = Math.max(screen.x + 4, Math.min(x, screen.x + screen.width - window.getWidth() - 4));
            window.setLocation(x, y);
        } catch (Exception e) {
            window.setLocation(200, 200);
        }
        window.setAlwaysOnTop(true);
        window.setVisible(true);
        activeToast = window;
        activeToastTimer = new javax.swing.Timer(1200, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            final float[] opacity = {1.0f};
            javax.swing.Timer fade = new javax.swing.Timer(16, null);
            fade.addActionListener(e2 -> {
                opacity[0] -= 0.12f;
                if (opacity[0] <= 0f) {
                    fade.stop();
                    window.dispose();
                    if (activeToast == window) {
                        activeToast = null;
                    }
                } else {
                    try {
                        window.setOpacity(opacity[0]);
                    } catch (Exception ex) {
                        fade.stop();
                        window.dispose();
                        if (activeToast == window) {
                            activeToast = null;
                        }
                    }
                }
            });
            fade.start();
        });
        activeToastTimer.setRepeats(false);
        activeToastTimer.start();
    }

    /**
     * 从剪贴板取内容填充输入框：优先取资源管理器复制的文件路径，
     * 其次取文本（去除首尾引号）。返回实际填充的文本，剪贴板为空返回 null。
     */
    public static String pasteFileOrText(javax.swing.JTextField field) {
        String text = clipboardFilePathOrText();
        if (text != null) {
            field.setText(text);
        }
        return text;
    }

    /** 剪贴板内容：文件路径优先，其次文本；均无返回 null。 */
    public static String clipboardFilePathOrText() {
        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents == null) {
                return null;
            }
            if (contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                java.util.List<File> files = (java.util.List<File>)
                        contents.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    return files.get(0).getAbsolutePath();
                }
            }
            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = String.valueOf(contents.getTransferData(DataFlavor.stringFlavor)).trim();
                if (text.length() >= 2
                        && ((text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"')
                        || (text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\''))) {
                    text = text.substring(1, text.length() - 1);
                }
                return text.isEmpty() ? null : text;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 剪贴板中的图片（截图后直接粘贴），没有返回 null。 */
    public static java.awt.Image clipboardImage() {
        try {
            Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return (java.awt.Image) contents.getTransferData(DataFlavor.imageFlavor);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 任意 AWT 图像转 BufferedImage（剪贴板位图统一处理用）。 */
    public static java.awt.image.BufferedImage toBufferedImage(java.awt.Image img) {
        if (img instanceof java.awt.image.BufferedImage) {
            return (java.awt.image.BufferedImage) img;
        }
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                Math.max(1, img.getWidth(null)), Math.max(1, img.getHeight(null)),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = bi.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return bi;
    }

    /**
     * 同步缩放图片到限定尺寸内（保持比例，不超过原尺寸）。
     * getScaledInstance 是异步的，配 ImageIcon 会在未加载完成时画出异常内容，
     * 这里用 BufferedImage 一次性绘制，结果立即可用。
     */
    public static javax.swing.ImageIcon scaledIcon(java.awt.image.BufferedImage src, int maxWidth, int maxHeight) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            return new javax.swing.ImageIcon();
        }
        double scale = Math.min(1.0, Math.min((double) maxWidth / w, (double) maxHeight / h));
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(tw, th,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, tw, th, null);
        g.dispose();
        return new javax.swing.ImageIcon(out);
    }

    /**
     * 规范化路径文本：去首尾引号、剥离 file:/// 协议前缀并做 URL 解码
     * （微信等应用复制的图片常带 file:///E:/... 形式的文本路径）。
     */
    public static String normalizeFilePath(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim();
        if (s.length() >= 2
                && ((s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
                || (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\''))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        if (s.regionMatches(true, 0, "file:///", 0, 8)) {
            s = s.substring(8);
            try {
                s = java.net.URLDecoder.decode(s, "UTF-8");
            } catch (Exception ignored) {
            }
        } else if (s.regionMatches(true, 0, "file:", 0, 5)) {
            try {
                return java.nio.file.Paths.get(new java.net.URI(s)).toString();
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    /** 让组件支持拖入文件（资源管理器/微信拖出的文件，取第一个回调）。 */
    public static void acceptFileDrop(JComponent target, java.util.function.Consumer<File> handler) {
        target.setTransferHandler(new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                try {
                    java.util.List<File> files = (java.util.List<File>)
                            support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        handler.accept(files.get(0));
                        return true;
                    }
                } catch (Exception ignored) {
                }
                return false;
            }
        });
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
