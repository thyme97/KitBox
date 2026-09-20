package com.kitbox.ui;

import com.kitbox.AppContext;
import com.kitbox.ui.panel.AsymmetricPanel;
import com.kitbox.ui.panel.ConvertPanel;
import com.kitbox.ui.panel.DigestPanel;
import com.kitbox.ui.panel.EncodePanel;
import com.kitbox.ui.panel.FileChecksumPanel;
import com.kitbox.ui.panel.JsonFieldPanel;
import com.kitbox.ui.panel.JsonToolsPanel;
import com.kitbox.ui.panel.KeyManagerPanel;
import com.kitbox.ui.panel.MessageFormatPanel;
import com.kitbox.ui.panel.PasswordGenPanel;
import com.kitbox.ui.panel.QRPanel;
import com.kitbox.ui.panel.SettingsPanel;
import com.kitbox.ui.panel.SignaturePanel;
import com.kitbox.ui.panel.SymmetricPanel;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主窗口：左侧导航 + 右侧功能卡片。
 */
public class MainWindow extends JFrame {

    /** 左侧导航分组：组名 -> 工具列表。 */
    private static final Map<String, String[]> NAV_GROUPS = new LinkedHashMap<>();

    static {
        NAV_GROUPS.put("加密安全", new String[]{
                "对称加解密", "非对称加解密", "加签 / 验签", "摘要与 HMAC",
                "JSON 字段加解密", "报文格式加解密", "密钥库"});
        NAV_GROUPS.put("编码与格式", new String[]{
                "编码转换", "JSON 工具", "转换工具", "二维码工具"});
        NAV_GROUPS.put("校验与生成", new String[]{
                "文件批量校验", "密码生成器"});
        NAV_GROUPS.put("设置", new String[]{"设置"});
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel contentPanel = new JPanel(cards);
    private final Map<String, JPanel> panels = new LinkedHashMap<>();

    public MainWindow() {
        super("KitBox 工具箱");
        applyWindowIcons();

        for (String[] tools : NAV_GROUPS.values()) {
            for (String tool : tools) {
                panels.put(tool, createPanel(tool));
            }
        }
        for (Map.Entry<String, JPanel> e : panels.entrySet()) {
            contentPanel.add(e.getValue(), e.getKey());
        }

        JPanel nav = buildNav();
        nav.setPreferredSize(new Dimension(170, 0));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(nav, BorderLayout.WEST);
        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AppContext.config.setWindowWidth(getWidth());
                AppContext.config.setWindowHeight(getHeight());
                AppContext.saveConfig();
                System.exit(0);
            }
        });
    }

    /** 用 resources/icon/ 下的 PNG 设置窗口与任务栏图标；缺失时保持默认。 */
    private void applyWindowIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            URL url = MainWindow.class.getResource("/icon/icon" + size + ".png");
            if (url != null) {
                icons.add(Toolkit.getDefaultToolkit().getImage(url));
            }
        }
        if (!icons.isEmpty()) {
            setIconImages(icons);
        }
    }

    private JPanel buildNav() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("KitBox");
        for (Map.Entry<String, String[]> group : NAV_GROUPS.entrySet()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group.getKey());
            for (String tool : group.getValue()) {
                groupNode.add(new DefaultMutableTreeNode(tool));
            }
            root.add(groupNode);
        }
        JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(26);
        tree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                                   boolean expanded, boolean leaf, int row,
                                                                   boolean focus) {
                java.awt.Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);
                if (!leaf && c instanceof javax.swing.JLabel) {
                    // 分组节点加粗，突出层级
                    javax.swing.JLabel label = (javax.swing.JLabel) c;
                    label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
                }
                return c;
            }
        });
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        tree.addTreeSelectionListener(e -> {
            Object node = tree.getLastSelectedPathComponent();
            if (node != null && tree.getModel().isLeaf(node)) {
                showCard(node.toString());
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));
        panel.add(new JScrollPane(tree), BorderLayout.CENTER);
        tree.setSelectionRow(1);
        return panel;
    }

    private JPanel createPanel(String name) {
        switch (name) {
            case "对称加解密":
                return new SymmetricPanel();
            case "非对称加解密":
                return new AsymmetricPanel();
            case "加签 / 验签":
                return new SignaturePanel();
            case "摘要与 HMAC":
                return new DigestPanel();
            case "JSON 字段加解密":
                return new JsonFieldPanel();
            case "报文格式加解密":
                return new MessageFormatPanel();
            case "密钥库":
                return new KeyManagerPanel();
            case "编码转换":
                return new EncodePanel();
            case "JSON 工具":
                return new JsonToolsPanel();
            case "转换工具":
                return new ConvertPanel();
            case "二维码工具":
                return new QRPanel();
            case "文件批量校验":
                return new FileChecksumPanel();
            case "密码生成器":
                return new PasswordGenPanel();
            case "设置":
                return new SettingsPanel();
            default:
                throw new IllegalArgumentException("未知面板：" + name);
        }
    }

    private void showCard(String name) {
        if (name == null) {
            return;
        }
        cards.show(contentPanel, name);
        JPanel panel = panels.get(name);
        if (panel instanceof KeyManagerPanel) {
            ((KeyManagerPanel) panel).refreshState();
        }
    }

    /** 显示主窗口。 */
    public void display() {
        int w = Math.max(960, AppContext.config.getWindowWidth());
        int h = Math.max(640, AppContext.config.getWindowHeight());
        setSize(w, h);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            JPanel panel = panels.get("密钥库");
            if (panel instanceof KeyManagerPanel) {
                ((KeyManagerPanel) panel).refreshState();
            }
        });
    }
}
