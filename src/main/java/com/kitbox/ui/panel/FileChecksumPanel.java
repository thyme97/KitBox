package com.kitbox.ui.panel;

import com.kitbox.tools.ChecksumService;
import com.kitbox.ui.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.DefaultListModel;
import javax.swing.UIManager;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件批量校验面板：拖入 / 添加文件，流式计算 MD5 / SHA-1 / SHA-256 / SM3，
 * 生成并另存 GNU 校验和清单，或导入清单逐文件校验完整性。
 */
public class FileChecksumPanel extends JPanel {

    private final JComboBox<ChecksumService.FileAlg> algCombo =
            new JComboBox<>(ChecksumService.FileAlg.values());
    private final DefaultListModel<Path> fileModel = new DefaultListModel<>();
    private final JList<Path> fileList = new JList<>(fileModel);
    private final javax.swing.JTextPane resultPane = new javax.swing.JTextPane();

    /** 最近一次「计算哈希」的结果，供生成清单使用。 */
    private List<ChecksumService.FileHash> lastHashes = new ArrayList<>();

    public FileChecksumPanel() {
        super(new BorderLayout());

        JPanel paramBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        paramBar.add(algCombo);
        JButton addFiles = new JButton("添加文件");
        JButton removeSelected = new JButton("移除选中");
        JButton clearFiles = new JButton("清空列表");
        paramBar.add(addFiles);
        paramBar.add(removeSelected);
        paramBar.add(clearFiles);

        addFiles.addActionListener(e -> chooseFiles());
        removeSelected.addActionListener(e -> {
            for (Path path : fileList.getSelectedValuesList()) {
                fileModel.removeElement(path);
            }
        });
        clearFiles.addActionListener(e -> fileModel.clear());

        fileList.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                try {
                    List<File> dropped =
                            (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : dropped) {
                        Path path = file.toPath();
                        if (file.isFile() && !fileModel.contains(path)) {
                            fileModel.addElement(path);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(SwingUtils.cardBorder("文件列表（可从资源管理器拖入）"));
        listScroll.setPreferredSize(new Dimension(100, 130));

        JButton hashButton = new JButton("计算哈希");
        JButton manifestButton = new JButton("生成清单");
        JButton saveManifest = new JButton("另存清单");
        JButton verifyButton = new JButton("校验清单");
        SwingUtils.uniformSize(hashButton, manifestButton, saveManifest, verifyButton);
        hashButton.addActionListener(e -> runWithCatch(this::calculateHashes));
        manifestButton.addActionListener(e -> runWithCatch(this::generateManifest));
        saveManifest.addActionListener(e -> runWithCatch(this::saveManifest));
        verifyButton.addActionListener(e -> runWithCatch(this::verifyManifest));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonBar.add(hashButton);
        buttonBar.add(manifestButton);
        buttonBar.add(saveManifest);
        buttonBar.add(verifyButton);

        resultPane.setEditable(false);
        resultPane.setFont(mono());
        JScrollPane resultScroll = new JScrollPane(resultPane);
        resultScroll.setBorder(SwingUtils.cardBorder("结果"));

        JPanel south = new JPanel(new BorderLayout());
        south.add(buttonBar, BorderLayout.NORTH);
        south.add(resultScroll, BorderLayout.CENTER);

        add(paramBar, BorderLayout.NORTH);
        add(listScroll, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);
    }

    private void chooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("选择要校验的文件");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                Path path = file.toPath();
                if (!fileModel.contains(path)) {
                    fileModel.addElement(path);
                }
            }
        }
    }

    private void calculateHashes() {
        if (fileModel.isEmpty()) {
            SwingUtils.error(this, "请先添加或拖入文件");
            return;
        }
        List<Path> paths = new ArrayList<>(fileModel.size());
        for (int i = 0; i < fileModel.size(); i++) {
            paths.add(fileModel.get(i));
        }
        ChecksumService.FileAlg alg = (ChecksumService.FileAlg) algCombo.getSelectedItem();
        resultPane.setText("");
        appendResult("正在计算…（" + alg.getDisplay() + "）\n");

        SwingWorker<List<ChecksumService.FileHash>, ChecksumService.FileHash> worker =
                new SwingWorker<List<ChecksumService.FileHash>, ChecksumService.FileHash>() {
                    @Override
                    protected List<ChecksumService.FileHash> doInBackground() {
                        List<ChecksumService.FileHash> hashes = new ArrayList<>(paths.size());
                        for (Path path : paths) {
                            try {
                                ChecksumService.FileHash hash = new ChecksumService.FileHash(
                                        path.getFileName().toString(), Files.size(path),
                                        ChecksumService.hashFile(path, alg));
                                hashes.add(hash);
                                publish(hash);
                            } catch (Exception e) {
                                ChecksumService.FileHash failed = new ChecksumService.FileHash(
                                        path.getFileName().toString(), 0, "读取失败：" + e.getMessage());
                                hashes.add(failed);
                                publish(failed);
                            }
                        }
                        return hashes;
                    }

                    @Override
                    protected void process(List<ChecksumService.FileHash> chunks) {
                        for (ChecksumService.FileHash hash : chunks) {
                            appendResult(renderHash(hash));
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            lastHashes = get();
                            appendResult("完成，共 " + lastHashes.size() + " 个文件。\n");
                        } catch (Exception e) {
                            SwingUtils.error(FileChecksumPanel.this, "计算失败：" + e.getMessage());
                        }
                    }
                };
        worker.execute();
    }

    private void generateManifest() {
        if (lastHashes.isEmpty()) {
            SwingUtils.error(this, "请先「计算哈希」");
            return;
        }
        resultPane.setText(ChecksumService.buildManifest(lastHashes));
        appendResult("\n（清单已生成：可「另存清单」，或直接复制内容）\n");
    }

    private void saveManifest() {
        if (resultPane.getText().trim().isEmpty()) {
            SwingUtils.error(this, "结果区为空，请先计算哈希或生成清单");
            return;
        }
        ChecksumService.FileAlg alg = (ChecksumService.FileAlg) algCombo.getSelectedItem();
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("另存校验清单");
        chooser.setSelectedFile(new File("checksums." + manifestExt(alg)));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        try {
            Files.write(target, resultPane.getText().getBytes(StandardCharsets.UTF_8));
            SwingUtils.info(this, "清单已保存：" + target);
        } catch (IOException e) {
            SwingUtils.error(this, "保存失败：" + e.getMessage());
        }
    }

    private void verifyManifest() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择校验清单（以清单所在目录为基准）");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File manifestFile = chooser.getSelectedFile();
        ChecksumService.FileAlg alg = (ChecksumService.FileAlg) algCombo.getSelectedItem();
        resultPane.setText("正在校验…（算法：" + alg.getDisplay() + "，基准目录：" + manifestFile.getParent() + "）\n");

        SwingWorker<List<ChecksumService.VerifyItem>, Object[]> worker =
                new SwingWorker<List<ChecksumService.VerifyItem>, Object[]>() {
                    @Override
                    protected List<ChecksumService.VerifyItem> doInBackground() throws Exception {
                        String text = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
                        List<ChecksumService.ManifestEntry> entries = ChecksumService.parseManifest(text);
                        List<ChecksumService.VerifyItem> items = ChecksumService.verify(entries, manifestFile.getParentFile().toPath(), alg);
                        publish(new Object[]{items});
                        return items;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    protected void process(List<Object[]> chunks) {
                        for (Object[] chunk : chunks) {
                            renderVerify((List<ChecksumService.VerifyItem>) chunk[0]);
                        }
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        } catch (Exception e) {
                            resultPane.setText("");
                            SwingUtils.error(FileChecksumPanel.this, "校验失败：" + e.getMessage());
                        }
                    }
                };
        worker.execute();
    }

    private void renderVerify(List<ChecksumService.VerifyItem> items) {
        int ok = 0;
        int mismatch = 0;
        int missing = 0;
        for (ChecksumService.VerifyItem item : items) {
            switch (item.status) {
                case OK:
                    appendResult("✓ " + item.name + "  " + item.actual + "\n", okColor());
                    ok++;
                    break;
                case MISMATCH:
                    appendResult("✗ " + item.name + "  不匹配（期望 " + item.expected
                            + "，实际 " + item.actual + "）\n", badColor());
                    mismatch++;
                    break;
                default:
                    appendResult("⚠ " + item.name + "  缺失\n", warnColor());
                    missing++;
                    break;
            }
        }
        appendResult(String.format("校验完成：%d 通过，%d 不匹配，%d 缺失（共 %d）。%n",
                ok, mismatch, missing, items.size()));
        if (mismatch == 0 && missing == 0) {
            appendResult("全部通过。\n", okColor());
        }
    }

    /** 向结果区追加文本；color 为 null 时用默认前景色。 */
    private void appendResult(String text, java.awt.Color color) {
        javax.swing.text.SimpleAttributeSet attrs = new javax.swing.text.SimpleAttributeSet();
        if (color != null) {
            javax.swing.text.StyleConstants.setForeground(attrs, color);
        }
        try {
            resultPane.getDocument().insertString(
                    resultPane.getDocument().getLength(), text, attrs);
        } catch (javax.swing.text.BadLocationException ignored) {
        }
    }

    private void appendResult(String text) {
        appendResult(text, null);
    }

    private static java.awt.Color themedColor(String key, int fallbackRgb) {
        java.awt.Color color = UIManager.getColor(key);
        return color != null ? color : new java.awt.Color(fallbackRgb);
    }

    private static java.awt.Color okColor() {
        return themedColor("Actions.Green", 0x2E7D32);
    }

    private static java.awt.Color badColor() {
        return themedColor("Actions.Red", 0xC62828);
    }

    private static java.awt.Color warnColor() {
        return themedColor("Actions.Yellow", 0xB26A00);
    }

    private static String renderHash(ChecksumService.FileHash hash) {
        return hash.name + "  (" + humanSize(hash.size) + ")  " + hash.hash + "\n";
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        for (String unit : new String[]{"KB", "MB", "GB", "TB"}) {
            value /= 1024;
            if (value < 1024) {
                return String.format("%.1f %s", value, unit);
            }
        }
        return String.format("%.1f PB", value / 1024);
    }

    private static String manifestExt(ChecksumService.FileAlg alg) {
        switch (alg) {
            case MD5:
                return "md5";
            case SHA1:
                return "sha1";
            case SM3:
                return "sm3";
            default:
                return "sha256";
        }
    }

    private void runWithCatch(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            SwingUtils.error(this, "操作失败：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private static Font mono() {
        return SwingUtils.monoFont(new JTextArea().getFont().getSize());
    }
}
