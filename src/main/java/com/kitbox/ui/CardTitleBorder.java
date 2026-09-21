package com.kitbox.ui;

import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * 现代分组边框：小号加粗标题在最上方，下方用圆角描边把内容圈成卡片，
 * 取代老式的 TitledBorder 切线框。颜色与字体在绘制时读取 UIManager，
 * 深浅色主题切换后无需重建边框即可自动适配。
 */
public class CardTitleBorder implements Border {

    private final String title;

    public CardTitleBorder(String title) {
        this.title = title == null ? "" : title;
    }

    private Font captionFont(Component c) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) {
            base = c.getFont();
        }
        if (base == null) {
            base = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
        return base.deriveFont(Font.BOLD, Math.max(11f, base.getSize2D() - 1f));
    }

    private Color captionColor() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.DARK_GRAY;
    }

    private Color lineColor() {
        Color color = UIManager.getColor("Component.borderColor");
        return color != null ? color : Color.GRAY;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        if (title.isEmpty()) {
            // 纯线框卡片：无标题区
            return new Insets(8, 10, 10, 10);
        }
        FontMetrics fm = c.getFontMetrics(captionFont(c));
        return new Insets(fm.getHeight() + 12, 10, 10, 10);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int lineTop;
        if (title.isEmpty()) {
            lineTop = y + 4;
        } else {
            Font font = captionFont(c);
            FontMetrics fm = c.getFontMetrics(font);
            g2.setFont(font);
            g2.setColor(captionColor());
            g2.drawString(title, x + 2, y + fm.getAscent() + 1);
            lineTop = y + fm.getHeight() + 4;
        }
        g2.setColor(lineColor());
        g2.drawRoundRect(x, lineTop, width - 1, height - lineTop - 1, 10, 10);
        g2.dispose();
    }
}
