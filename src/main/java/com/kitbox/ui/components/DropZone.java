package com.kitbox.ui.components;

import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.UiTheme;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Element-UI 风格的文件拖放区：虚线圆角边框，点击选择、拖入文件、Ctrl+V 粘贴。
 * 拖拽悬停时边框与背景高亮为主题色。
 */
public class DropZone extends JPanel implements DropTargetListener {

    private final JLabel content = new JLabel(" ", javax.swing.SwingConstants.CENTER);
    private final JButton pasteButton = new JButton();
    private final JButton clearButton = new JButton();
    private final String icon;
    private final JFileChooser chooser = new JFileChooser();

    private Consumer<File> fileConsumer;
    private Runnable pasteHandler;
    private Runnable clearHandler;
    private String fileName;
    private boolean hover;

    public DropZone(String icon) {
        this.icon = icon;
        setLayout(new BorderLayout());
        setOpaque(false);

        content.setFont(content.getFont().deriveFont(Font.PLAIN, content.getFont().getSize2D() + 0f));
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        center.add(content, gc);
        add(center, BorderLayout.CENTER);

        Icon pasteIcon = SwingUtils.svgIcon("clipboard-paste", 14);
        if (pasteIcon != null) {
            pasteButton.setIcon(pasteIcon);
        } else {
            pasteButton.setText("粘贴");
        }
        pasteButton.setToolTipText("粘贴（Ctrl+V）");
        pasteButton.setBorder(null);
        pasteButton.setContentAreaFilled(false);
        pasteButton.setFocusable(false);
        pasteButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        pasteButton.addActionListener(e -> {
            if (pasteHandler != null) {
                pasteHandler.run();
            }
        });
        pasteButton.setVisible(false);

        Icon trash = SwingUtils.svgIcon("trash-2", 14);
        if (trash != null) {
            clearButton.setIcon(trash);
        } else {
            clearButton.setText("移除");
        }
        clearButton.setToolTipText("移除已选文件");
        clearButton.setBorder(null);
        clearButton.setContentAreaFilled(false);
        clearButton.setFocusable(false);
        clearButton.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> {
            fileName = null;
            clearButton.setVisible(false);
            refreshContent();
            if (clearHandler != null) {
                clearHandler.run();
            }
        });
        clearButton.setVisible(false);
        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        east.setOpaque(false);
        east.add(pasteButton);
        east.add(clearButton);
        add(east, BorderLayout.EAST);

        // 点击任意空白处打开文件选择器
        MouseAdapter click = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (fileConsumer == null) {
                    return;
                }
                if (chooser.showOpenDialog(DropZone.this) == JFileChooser.APPROVE_OPTION) {
                    fileConsumer.accept(chooser.getSelectedFile());
                }
            }
        };
        addMouseListener(click);
        center.addMouseListener(click);
        content.addMouseListener(click);

        // 拖拽（自管 DropTarget，以便控制悬停高亮）
        new DropTarget(this, DnDConstants.ACTION_COPY, this, true);

        setPreferredSize(new java.awt.Dimension(120, 92));
        refreshContent();
    }

    /** 设置文件回调（点击选择 / 拖入都会回调）。 */
    public void setFileConsumer(Consumer<File> consumer) {
        this.fileConsumer = consumer;
    }

    /** 设置 Ctrl+V 粘贴回调（整页生效），并显示粘贴小按钮。 */
    public void setPasteHandler(Runnable handler) {
        this.pasteHandler = handler;
        pasteButton.setVisible(handler != null);
        bindPaste();
    }

    /** 设置移除回调（点 × 时在恢复空状态后调用）。 */
    public void setClearHandler(Runnable handler) {
        this.clearHandler = handler;
    }

    /** 显示已选文件名（null 恢复空状态）。 */
    public void setFileName(String name) {
        this.fileName = name;
        clearButton.setVisible(name != null);
        refreshContent();
    }

    private void bindPaste() {
        if (pasteHandler == null) {
            return;
        }
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK), "kitboxZonePaste");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V,
                        java.awt.event.InputEvent.CTRL_MASK), "kitboxZonePaste");
        getActionMap().put("kitboxZonePaste", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pasteHandler.run();
            }
        });
    }

    private void refreshContent() {
        String blue = UiTheme.isDarkTheme() ? "#8AB0FF" : "#446CF5";
        Icon icon = SwingUtils.svgIcon(this.icon, 28);
        String body;
        if (hover) {
            body = "<b>释放鼠标即可</b>";
        } else if (fileName != null) {
            body = "已选择：<b>" + escape(fileName) + "</b><br>"
                    + "<font color='" + blue + "'>点击重新选择</font> · 也可拖入更换";
        } else {
            body = "将文件拖到此处，或 <font color='" + blue + "'>点击选择</font>";
        }
        String html = "<html><div style='text-align:center'>" + body + "</div></html>";
        if (icon != null) {
            content.setIcon(icon);
            int gap = 10;
            content.setHorizontalTextPosition(javax.swing.SwingConstants.TRAILING);
            content.setIconTextGap(gap);
        } else {
            content.setIcon(null);
        }
        content.setText(html);
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        int inset = 5;
        int w = getWidth() - inset * 2;
        int h = getHeight() - inset * 2;
        Color accent = new Color(0x446CF5);
        Color borderColor = UIManagerColor("Component.borderColor", new Color(0xC0C6CF));
        if (hover) {
            g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
            g.fillRoundRect(inset, inset, w, h, 12, 12);
            g.setColor(accent);
            g.setStroke(new BasicStroke(1.6f));
        } else {
            g.setColor(borderColor);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[]{4f, 4f}, 0f));
        }
        g.drawRoundRect(inset, inset, w, h, 12, 12);
        g.dispose();
    }

    private static Color UIManagerColor(String key, Color fallback) {
        Color c = javax.swing.UIManager.getColor(key);
        return c != null ? c : fallback;
    }

    // ---------------- DropTargetListener ----------------

    private boolean accepts(java.awt.dnd.DropTargetDragEvent e) {
        return e.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public void dragEnter(DropTargetDragEvent e) {
        hover = accepts(e);
        refreshContent();
        repaint();
    }

    @Override
    public void dragOver(DropTargetDragEvent e) {
        hover = accepts(e);
        refreshContent();
        repaint();
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        // 无需处理
    }

    @Override
    public void dragExit(DropTargetEvent e) {
        hover = false;
        refreshContent();
        repaint();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent e) {
        hover = false;
        refreshContent();
        repaint();
        if (!e.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || fileConsumer == null) {
            e.rejectDrop();
            return;
        }
        e.acceptDrop(DnDConstants.ACTION_COPY);
        try {
            List<File> files = (List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            e.dropComplete(!files.isEmpty());
            if (!files.isEmpty()) {
                fileConsumer.accept(files.get(0));
            }
        } catch (Exception ex) {
            e.dropComplete(false);
        }
    }
}
