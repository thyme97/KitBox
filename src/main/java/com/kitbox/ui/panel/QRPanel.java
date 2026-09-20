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
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
    private final JTextField imageField = new JTextField(26);
    private final JLabel decodePreview = new JLabel(" ", javax.swing.SwingConstants.CENTER);

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
        contentScroll.setBorder(BorderFactory.createTitledBorder("二维码内容（文本 / 链接，按 UTF-8 编码）"));

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

        previewLabel.setBorder(BorderFactory.createTitledBorder("预览"));
        JScrollPane previewScroll = new JScrollPane(previewLabel);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton generate = new JButton("生成二维码");
        JButton savePng = new JButton("保存 PNG…");
        JButton copyImage = new JButton("复制图片");
        buttonBar.add(generate);
        buttonBar.add(savePng);
        buttonBar.add(copyImage);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generate));
        savePng.addActionListener(e -> SwingUtils.runWithCatch(this, this::savePng));
        copyImage.addActionListener(e -> SwingUtils.runWithCatch(this, this::copyImage));

        panel.add(top, BorderLayout.NORTH);
        panel.add(previewScroll, BorderLayout.CENTER);
        panel.add(buttonBar, BorderLayout.SOUTH);
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
        SwingUtils.info(this, "图片已复制到剪贴板，可直接粘贴到聊天/文档");
    }

    // ---------------- 识别 ----------------

    private JPanel buildDecodeTab() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton browse = new JButton("浏览…");
        browse.addActionListener(e -> SwingUtils.runWithCatch(this, this::chooseImage));
        JPanel fileRow = new JPanel(new BorderLayout(4, 0));
        fileRow.add(imageField, BorderLayout.CENTER);
        fileRow.add(browse, BorderLayout.EAST);

        com.kitbox.ui.FormPanel form = new com.kitbox.ui.FormPanel();
        form.addField("图片文件：", fileRow);
        form.addHint("支持 PNG / JPG / BMP 等常见格式；截图后直接保存为图片文件即可。");
        form.addGlue();

        JButton decode = new JButton("识别");
        decode.setAlignmentX(LEFT_ALIGNMENT);
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonBar.add(decode);

        decodeArea.setEditable(false);
        decodeArea.setFont(SwingUtils.monoFont(decodeArea.getFont().getSize()));
        decodeArea.setLineWrap(true);
        JScrollPane decodeScroll = new JScrollPane(decodeArea);
        decodeScroll.setBorder(BorderFactory.createTitledBorder("识别结果"));
        JButton copy = new JButton("复制结果");
        copy.addActionListener(e -> {
            if (!decodeArea.getText().isEmpty()) {
                SwingUtils.copyToClipboard(decodeArea.getText());
            }
        });
        JPanel southButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        southButtons.add(copy);

        decodePreview.setPreferredSize(new java.awt.Dimension(200, 200));
        decodePreview.setBorder(BorderFactory.createTitledBorder("图片预览"));

        JPanel center = new JPanel(new BorderLayout());
        center.add(decodeScroll, BorderLayout.CENTER);
        center.add(decodePreview, BorderLayout.EAST);

        decode.addActionListener(e -> SwingUtils.runWithCatch(this, this::decodeImage));

        panel.add(form, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonBar, BorderLayout.NORTH);
        south.add(southButtons, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择二维码图片");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imageField.setText(chooser.getSelectedFile().getAbsolutePath());
            decodePreview.setText(" ");
        }
    }

    private void decodeImage() throws Exception {
        String path = imageField.getText().trim();
        if (path.isEmpty()) {
            throw new IllegalStateException("请选择图片文件");
        }
        BufferedImage image = ImageIO.read(new File(path));
        if (image == null) {
            throw new IOException("无法读取图片：" + path);
        }
        // 预览缩略图
        Image thumb = image.getWidth() > 190 || image.getHeight() > 190
                ? image.getScaledInstance(190, -1, Image.SCALE_SMOOTH) : image;
        decodePreview.setIcon(new javax.swing.ImageIcon(thumb));
        decodePreview.setText(" ");

        String content = QrCodeService.decode(image);
        decodeArea.setText(content);
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(content);
        }
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
