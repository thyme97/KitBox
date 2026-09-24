package com.kitbox.ui;

import javax.swing.Icon;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * 导航树工具图标：16px 线条风格，Graphics2D 手绘矢量，
 * 颜色在绘制时读取当前主题前景色，深浅色切换自动适配。无需图标资源文件。
 */
public final class ToolIcons {

    private static final Map<String, Icon> CACHE = new HashMap<>();

    private ToolIcons() {
    }

    /** 按工具名取图标（未知名称返回通用点形图标）。 */
    public static Icon of(String toolName) {
        return CACHE.computeIfAbsent(toolName, ToolIcons::create);
    }

    private static Icon create(String name) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                draw(name, g, x, y);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private static void draw(String name, Graphics g0, int ox, int oy) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color fg = UIManager.getColor("Label.foreground");
        if (fg == null) {
            fg = Color.DARK_GRAY;
        }
        g.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 205));
        g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.translate(ox, oy);
        try {
            switch (name) {
                case "主页":
                    drawHome(g);
                    break;
                case "对称加解密":
                    drawLock(g);
                    break;
                case "非对称加解密":
                    drawKeyPair(g);
                    break;
                case "加签 / 验签":
                    drawSignature(g);
                    break;
                case "摘要与 HMAC":
                    drawHash(g);
                    break;
                case "JSON 字段加解密":
                    drawBraces(g, true);
                    break;
                case "JSON 工具":
                    drawBraces(g, false);
                    break;
                case "报文格式加解密":
                    drawMail(g);
                    break;
                case "密钥库":
                    drawKey(g);
                    break;
                case "编码转换":
                    drawSwap(g);
                    break;
                case "文件 Base64":
                    drawDocSwap(g);
                    break;
                case "转换工具":
                    drawCycle(g);
                    break;
                case "二维码工具":
                    drawQr(g);
                    break;
                case "文件批量校验":
                    drawDocCheck(g);
                    break;
                case "密码生成器":
                    drawPassword(g);
                    break;
                case "假数据生成":
                    drawIdCard(g);
                    break;
                case "设置":
                    drawGear(g);
                    break;
                default:
                    drawBraces(g, false);
                    break;
            }
        } finally {
            g.dispose();
        }
    }

    private static void drawHome(Graphics2D g) {
        Path2D roof = new Path2D.Double();
        roof.moveTo(2.2, 8.2);
        roof.lineTo(8, 3);
        roof.lineTo(13.8, 8.2);
        g.draw(roof);
        Path2D wall = new Path2D.Double();
        wall.moveTo(3.8, 7.2);
        wall.lineTo(3.8, 13.2);
        wall.lineTo(12.2, 13.2);
        wall.lineTo(12.2, 7.2);
        g.draw(wall);
        g.draw(new RoundRectangle2D.Double(6.7, 9.4, 2.6, 3.8, 1.2, 1.2));
    }

    private static void drawLock(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(3.2, 6.5, 9.6, 7.2, 2.2, 2.2));
        g.drawArc(5, 2, 6, 9, 0, 180);
        g.fill(new Ellipse2D.Double(7.1, 8.6, 1.8, 1.8));
        g.draw(new Line2D.Double(8, 10.3, 8, 11.4));
    }

    private static void drawKeyPair(Graphics2D g) {
        g.draw(new Ellipse2D.Double(2.4, 4.6, 6.8, 6.8));
        g.draw(new Ellipse2D.Double(6.8, 4.6, 6.8, 6.8));
    }

    private static void drawSignature(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(1.8, 12.2);
        p.curveTo(4.2, 3.6, 6.8, 3.6, 7.8, 7.6);
        p.curveTo(8.6, 10.8, 10.2, 10.6, 11.4, 8.4);
        p.curveTo(12.2, 7.0, 13.2, 5.4, 14.2, 4.6);
        g.draw(p);
    }

    private static void drawHash(Graphics2D g) {
        g.draw(new Line2D.Double(6.2, 3, 5, 13));
        g.draw(new Line2D.Double(11, 3, 9.8, 13));
        g.draw(new Line2D.Double(3, 6.4, 13, 6.4));
        g.draw(new Line2D.Double(3, 9.6, 13, 9.6));
    }

    private static void drawBraces(Graphics2D g, boolean withDot) {
        Path2D left = new Path2D.Double();
        left.moveTo(10.6, 3);
        left.curveTo(8.4, 3, 9.2, 5.6, 8.4, 7.2);
        left.curveTo(8.1, 7.9, 7.4, 8, 6.2, 8);
        left.curveTo(7.4, 8, 8.1, 8.1, 8.4, 8.8);
        left.curveTo(9.2, 10.4, 8.4, 13, 10.6, 13);
        g.draw(left);
        Path2D right = new Path2D.Double();
        right.moveTo(5.4, 3);
        right.curveTo(7.6, 3, 6.8, 5.6, 7.6, 7.2);
        right.curveTo(7.9, 7.9, 8.6, 8, 9.8, 8);
        right.curveTo(8.6, 8, 7.9, 8.1, 7.6, 8.8);
        right.curveTo(6.8, 10.4, 7.6, 13, 5.4, 13);
        g.draw(right);
        if (withDot) {
            g.fill(new Ellipse2D.Double(6.9, 6.9, 2.2, 2.2));
        }
    }

    private static void drawMail(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(2.4, 3.6, 11.2, 8.8, 2, 2));
        Path2D flap = new Path2D.Double();
        flap.moveTo(3.2, 4.6);
        flap.lineTo(8, 8.3);
        flap.lineTo(12.8, 4.6);
        g.draw(flap);
    }

    private static void drawKey(Graphics2D g) {
        g.draw(new Ellipse2D.Double(2.6, 5.4, 5.2, 5.2));
        g.draw(new Line2D.Double(7.8, 8, 13.4, 8));
        g.draw(new Line2D.Double(11.2, 8, 11.2, 10.6));
        g.draw(new Line2D.Double(13.2, 8, 13.2, 9.9));
    }

    private static void drawSwap(Graphics2D g) {
        g.draw(new Line2D.Double(3, 5.6, 13, 5.6));
        Path2D head1 = new Path2D.Double();
        head1.moveTo(10.7, 3.6);
        head1.lineTo(13, 5.6);
        head1.lineTo(10.7, 7.6);
        g.draw(head1);
        g.draw(new Line2D.Double(13, 10.4, 3, 10.4));
        Path2D head2 = new Path2D.Double();
        head2.moveTo(5.3, 8.4);
        head2.lineTo(3, 10.4);
        head2.lineTo(5.3, 12.4);
        g.draw(head2);
    }

    private static void drawDocSwap(Graphics2D g) {
        Path2D doc = new Path2D.Double();
        doc.moveTo(3.4, 2.6);
        doc.lineTo(8.8, 2.6);
        doc.lineTo(11.8, 5.6);
        doc.lineTo(11.8, 8.2);
        g.draw(doc);
        Path2D fold = new Path2D.Double();
        fold.moveTo(8.8, 2.6);
        fold.lineTo(8.8, 5.6);
        fold.lineTo(11.8, 5.6);
        g.draw(fold);
        g.draw(new Line2D.Double(4.4, 7.6, 10.4, 7.6));
        Path2D head1 = new Path2D.Double();
        head1.moveTo(8.6, 6.2);
        head1.lineTo(10.4, 7.6);
        head1.lineTo(8.6, 9.0);
        g.draw(head1);
        g.draw(new Line2D.Double(11.6, 10.8, 5.6, 10.8));
        Path2D head2 = new Path2D.Double();
        head2.moveTo(7.4, 9.4);
        head2.lineTo(5.6, 10.8);
        head2.lineTo(7.4, 12.2);
        g.draw(head2);
    }

    private static void drawCycle(Graphics2D g) {
        g.drawArc(3, 3, 10, 10, 40, 140);
        g.drawArc(3, 3, 10, 10, 220, 140);
        Path2D arrow1 = new Path2D.Double();
        arrow1.moveTo(1.5, 6.9);
        arrow1.lineTo(3, 8.9);
        arrow1.lineTo(4.5, 6.9);
        g.draw(arrow1);
        Path2D arrow2 = new Path2D.Double();
        arrow2.moveTo(11.5, 9.1);
        arrow2.lineTo(13, 7.1);
        arrow2.lineTo(14.5, 9.1);
        g.draw(arrow2);
    }

    private static void drawQr(Graphics2D g) {
        g.fill(new RoundRectangle2D.Double(2.4, 2.4, 4.6, 4.6, 1.2, 1.2));
        g.fill(new RoundRectangle2D.Double(9.0, 2.4, 4.6, 4.6, 1.2, 1.2));
        g.fill(new RoundRectangle2D.Double(2.4, 9.0, 4.6, 4.6, 1.2, 1.2));
        g.fill(new Ellipse2D.Double(9.6, 9.6, 1.9, 1.9));
        g.fill(new Ellipse2D.Double(12.3, 12.3, 1.4, 1.4));
        g.fill(new Ellipse2D.Double(12.4, 9.8, 1.3, 1.3));
        g.fill(new Ellipse2D.Double(9.8, 12.4, 1.3, 1.3));
    }

    private static void drawDocCheck(Graphics2D g) {
        Path2D doc = new Path2D.Double();
        doc.moveTo(4, 2.6);
        doc.lineTo(9.4, 2.6);
        doc.lineTo(12.4, 5.6);
        doc.lineTo(12.4, 13.4);
        doc.lineTo(4, 13.4);
        doc.closePath();
        g.draw(doc);
        Path2D fold = new Path2D.Double();
        fold.moveTo(9.4, 2.6);
        fold.lineTo(9.4, 5.6);
        fold.lineTo(12.4, 5.6);
        g.draw(fold);
        Path2D check = new Path2D.Double();
        check.moveTo(5.8, 9.4);
        check.lineTo(7.4, 11);
        check.lineTo(10.4, 7.6);
        g.draw(check);
    }

    private static void drawPassword(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(2.4, 3.9, 11.2, 8.2, 2.4, 2.4));
        g.fill(new Ellipse2D.Double(4.7, 7.1, 1.8, 1.8));
        g.fill(new Ellipse2D.Double(7.1, 7.1, 1.8, 1.8));
        g.fill(new Ellipse2D.Double(9.5, 7.1, 1.8, 1.8));
    }

    private static void drawIdCard(Graphics2D g) {
        g.draw(new RoundRectangle2D.Double(2.4, 3.2, 11.2, 9.6, 2, 2));
        g.draw(new Ellipse2D.Double(4.3, 5.3, 2.8, 2.8));
        Path2D body = new Path2D.Double();
        body.moveTo(3.3, 11.3);
        body.curveTo(3.8, 8.4, 7.6, 8.4, 8.1, 11.3);
        g.draw(body);
        g.draw(new Line2D.Double(9.6, 6.0, 12.2, 6.0));
        g.draw(new Line2D.Double(9.6, 8.3, 12.2, 8.3));
        g.draw(new Line2D.Double(9.6, 10.6, 11.5, 10.6));
    }

    private static void drawGear(Graphics2D g) {
        g.draw(new Ellipse2D.Double(5.1, 5.1, 5.8, 5.8));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            g.draw(new Line2D.Double(
                    8 + Math.cos(a) * 4.4, 8 - Math.sin(a) * 4.4,
                    8 + Math.cos(a) * 6.3, 8 - Math.sin(a) * 6.3));
        }
    }
}
