package com.kitbox.ui.components;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;

/**
 * 图片放大查看对话框：滚轮缩放（0.05x~16x），工具条提供缩小/放大/1:1/适应窗口。
 * 画布按缩放比例只直绘可视区域（双线性），缩小时图片在视口内居中；
 * 支持 {@link #setImage} 原地换图，供调用方复用同一个窗口。
 */
public class ImageZoomDialog extends JDialog {

    private static final double STEP = 1.25;
    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 16;

    private BufferedImage image;
    private final JScrollPane scroll;
    private final ZoomCanvas canvas;
    private final JLabel zoomLabel = new JLabel(" ");
    private double scale = 1.0;

    public ImageZoomDialog(Window owner, String title, BufferedImage image) {
        super(owner, title, ModalityType.MODELESS);
        this.image = image;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        canvas = new ZoomCanvas();
        scroll = new JScrollPane(canvas);
        scroll.setBorder(null);
        scroll.addMouseWheelListener(new WheelZoom());
        add(scroll, BorderLayout.CENTER);

        JButton minus = new JButton("－");
        JButton plus = new JButton("＋");
        JButton one = new JButton("1:1");
        JButton fit = new JButton("适应窗口");
        minus.addActionListener(e -> setScale(scale / STEP));
        plus.addActionListener(e -> setScale(scale * STEP));
        one.addActionListener(e -> setScale(1.0));
        fit.addActionListener(e -> fitToWindow());
        zoomLabel.setFont(zoomLabel.getFont().deriveFont(java.awt.Font.PLAIN));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        bar.add(minus);
        bar.add(zoomLabel);
        bar.add(plus);
        bar.add(one);
        bar.add(fit);
        add(bar, BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "kitboxCloseZoom");
        getRootPane().getActionMap().put("kitboxCloseZoom", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                dispose();
            }
        });
        canvas.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setScale(1.0);
                }
            }
        });

        setSize(860, 660);
        setLocationRelativeTo(owner);
        fitToWindow();
    }

    /** 原地更换图片（窗口已打开时更新内容并重新适应）。 */
    public void setImage(BufferedImage newImage) {
        this.image = newImage;
        fitToWindow();
    }

    private void fitToWindow() {
        Dimension avail = scroll.getViewport().getSize();
        if (avail.width < 40 || avail.height < 40) {
            avail = new Dimension(getWidth() - 40, getHeight() - 90);
        }
        double fit = Math.min((double) avail.width / image.getWidth(),
                (double) avail.height / image.getHeight());
        setScale(Math.max(MIN_SCALE, Math.min(MAX_SCALE, fit)));
    }

    private void setScale(double newScale) {
        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
        canvas.setPreferredSize(new Dimension(scaledW(), scaledH()));
        canvas.revalidate();
        canvas.repaint();
        zoomLabel.setText((int) Math.round(scale * 100) + "%");
    }

    private int scaledW() {
        return Math.max(1, (int) Math.round(image.getWidth() * scale));
    }

    private int scaledH() {
        return Math.max(1, (int) Math.round(image.getHeight() * scale));
    }

    /**
     * 画布：图片缩小时在视口内居中（Scrollable 按需贴合视口），
     * 绘制时只画可视区域对应的原图部分（双线性直绘，无中间缩放图）。
     */
    private class ZoomCanvas extends JComponent implements Scrollable {

        ZoomCanvas() {
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            int cw = getWidth();
            int ch = getHeight();
            g.setColor(getBackground());
            g.fillRect(0, 0, cw, ch);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int imgW = scaledW();
            int imgH = scaledH();
            int ox = Math.max(0, (cw - imgW) / 2);
            int oy = Math.max(0, (ch - imgH) / 2);
            Rectangle clip = g.getClipBounds();
            int dx1 = Math.max(clip.x, ox);
            int dy1 = Math.max(clip.y, oy);
            int dx2 = Math.min(clip.x + clip.width, ox + imgW);
            int dy2 = Math.min(clip.y + clip.height, oy + imgH);
            if (dx2 > dx1 && dy2 > dy1) {
                int sx1 = clamp((int) Math.floor((dx1 - ox) / scale), 0, image.getWidth());
                int sy1 = clamp((int) Math.floor((dy1 - oy) / scale), 0, image.getHeight());
                int sx2 = clamp((int) Math.ceil((dx2 - ox) / scale), 0, image.getWidth());
                int sy2 = clamp((int) Math.ceil((dy2 - oy) / scale), 0, image.getHeight());
                g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
            }
            g.dispose();
        }

        private int clamp(int v, int min, int max) {
            return Math.max(min, Math.min(max, v));
        }

        // 图片小于视口时贴合视口（画布充满、绘制居中），大于视口时出滚动条
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getPreferredSize().width <= scroll.getViewport().getWidth();
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getPreferredSize().height <= scroll.getViewport().getHeight();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 32;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
        }
    }

    /** 滚轮缩放。 */
    private class WheelZoom implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            setScale(scale * (e.getWheelRotation() < 0 ? STEP : 1 / STEP));
        }
    }
}
