package com.kitbox.ui.panel;

import com.kitbox.AppContext;
import com.kitbox.keystore.KeyEntry;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.crypto.model.ContentFormat;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.keystore.KeyStoreException;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.KeyEntryDialog;
import com.kitbox.ui.components.KeyGenDialog;
import com.kitbox.ui.components.KeyStoreDialogs;
import com.kitbox.util.HexUtils;
import com.kitbox.util.KeyCodec;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;

/**
 * 密钥库管理面板：场景分组、生成/导入/编辑/删除密钥、PEM 导出、加密备份。
 */
public class KeyManagerPanel extends JPanel {

    private static final String CARD_INIT = "init";
    private static final String CARD_LOCKED = "locked";
    private static final String CARD_MAIN = "main";

    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);

    private final JTree tree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("密钥库")));
    private final JTextArea detailArea = new JTextArea();

    public KeyManagerPanel() {
        setLayout(new BorderLayout());
        cardPanel.add(buildInitCard(), CARD_INIT);
        cardPanel.add(buildLockedCard(), CARD_LOCKED);
        cardPanel.add(buildMainCard(), CARD_MAIN);
        add(cardPanel, BorderLayout.CENTER);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refreshState();
            }
        });
    }

    // ---------------- 卡片 ----------------

    private JPanel buildInitCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("<html><div style='text-align:center'>"
                + "<h2>尚未创建密钥库</h2>"
                + "<p>密钥库用于按场景（开发/测试/生产等）加密保存常用密钥，<br>也可以不设置密码（明文保存）。</p>"
                + "<p><font color='#C62828'>主密码用于加密本地密钥库文件，一旦遗忘将无法找回密钥！</font></p>"
                + "</div></html>", javax.swing.SwingConstants.CENTER);
        JButton init = new JButton("初始化密钥库…");
        init.addActionListener(e -> {
            if (KeyStoreDialogs.showInitDialog(this)) {
                refreshState();
            }
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(12, 12, 12, 12);
        panel.add(label, gbc);
        gbc.gridy = 1;
        panel.add(init, gbc);
        return panel;
    }

    private JPanel buildLockedCard() {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel("密钥库已锁定", javax.swing.SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(16f));
        JButton unlock = new JButton("解锁密钥库…");
        unlock.addActionListener(e -> {
            if (KeyStoreDialogs.showUnlockDialog(this)) {
                refreshState();
            }
        });
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(12, 12, 12, 12);
        panel.add(label, gbc);
        gbc.gridy = 1;
        panel.add(unlock, gbc);
        return panel;
    }

    private JPanel buildMainCard() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel toolbar = new JPanel(new com.kitbox.ui.components.WrapLayout(FlowLayout.LEFT, 6, 4));
        JButton gen = new JButton("生成密钥…");
        JButton imp = new JButton("导入密钥…");
        JButton edit = new JButton("编辑…");
        JButton del = new JButton("删除");
        JButton exportPem = new JButton("导出 PEM…");
        JButton addScenario = new JButton("新建场景…");
        JButton renameScenario = new JButton("重命名场景…");
        JButton delScenario = new JButton("删除场景");
        JButton backup = new JButton("导出备份…");
        JButton restore = new JButton("导入备份…");
        JButton changePwd = new JButton("修改密码…");
        JButton lock = new JButton("锁定");
        toolbar.add(gen);
        toolbar.add(imp);
        toolbar.add(edit);
        toolbar.add(del);
        toolbar.add(exportPem);
        toolbar.add(separator());
        toolbar.add(addScenario);
        toolbar.add(renameScenario);
        toolbar.add(delScenario);
        toolbar.add(separator());
        toolbar.add(backup);
        toolbar.add(restore);
        toolbar.add(changePwd);
        toolbar.add(separator());
        toolbar.add(lock);

        gen.addActionListener(e -> runDialog(() -> new KeyGenDialog(owner(), this::refreshTree).setVisible(true)));
        imp.addActionListener(e -> runDialog(() -> new KeyEntryDialog(owner(), this::refreshTree).setVisible(true)));
        edit.addActionListener(e -> SwingUtils.runWithCatch(this, this::editSelected));
        del.addActionListener(e -> SwingUtils.runWithCatch(this, this::deleteSelected));
        exportPem.addActionListener(e -> SwingUtils.runWithCatch(this, this::exportPem));
        addScenario.addActionListener(e -> SwingUtils.runWithCatch(this, this::addScenario));
        renameScenario.addActionListener(e -> SwingUtils.runWithCatch(this, this::renameScenario));
        delScenario.addActionListener(e -> SwingUtils.runWithCatch(this, this::deleteScenario));
        backup.addActionListener(e -> SwingUtils.runWithCatch(this, this::exportBackup));
        restore.addActionListener(e -> SwingUtils.runWithCatch(this, this::importBackup));
        lock.addActionListener(e -> {
            AppContext.keyStore.lock();
            refreshState();
        });
        changePwd.addActionListener(e -> {
            if (KeyStoreDialogs.showChangePasswordDialog(this)) {
                refreshState();
            }
        });

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> showDetail());
        detailArea.setEditable(false);
        detailArea.setFont(SwingUtils.monoFont(detailArea.getFont().getSize()));
        detailArea.setBorder(BorderFactory.createTitledBorder("密钥详情"));

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setPreferredSize(new java.awt.Dimension(280, 400));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, new JScrollPane(detailArea));
        split.setDividerLocation(280);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void runDialog(Runnable runnable) {
        if (KeyStoreDialogs.ensureUnlocked(this)) {
            runnable.run();
        }
    }

    private javax.swing.JSeparator separator() {
        javax.swing.JSeparator sep = new javax.swing.JSeparator();
        sep.setPreferredSize(new java.awt.Dimension(2, 24));
        return sep;
    }

    private javax.swing.JFrame owner() {
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        return w instanceof javax.swing.JFrame ? (javax.swing.JFrame) w : null;
    }

    // ---------------- 状态刷新 ----------------

    public void refreshState() {
        if (!AppContext.keyStore.exists()) {
            cards.show(cardPanel, CARD_INIT);
            return;
        }
        if (!AppContext.keyStore.isUnlocked() && AppContext.keyStore.isPlainFile()) {
            // 无密码库：自动加载，无需解锁
            try {
                AppContext.keyStore.unlock(new char[0]);
            } catch (KeyStoreException ignored) {
            }
        }
        if (!AppContext.keyStore.isUnlocked()) {
            cards.show(cardPanel, CARD_LOCKED);
        } else {
            refreshTree();
            cards.show(cardPanel, CARD_MAIN);
        }
    }

    private void refreshTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("密钥库");
        java.util.List<KeyEntry> entries = AppContext.keyStore.getEntries();
        for (String scenario : AppContext.keyStore.getScenarios()) {
            long count = entries.stream().filter(e -> e.getScenario().equals(scenario)).count();
            DefaultMutableTreeNode scenarioNode = new DefaultMutableTreeNode(scenario + "  (" + count + ")");
            for (KeyEntry entry : entries) {
                // 空安全：脏条目归入“未分组”，避免空指针导致整个面板不可用
                String entryScenario = entry.getScenario() == null ? "未分组" : entry.getScenario();
                if (entryScenario.equals(scenario)) {
                    scenarioNode.add(new DefaultMutableTreeNode(entry));
                }
            }
            root.add(scenarioNode);
        }
        tree.setModel(new DefaultTreeModel(root));
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        detailArea.setText("");
    }

    private KeyEntry selectedEntry() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode
                && ((DefaultMutableTreeNode) node).getUserObject() instanceof KeyEntry) {
            return (KeyEntry) ((DefaultMutableTreeNode) node).getUserObject();
        }
        return null;
    }

    private String selectedScenario() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object node = path.getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode) {
            Object user = ((DefaultMutableTreeNode) node).getUserObject();
            if (user instanceof KeyEntry) {
                return ((KeyEntry) user).getScenario();
            }
            String text = String.valueOf(user);
            int idx = text.lastIndexOf("  (");
            return idx > 0 ? text.substring(0, idx) : text;
        }
        return null;
    }

    // ---------------- 操作 ----------------

    private void showDetail() {
        KeyEntry entry = selectedEntry();
        if (entry == null) {
            detailArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("名称：").append(entry.getName() == null ? "" : entry.getName()).append('\n');
        sb.append("场景：").append(entry.getScenario() == null ? "未分组" : entry.getScenario()).append('\n');
        sb.append("类型：").append(entry.getType() == null ? "（未知，条目数据不完整）" : entry.getType().getDisplay()).append('\n');
        sb.append("值：").append("\n").append(prettyValue(entry)).append('\n');
        sb.append("备注：").append(entry.getRemark() == null ? "" : entry.getRemark()).append('\n');
        sb.append("创建时间：").append(formatTime(entry.getCreatedAt()));
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private String prettyValue(KeyEntry entry) {
        String value = entry.getValue();
        if (value == null) {
            return "（条目数据不完整）";
        }
        if (entry.getType().getKind() == KeyEntryType.Kind.KEYPAIR) {
            return "公钥(Base64)：\n" + KeyStoreCodec.partValueJson(value, "publicKey")
                    + "\n\n私钥(Base64)：\n" + KeyStoreCodec.partValueJson(value, "privateKey");
        }
        if (entry.getType().getKind() == KeyEntryType.Kind.SYMMETRIC
                || entry.getType().getKind() == KeyEntryType.Kind.HMAC) {
            try {
                byte[] keyBytes = KeyStoreCodec.symmetricKeyBytes(entry);
                StringBuilder sb = new StringBuilder();
                sb.append("Base64：\n").append(value);
                sb.append("\n\nHex：\n").append(HexUtils.encode(keyBytes));
                if (ContentFormat.isPrintableUtf8(keyBytes)) {
                    sb.append("\n\n明文：\n").append(
                            new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8));
                }
                return sb.toString();
            } catch (Exception e) {
                return value;
            }
        }
        return value;
    }

    private String formatTime(String iso) {
        try {
            return java.time.LocalDateTime.parse(iso)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return iso == null ? "" : iso;
        }
    }

    private void editSelected() throws KeyStoreException {
        if (!AppContext.keyStore.isUnlocked()) {
            throw new KeyStoreException("请先解锁密钥库");
        }
        KeyEntry entry = selectedEntry();
        if (entry == null) {
            throw new KeyStoreException("请先选择要编辑的密钥");
        }
        new KeyEntryDialog(owner(), entry, this::refreshTree).setVisible(true);
    }

    private void deleteSelected() throws KeyStoreException {
        KeyEntry entry = selectedEntry();
        if (entry == null) {
            throw new KeyStoreException("请先选择要删除的密钥");
        }
        if (!SwingUtils.confirm(this, "确定删除密钥「" + entry.getName() + "」（" + entry.getScenario() + "）？")) {
            return;
        }
        AppContext.keyStore.deleteEntry(entry.getId());
        refreshTree();
    }

    private void exportPem() throws Exception {
        KeyEntry entry = selectedEntry();
        if (entry == null) {
            throw new KeyStoreException("请先选择要导出的密钥");
        }
        switch (entry.getType()) {
            case RSA_PUBLIC:
            case SM2_PUBLIC:
                saveText("保存公钥 PEM", KeyCodec.pemFromPublicBase64(publicBase64Of(entry)));
                break;
            case RSA_PRIVATE:
            case SM2_PRIVATE:
                saveText("保存私钥 PEM", KeyCodec.pemFromPrivateBase64(privateBase64Of(entry)));
                break;
            case RSA_KEYPAIR:
            case SM2_KEYPAIR:
                saveText("保存公钥 PEM", KeyCodec.pemFromPublicBase64(publicBase64Of(entry)));
                saveText("保存私钥 PEM", KeyCodec.pemFromPrivateBase64(privateBase64Of(entry)));
                break;
            default:
                throw new KeyStoreException("该类型不支持 PEM 导出（仅支持 RSA/SM2 密钥）");
        }
        SwingUtils.info(this, "PEM 导出完成");
    }

    private String publicBase64Of(KeyEntry entry) {
        return entry.getType().getKind() == KeyEntryType.Kind.KEYPAIR
                ? KeyStoreCodec.partValueJson(entry.getValue(), "publicKey")
                : entry.getValue();
    }

    private String privateBase64Of(KeyEntry entry) {
        return entry.getType().getKind() == KeyEntryType.Kind.KEYPAIR
                ? KeyStoreCodec.partValueJson(entry.getValue(), "privateKey")
                : entry.getValue();
    }

    private void saveText(String title, String content) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setSelectedFile(new File(suggestName(title)));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Files.write(chooser.getSelectedFile().toPath(), content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String suggestName(String title) {
        return title.contains("公钥") ? "public.pem" : "private.pem";
    }

    private void addScenario() throws KeyStoreException {
        if (!AppContext.keyStore.isUnlocked()) {
            throw new KeyStoreException("请先解锁密钥库");
        }
        String name = JOptionPane.showInputDialog(this, "新场景名称：", "新建场景",
                JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        AppContext.keyStore.addScenario(name);
        refreshTree();
    }

    private void renameScenario() throws KeyStoreException {
        String oldName = selectedScenario();
        if (oldName == null) {
            throw new KeyStoreException("请先选择场景");
        }
        String newName = JOptionPane.showInputDialog(this, "新名称：", oldName);
        if (newName == null || newName.trim().isEmpty() || newName.trim().equals(oldName)) {
            return;
        }
        AppContext.keyStore.renameScenario(oldName, newName);
        refreshTree();
    }

    private void deleteScenario() throws KeyStoreException {
        String name = selectedScenario();
        if (name == null) {
            throw new KeyStoreException("请先选择场景");
        }
        if (!SwingUtils.confirm(this, "确定删除场景「" + name + "」？")) {
            return;
        }
        AppContext.keyStore.deleteScenario(name);
        refreshTree();
    }

    private void exportBackup() throws Exception {
        if (!AppContext.keyStore.isUnlocked()) {
            throw new KeyStoreException("请先解锁密钥库");
        }
        char[] pwd = askPassword("设置备份密码（用于加密备份文件）", true);
        if (pwd == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出备份");
        chooser.setSelectedFile(new File("keystore-backup.dat"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            AppContext.keyStore.exportBackup(chooser.getSelectedFile().toPath(), pwd);
            SwingUtils.info(this, "备份导出成功");
        }
    }

    private void importBackup() throws Exception {
        if (!AppContext.keyStore.isUnlocked()) {
            throw new KeyStoreException("请先解锁密钥库");
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择备份文件");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        char[] pwd = askPassword("输入备份密码", false);
        if (pwd == null) {
            return;
        }
        int added = AppContext.keyStore.importBackup(chooser.getSelectedFile().toPath(), pwd);
        refreshTree();
        SwingUtils.info(this, "备份导入完成，新增 " + added + " 个密钥（已存在的跳过）");
    }

    private char[] askPassword(String title, boolean confirm) {
        JPanel panel = new JPanel(new GridBagLayout());
        JPasswordField pwd = new JPasswordField(20);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(pwd, gbc);
        JPasswordField pwd2 = null;
        if (confirm) {
            pwd2 = new JPasswordField(20);
            gbc.gridy = 1;
            panel.add(pwd2, gbc);
        }
        while (true) {
            int option = JOptionPane.showConfirmDialog(this, panel, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            if (pwd.getPassword().length == 0) {
                SwingUtils.error(this, "密码不能为空");
                continue;
            }
            if (confirm && !java.util.Arrays.equals(pwd.getPassword(), pwd2.getPassword())) {
                SwingUtils.error(this, "两次输入的密码不一致");
                continue;
            }
            return pwd.getPassword();
        }
    }
}
