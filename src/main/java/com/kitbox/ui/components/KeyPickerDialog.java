package com.kitbox.ui.components;

import com.kitbox.keystore.KeyEntry;
import com.kitbox.keystore.KeyEntryType;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 从密钥库选择一个密钥条目（按场景分组展示，按用途过滤）。
 */
public final class KeyPickerDialog extends JDialog {

    private KeyEntry selected;

    private KeyPickerDialog(JFrame owner, EnumSet<KeyEntryType.Kind> kinds) {
        super(owner, "从密钥库选择", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("密钥库");
        // 场景 → 该场景下符合过滤条件的条目
        Map<String, DefaultMutableTreeNode> scenarioNodes = new LinkedHashMap<>();
        List<KeyEntry> entries = com.kitbox.AppContext.keyStore.getEntries();
        for (String scenario : com.kitbox.AppContext.keyStore.getScenarios()) {
            scenarioNodes.put(scenario, new DefaultMutableTreeNode(scenario));
            root.add(scenarioNodes.get(scenario));
        }
        int count = 0;
        for (KeyEntry entry : entries) {
            if (!kinds.contains(entry.getType().getKind())) {
                continue;
            }
            DefaultMutableTreeNode node = scenarioNodes.get(entry.getScenario());
            if (node == null) {
                node = new DefaultMutableTreeNode(entry.getScenario());
                scenarioNodes.put(entry.getScenario(), node);
                root.add(node);
            }
            node.add(new DefaultMutableTreeNode(entry));
            count++;
        }

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        add(new JScrollPane(tree), BorderLayout.CENTER);

        JLabel tip = new JLabel("  可用密钥 " + count + " 个，选择后点击确定  ");
        tip.setEnabled(false);
        add(tip, BorderLayout.NORTH);

        JPanel buttonBar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        JButton ok = new JButton("确定");
        JButton cancel = new JButton("取消");
        buttonBar.add(ok);
        buttonBar.add(cancel);
        add(buttonBar, BorderLayout.SOUTH);

        ok.addActionListener(e -> {
            Object last = tree.getLastSelectedPathComponent();
            if (last == null || !(((DefaultMutableTreeNode) last).getUserObject() instanceof KeyEntry)) {
                JOptionPane.showMessageDialog(this, "请先选择一个密钥");
                return;
            }
            selected = (KeyEntry) ((DefaultMutableTreeNode) last).getUserObject();
            dispose();
        });
        cancel.addActionListener(e -> dispose());

        setSize(380, 460);
        setLocationRelativeTo(owner);
    }

    /** 打开选择器，返回选中的条目（取消返回 null）。 */
    public static KeyEntry pick(JFrame owner, EnumSet<KeyEntryType.Kind> kinds,
                                Consumer<KeyEntry> onSelected) {
        KeyPickerDialog dialog = new KeyPickerDialog(owner, kinds);
        dialog.setVisible(true);
        if (dialog.selected != null && onSelected != null) {
            onSelected.accept(dialog.selected);
        }
        return dialog.selected;
    }
}
