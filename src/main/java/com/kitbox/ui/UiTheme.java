package com.kitbox.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.kitbox.config.AppConfig;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Window;
import java.io.InputStream;

/**
 * 主题应用：FlatLaf 亮/暗（可跟随系统深浅色）+ 中文字体。
 */
public final class UiTheme {

    private UiTheme() {
    }

    /** 当前是否深色主题（apply 后有效），供主题切换按钮判断方向。 */
    private static boolean currentDark;

    public static boolean isDarkTheme() {
        return currentDark;
    }

    public static void apply(AppConfig config) {
        // 品牌主色与应用图标一致：选中高亮、焦点描边、默认按钮统一为品牌蓝
        java.util.Map<String, String> brand = new java.util.HashMap<>();
        brand.put("@accentColor", "#446CF5");
        brand.put("@selectionArc", "8");
        com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(brand);
        currentDark = isDark(config);
        try {
            if (currentDark) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ignored) {
        }
        // 中文环境优先使用雅黑系字体
        String fontName = pickFont();
        UIManager.put("defaultFont", new java.awt.Font(fontName, java.awt.Font.PLAIN, config.getFontSize()));
        // 现代度量：更大圆角、更松内边距、更高行高，全局生效
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.margin", new java.awt.Insets(5, 14, 5, 14));
        UIManager.put("TextField.margin", new java.awt.Insets(5, 8, 5, 8));
        UIManager.put("ComboBox.padding", new java.awt.Insets(4, 10, 4, 10));
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("TabbedPane.tabHeight", 36);
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(6, 14, 6, 14));
        UIManager.put("Tree.rowHeight", 28);
        // 树选中项：浅色填充 + 深色文字（IDEA 风格），替代整行实心反白
        if (currentDark) {
            UIManager.put("Tree.selectionBackground", new java.awt.Color(0x33517E));
            UIManager.put("Tree.selectionForeground", new java.awt.Color(0xE8EAED));
            UIManager.put("Tree.selectionInactiveBackground", new java.awt.Color(0x2C4468));
            UIManager.put("Tree.selectionInactiveForeground", new java.awt.Color(0xC7CBD1));
        } else {
            UIManager.put("Tree.selectionBackground", new java.awt.Color(0xE8F0FE));
            UIManager.put("Tree.selectionForeground", new java.awt.Color(0x1F2333));
            UIManager.put("Tree.selectionInactiveBackground", new java.awt.Color(0xF0F2F5));
            UIManager.put("Tree.selectionInactiveForeground", new java.awt.Color(0x3C4043));
        }
        // 细滚动条
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 10);
        // Tooltip 更快出现（默认 750ms 悬停延迟容易被认为「不显示」）
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(300);
        javax.swing.ToolTipManager.sharedInstance().setReshowDelay(150);
    }

    /**
     * 重新应用主题并刷新所有已打开窗口（设置页切换主题/字号时调用）。
     * 边框等在绘制时读取 UIManager 的部件（卡片边框、导航图标）会随重绘自动适配。
     */
    public static void applyAndRefresh(AppConfig config) {
        apply(config);
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
            if (window instanceof MainWindow) {
                ((MainWindow) window).refreshTheme();
            }
        }
    }

    /** 解析主题：dark / light / system。kitbox.theme 系统属性可临时覆盖（冒烟测试用）。 */
    private static boolean isDark(AppConfig config) {
        String theme = config.getTheme();
        String override = System.getProperty("kitbox.theme");
        if (override != null && !override.isEmpty()) {
            theme = override;
        }
        if ("dark".equals(theme)) {
            return true;
        }
        return "system".equals(theme) && isSystemDark();
    }

    /** 读系统深浅色：Windows 注册表 AppsUseLightTheme；macOS 读 defaults。失败按亮色。 */
    static boolean isSystemDark() {
        try {
            if (com.formdev.flatlaf.util.SystemInfo.isWindows) {
                Process process = new ProcessBuilder("reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme").start();
                String text = readAll(process.getInputStream());
                process.waitFor();
                for (String line : text.split("\r?\n")) {
                    if (line.contains("AppsUseLightTheme")) {
                        // 0x0 = 深色模式，0x1 = 亮色模式
                        return line.trim().endsWith("0x0");
                    }
                }
                return false;
            }
            if (com.formdev.flatlaf.util.SystemInfo.isMacOS) {
                Process process = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").start();
                String text = readAll(process.getInputStream());
                process.waitFor();
                return text.contains("Dark");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** 按 ISO-8859-1 无损读取进程输出（只匹配 ASCII 关键字，避免控制台编码差异）。 */
    private static String readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            out.write(buffer, 0, n);
        }
        in.close();
        return new String(out.toByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    private static String pickFont() {
        String[] candidates = {"Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", null};
        for (String name : candidates) {
            if (name != null) {
                java.awt.Font f = new java.awt.Font(name, java.awt.Font.PLAIN, 12);
                if (!f.getFamily().equalsIgnoreCase(java.awt.Font.DIALOG) && f.canDisplay('密')) {
                    return name;
                }
            }
        }
        return java.awt.Font.SANS_SERIF;
    }
}
