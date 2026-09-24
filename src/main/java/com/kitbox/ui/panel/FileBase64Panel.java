package com.kitbox.ui.panel;

import com.kitbox.tools.FileBase64Service;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.DropZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件 / 图片与 Base64 互转：拖放区支持点击选择、拖入、Ctrl+V 粘贴
 * （剪贴板里的截图/网页图片也能直接编码）；粘贴 Base64 后自动预览图片，
 * 「另存为…」解码保存，文件名按 Data URI 类型推荐。
 */
public class FileBase64Panel extends JPanel {

    private final DropZone sourceZone = new DropZone("file-up");
    private final JCheckBox dataUriCheck = new JCheckBox(
            "输出带 Data URI 前缀（data:image/png;base64,…，按扩展名识别类型）");
    private final JTextArea base64Area = new JTextArea(10, 40);
    private final JLabel previewLabel = new JLabel(" ", javax.swing.SwingConstants.CENTER);
    private final Timer previewTimer;
    /** 预览区当前展示的原图（供放大查看）。 */
    private Image previewImage;
    /** 放大查看窗口（复用，避免重复打开）。 */
    private com.kitbox.ui.components.ImageZoomDialog zoomDialog;

    public FileBase64Panel() {
        super(new BorderLayout());

        // 文件 → Base64 拖放区
        sourceZone.setFileConsumer(file -> {
            sourceZone.setFileName(file.getName());
            SwingUtils.runWithCatch(this, () -> doEncode(file));
        });
        sourceZone.setPasteHandler(this::pasteSource);
        sourceZone.setClearHandler(() -> { /* 仅清空显示，Base64 内容保留 */ });

        JPanel encodeCard = new JPanel(new BorderLayout());
        encodeCard.setBorder(SwingUtils.cardBorder("文件 → Base64"));
        encodeCard.add(sourceZone, BorderLayout.CENTER);
        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        checkRow.setOpaque(false);
        checkRow.setBorder(BorderFactory.createEmptyBorder(2, 12, 4, 12));
        checkRow.add(dataUriCheck);
        encodeCard.add(checkRow, BorderLayout.SOUTH);

        JPanel forms = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 10, 0, 10);
        gc.gridy = 0;
        forms.add(encodeCard, gc);

        // 共享 Base64 文本区 + 图片自动预览
        JPanel base64Card = new JPanel(new BorderLayout());
        base64Card.setBorder(SwingUtils.cardBorder("Base64 内容（编码结果在此输出；粘贴待解码内容可预览/另存）"));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        JButton pasteText = SwingUtils.iconButton("clipboard-paste", "粘贴", "粘贴");
        pasteText.addActionListener(e -> {
            try {
                Object text = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
                if (text != null) {
                    base64Area.setText(String.valueOf(text));
                }
            } catch (Exception ignored) {
            }
        });
        JButton copyText = SwingUtils.iconButton("copy", "复制", "复制");
        copyText.addActionListener(e -> {
            if (!base64Area.getText().isEmpty()) {
                SwingUtils.copyToClipboard(base64Area.getText());
                SwingUtils.showToast(copyText, "已复制到剪贴板");
            }
        });
        JButton clearText = SwingUtils.iconButton("trash-2", "清空", "清空");
        clearText.addActionListener(e -> base64Area.setText(""));
        bar.add(pasteText);
        bar.add(copyText);
        bar.add(clearText);
        JPanel caption = new JPanel(new BorderLayout());
        caption.setBorder(BorderFactory.createEmptyBorder(2, 10, 0, 6));
        caption.add(bar, BorderLayout.EAST);
        base64Area.setFont(SwingUtils.monoFont(base64Area.getFont().getSize()));
        base64Area.setLineWrap(true);
        base64Card.add(caption, BorderLayout.NORTH);
        base64Card.add(new JScrollPane(base64Area), BorderLayout.CENTER);

        previewLabel.setPreferredSize(new java.awt.Dimension(210, 210));
        previewLabel.setBorder(SwingUtils.cardBorder("内容预览（点击放大）"));
        previewLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        previewLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!(previewImage instanceof java.awt.image.BufferedImage)) {
                    return;
                }
                if (zoomDialog != null && zoomDialog.isShowing()) {
                    zoomDialog.setImage((java.awt.image.BufferedImage) previewImage);
                    zoomDialog.toFront();
                } else {
                    zoomDialog = new com.kitbox.ui.components.ImageZoomDialog(
                            javax.swing.SwingUtilities.windowForComponent(FileBase64Panel.this),
                            "内容预览", (java.awt.image.BufferedImage) previewImage);
                    zoomDialog.setVisible(true);
                }
            }
        });

        JButton saveAs = new JButton("另存为…");
        JButton clear = new JButton("清空");
        SwingUtils.styleSecondary(saveAs);
        saveAs.addActionListener(e -> SwingUtils.runWithCatch(this, this::doSaveAs));
        clear.addActionListener(e -> base64Area.setText(""));

        JPanel north = new JPanel(new BorderLayout());
        north.add(forms, BorderLayout.NORTH);
        north.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{saveAs},
                new javax.swing.JComponent[]{clear}), BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(base64Card, BorderLayout.CENTER);
        center.add(previewLabel, BorderLayout.EAST);

        add(north, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // 内容变化后自动尝试图片预览（防抖 + 后台解码）
        previewTimer = new Timer(600, e -> updatePreview());
        previewTimer.setRepeats(false);
        base64Area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                previewTimer.restart();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                previewTimer.restart();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                previewTimer.restart();
            }
        });

        // 拖入文本 → 作为 Base64 内容（文件拖放由拖放区处理）
        setTransferHandler(new javax.swing.TransferHandler() {
            @Override
            public boolean canImport(javax.swing.TransferHandler.TransferSupport support) {
                return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(javax.swing.TransferHandler.TransferSupport support) {
                try {
                    Object text = support.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                    if (text != null && !String.valueOf(text).trim().isEmpty()) {
                        base64Area.setText(String.valueOf(text));
                        return true;
                    }
                } catch (Exception ignored) {
                }
                return false;
            }
        });
    }

    private void pasteSource() {
        // 优先剪贴板位图（截图/网页图片直接编码）
        Image image = SwingUtils.clipboardImage();
        if (image != null) {
            java.awt.image.BufferedImage buffered = SwingUtils.toBufferedImage(image);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                javax.imageio.ImageIO.write(buffered, "png", bos);
            } catch (Exception e) {
                SwingUtils.error(this, "无法读取剪贴板图片");
                return;
            }
            sourceZone.setFileName("剪贴板图片.png");
            doEncodeBytes(bos.toByteArray(), "image/png");
            return;
        }
        // 其次剪贴板文件/路径
        String path = SwingUtils.clipboardFilePathOrText();
        if (path == null || path.isEmpty()) {
            SwingUtils.info(this, "剪贴板中没有图片或文件");
            return;
        }
        File file = new File(SwingUtils.normalizeFilePath(path));
        if (file.isFile()) {
            sourceZone.setFileName(file.getName());
            SwingUtils.runWithCatch(this, () -> doEncode(file));
        } else {
            SwingUtils.error(this, "文件不存在：" + file);
        }
    }

    private void doEncode(File file) throws Exception {
        if (file.length() > 20L * 1024 * 1024 && !SwingUtils.confirm(this,
                "文件超过 20MB，转出的 Base64 文本较长，可能占用较多内存。继续？")) {
            return;
        }
        String base64 = FileBase64Service.encode(file.toPath(), dataUriCheck.isSelected());
        base64Area.setText(base64);
        base64Area.setCaretPosition(0);
    }

    private void doEncodeBytes(byte[] data, String mime) {
        String base64 = FileBase64Service.encode(data, mime, dataUriCheck.isSelected());
        base64Area.setText(base64);
        base64Area.setCaretPosition(0);
    }

    private void doSaveAs() throws Exception {
        byte[] bytes = FileBase64Service.decode(base64Area.getText());
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(FileBase64Service.suggestFileName(base64Area.getText())));
        chooser.setDialogTitle("解码保存");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path out = chooser.getSelectedFile().toPath();
        Files.write(out, bytes);
        SwingUtils.info(this, "已保存：" + out.toAbsolutePath() + "（" + bytes.length + " 字节）");
    }

    /** 尝试把当前 Base64 内容解码为图片预览（后台执行，失败静默降级为提示）。 */
    private void updatePreview() {
        final String text = base64Area.getText();
        if (text.trim().isEmpty()) {
            previewImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText("（粘贴 Base64 后自动预览图片）");
            return;
        }
        if (text.length() > 16_000_000) {
            previewImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText("（内容过大，未自动预览）");
            return;
        }
        new SwingWorker<Image, Void>() {
            @Override
            protected Image doInBackground() {
                try {
                    byte[] bytes = FileBase64Service.decode(text);
                    return javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Image image = get();
                    if (image == null) {
                        previewImage = null;
                        previewLabel.setIcon(null);
                        previewLabel.setText("（非图片内容，可「另存为…」保存）");
                        return;
                    }
                    previewImage = image;
                    previewLabel.setIcon(SwingUtils.scaledIcon(
                            SwingUtils.toBufferedImage(image), 190, 190));
                    previewLabel.setText(" ");
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }
}
