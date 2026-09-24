package com.kitbox.ui.panel;

import com.kitbox.qrcode.QrCodeService;
import com.kitbox.ui.SwingUtils;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * 二维码工具面板：文本 → 二维码图片（生成/保存/复制），图片 → 文本（解码）。
 */
public class QRPanel extends JPanel {

    private final JTextArea contentArea = new JTextArea(6, 36);
    private final JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(320, 128, 1024, 32));
    private final JComboBox<String> ecCombo = new JComboBox<>(new String[]{"M（标准）", "L（低）", "Q（较高）", "H（最高）"});
    private final JSpinner marginSpinner = new JSpinner(new SpinnerNumberModel(2, 0, 8, 1));
    private final JLabel previewLabel = new JLabel("生成后显示在这里", javax.swing.SwingConstants.CENTER);
    private final JTextArea decodeArea = new JTextArea(6, 30);
    private final JLabel decodePreview = new JLabel(" ", javax.swing.SwingConstants.CENTER);

    /** 识别页当前的图片来源：文件或剪贴板位图。 */
    private File pendingFile;
    private BufferedImage pendingImage;
    /** 预览区当前展示的原图（供放大查看）。 */
    private BufferedImage lastPreviewImage;
    /** 识别页拖放区：任何来源（选择/拖入/粘贴）都登记到这里，提供移除按钮。 */
    private com.kitbox.ui.components.DropZone decodeZone;
    /** 放大查看窗口（复用，避免重复打开）。 */
    private com.kitbox.ui.components.ImageZoomDialog zoomDialog;

    private BufferedImage currentImage;

    public QRPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("生成二维码", buildEncodeTab());
        tabs.addTab("识别二维码", buildDecodeTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ---------------- 生成 ----------------

    private JPanel buildEncodeTab() {
        JPanel panel = new JPanel(new BorderLayout());

        contentArea.setFont(SwingUtils.monoFont(contentArea.getFont().getSize()));
        contentArea.setLineWrap(true);
        JScrollPane contentScroll = new JScrollPane(contentArea);
        contentScroll.setBorder(SwingUtils.cardBorder("二维码内容（文本 / 链接，按 UTF-8 编码）"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 6, 3, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("尺寸："), gbc);
        gbc.gridx = 1;
        form.add(sizeSpinner, gbc);
        gbc.gridx = 2;
        form.add(new JLabel("纠错级别："), gbc);
        gbc.gridx = 3;
        form.add(ecCombo, gbc);
        gbc.gridx = 4;
        form.add(new JLabel("边距："), gbc);
        gbc.gridx = 5;
        form.add(marginSpinner, gbc);
        JLabel hint = new JLabel("纠错级别越高越容易扫出，但容量越小");
        hint.setFont(hint.getFont().deriveFont(java.awt.Font.PLAIN, hint.getFont().getSize2D() - 1f));
        gbc.gridx = 6;
        form.add(hint, gbc);

        JPanel top = new JPanel(new BorderLayout());
        top.add(contentScroll, BorderLayout.CENTER);
        top.add(form, BorderLayout.SOUTH);

        previewLabel.setBorder(SwingUtils.cardBorder("预览"));
        JScrollPane previewScroll = new JScrollPane(previewLabel);

        JButton generate = new JButton("生成二维码");
        JButton savePng = new JButton("保存 PNG…");
        JButton copyImage = new JButton("复制图片");
        SwingUtils.stylePrimary(generate);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generate));
        savePng.addActionListener(e -> SwingUtils.runWithCatch(this, this::savePng));
        copyImage.addActionListener(e -> SwingUtils.runWithCatch(this, this::copyImage));

        panel.add(top, BorderLayout.NORTH);
        panel.add(previewScroll, BorderLayout.CENTER);
        panel.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{generate},
                new javax.swing.JComponent[]{savePng, copyImage}), BorderLayout.SOUTH);
        return panel;
    }

    private void generate() throws Exception {
        String content = contentArea.getText();
        if (content.trim().isEmpty()) {
            throw new IllegalStateException("请输入二维码内容");
        }
        int size = (Integer) sizeSpinner.getValue();
        String ec = String.valueOf(ecCombo.getSelectedItem()).substring(0, 1);
        int margin = (Integer) marginSpinner.getValue();
        currentImage = QrCodeService.encode(content, size, ec, margin);
        previewLabel.setIcon(new javax.swing.ImageIcon(currentImage));
        previewLabel.setText(" ");
    }

    private void savePng() throws Exception {
        if (currentImage == null) {
            throw new IllegalStateException("请先生成二维码");
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("保存二维码 PNG");
        chooser.setSelectedFile(new File("qrcode.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            ImageIO.write(currentImage, "png", file);
            SwingUtils.info(this, "已保存：" + file.getAbsolutePath());
        }
    }

    private void copyImage() throws Exception {
        if (currentImage == null) {
            throw new IllegalStateException("请先生成二维码");
        }
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new ImageTransferable(currentImage), null);
        SwingUtils.showToast(previewLabel, "图片已复制到剪贴板");
    }

    // ---------------- 识别 ----------------

    private JPanel buildDecodeTab() {
        JPanel panel = new JPanel(new BorderLayout());

        // 拖放区：点击选择 / 拖入 / Ctrl+V 粘贴（位图优先）
        com.kitbox.ui.components.DropZone zone = new com.kitbox.ui.components.DropZone("image-up");
        decodeZone = zone;
        zone.setPasteHandler(() -> SwingUtils.runWithCatch(this, this::pasteDecodeSource));
        zone.setFileConsumer(file -> {
            zone.setFileName(file.getName());
            pendingFile = file;
            pendingImage = null;
            showPreviewQuietly(file);
            SwingUtils.runWithCatch(this, () -> {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    throw new IOException("无法读取图片：" + file.getName());
                }
                decodeFromImage(image);
            });
        });
        zone.setClearHandler(() -> {
            pendingFile = null;
            pendingImage = null;
            lastPreviewImage = null;
            decodePreview.setIcon(null);
            decodePreview.setText(" ");
            decodeArea.setText("");
        });

        JPanel zoneRow = new JPanel(new GridBagLayout());
        zoneRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 0, 10));
        GridBagConstraints zgc = new GridBagConstraints();
        zgc.gridx = 0;
        zgc.weightx = 1;
        zgc.fill = GridBagConstraints.HORIZONTAL;
        zoneRow.add(zone, zgc);

        JButton decode = new JButton("识别");
        SwingUtils.stylePrimary(decode);
        decode.setAlignmentX(LEFT_ALIGNMENT);

        decodeArea.setEditable(false);
        decodeArea.setFont(SwingUtils.monoFont(decodeArea.getFont().getSize()));
        decodeArea.setLineWrap(true);
        JScrollPane decodeScroll = new JScrollPane(decodeArea);
        decodeScroll.setBorder(null);
        // 只读文本区默认会吞掉 Ctrl+V，这里覆盖为整页粘贴逻辑
        decodeArea.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V,
                        java.awt.event.InputEvent.CTRL_DOWN_MASK), "kitboxPasteSource");
        decodeArea.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V,
                        java.awt.event.InputEvent.CTRL_MASK), "kitboxPasteSource");
        decodeArea.getActionMap().put("kitboxPasteSource", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                SwingUtils.runWithCatch(QRPanel.this, QRPanel.this::pasteDecodeSource);
            }
        });
        // 复制按钮放在结果卡标题行，紧挨内容
        JButton copy = SwingUtils.iconButton("copy", "复制", "复制结果");
        copy.addActionListener(e -> {
            if (!decodeArea.getText().isEmpty()) {
                SwingUtils.copyToClipboard(decodeArea.getText());
                SwingUtils.showToast(copy, "已复制到剪贴板");
            }
        });
        JPanel resultBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        resultBar.setOpaque(false);
        resultBar.add(copy);
        JPanel resultCaption = new JPanel(new BorderLayout());
        resultCaption.setOpaque(false);
        resultCaption.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 10, 0, 6));
        resultCaption.add(SwingUtils.groupLabel("识别结果"), BorderLayout.WEST);
        resultCaption.add(resultBar, BorderLayout.EAST);
        JPanel resultCard = new JPanel(new BorderLayout());
        resultCard.setBorder(SwingUtils.cardLineBorder());
        resultCard.add(resultCaption, BorderLayout.NORTH);
        resultCard.add(decodeScroll, BorderLayout.CENTER);

        // 预览区：与结果卡同构（标题行 + 线框），点击可放大查看
        decodePreview.setPreferredSize(new java.awt.Dimension(400, 460));
        decodePreview.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        decodePreview.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (lastPreviewImage == null) {
                    return;
                }
                if (zoomDialog != null && zoomDialog.isShowing()) {
                    zoomDialog.setImage(lastPreviewImage);
                    zoomDialog.toFront();
                } else {
                    zoomDialog = new com.kitbox.ui.components.ImageZoomDialog(
                            javax.swing.SwingUtilities.windowForComponent(QRPanel.this),
                            "二维码图片", lastPreviewImage);
                    zoomDialog.setVisible(true);
                }
            }
        });
        JPanel previewCaption = new JPanel(new BorderLayout());
        previewCaption.setOpaque(false);
        previewCaption.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 10, 0, 6));
        previewCaption.add(SwingUtils.groupLabel("图片预览（点击放大）"), BorderLayout.WEST);
        JPanel previewCard = new JPanel(new BorderLayout());
        previewCard.setBorder(SwingUtils.cardLineBorder());
        previewCard.add(previewCaption, BorderLayout.NORTH);
        previewCard.add(decodePreview, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(6, 0));
        center.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 10, 10));
        center.add(resultCard, BorderLayout.CENTER);
        center.add(previewCard, BorderLayout.EAST);

        decode.addActionListener(e -> SwingUtils.runWithCatch(this, this::decodeImage));

        panel.add(zoneRow, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{decode},
                new javax.swing.JComponent[0]), BorderLayout.SOUTH);
        return panel;
    }

    /** 选择/拖入后立即显示图片预览（不自动识别，不打扰）。 */
    private void showPreviewQuietly(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                decodePreview.setIcon(null);
                decodePreview.setText("无法读取图片");
                return;
            }
            setPreviewThumb(image);
        } catch (Exception e) {
            decodePreview.setIcon(null);
            decodePreview.setText("无法读取图片");
        }
    }

    /** Ctrl+V / 粘贴按钮：优先剪贴板图片（截图/网页图片），其次剪贴板文件或路径。 */
    private void pasteDecodeSource() throws Exception {
        java.awt.Image clipboard = SwingUtils.clipboardImage();
        if (clipboard != null) {
            pendingImage = SwingUtils.toBufferedImage(clipboard);
            pendingFile = null;
            if (decodeZone != null) {
                decodeZone.setFileName("剪贴板图片");
            }
            decodeFromImage(pendingImage);
            return;
        }
        String path = SwingUtils.clipboardFilePathOrText();
        if (path == null || path.isEmpty()) {
            throw new IllegalStateException("剪贴板中没有图片或文件");
        }
        File file = new File(SwingUtils.normalizeFilePath(path));
        if (!file.isFile()) {
            throw new IllegalStateException("文件不存在：" + file);
        }
        pendingFile = file;
        pendingImage = null;
        if (decodeZone != null) {
            decodeZone.setFileName(file.getName());
        }
        showPreviewQuietly(file);
        decodeImage();
    }

    private void decodeImage() throws Exception {
        if (pendingImage != null) {
            decodeFromImage(pendingImage);
            return;
        }
        if (pendingFile == null) {
            throw new IllegalStateException("请先选择图片：点击拖放区选择、拖入文件或 Ctrl+V 粘贴");
        }
        BufferedImage image = ImageIO.read(pendingFile);
        if (image == null) {
            throw new IOException("无法读取图片：" + pendingFile.getName());
        }
        decodeFromImage(image);
    }

    /** 设置预览缩略图（同时记录原图供放大查看）。 */
    private void setPreviewThumb(BufferedImage image) {
        lastPreviewImage = image;
        decodePreview.setIcon(SwingUtils.scaledIcon(image, 370, 370));
        decodePreview.setText(" ");
    }

    /** 显示预览缩略图并识别内容。 */
    private String decodeFromImage(BufferedImage image) throws Exception {
        setPreviewThumb(image);
        String content = QrCodeService.decode(image);
        decodeArea.setText(content);
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(content);
            SwingUtils.showToast(decodeArea, "结果已自动复制");
        }
        return content;
    }

    /** 图片剪贴板传输对象。 */
    private static final class ImageTransferable implements Transferable {
        private final Image image;

        ImageTransferable(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
