package com.kitbox.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.kitbox.config.AppConfig;

import javax.swing.UIManager;

/**
 * 主题应用：FlatLaf 亮/暗 + 中文字体。
 */
public final class UiTheme {

    private UiTheme() {
    }

    public static void apply(AppConfig config) {
        try {
            if ("dark".equals(config.getTheme())) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ignored) {
        }
        // 中文环境优先使用雅黑系字体
        String fontName = pickFont();
        UIManager.put("defaultFont", new java.awt.Font(fontName, java.awt.Font.PLAIN, config.getFontSize()));
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
