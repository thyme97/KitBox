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
import com.kitbox.ui.panel.FakeDataPanel;
import com.kitbox.ui.panel.FileBase64Panel;
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
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

    /** 工具副标题：显示在内容区标题下方。 */
    private static final Map<String, String> TOOL_SUBTITLES = new LinkedHashMap<>();

    private static final Map<String, String> TOOL_HELP = new LinkedHashMap<>();

    static {
        NAV_GROUPS.put("加密安全", new String[]{
                "对称加解密", "非对称加解密", "加签 / 验签", "摘要与 HMAC",
                "JSON 字段加解密", "报文格式加解密", "密钥库"});
        NAV_GROUPS.put("编码与格式", new String[]{
                "编码转换", "文件 Base64", "JSON 工具", "转换工具", "二维码工具"});
        NAV_GROUPS.put("校验与生成", new String[]{
                "文件批量校验", "密码生成器", "假数据生成"});
        NAV_GROUPS.put("设置", new String[]{"设置"});

        TOOL_SUBTITLES.put("对称加解密", "AES / DES / 3DES / SM4 对称加解密，支持文本与文件；Jasypt 配置加密互通");
        TOOL_SUBTITLES.put("非对称加解密", "RSA / SM2 公私钥加解密，长文本自动分段");
        TOOL_SUBTITLES.put("加签 / 验签", "RSA / SM2 数字签名与验签");
        TOOL_SUBTITLES.put("摘要与 HMAC", "MD5 / SHA / SM3 摘要与 HMAC 消息认证码");
        TOOL_SUBTITLES.put("JSON 字段加解密", "按字段路径对 JSON 中的指定值加解密");
        TOOL_SUBTITLES.put("报文格式加解密", "按前后缀模板定位报文片段并加解密");
        TOOL_SUBTITLES.put("密钥库", "集中管理密钥，按场景分组，支持导入导出");
        TOOL_SUBTITLES.put("编码转换", "Base64 / Hex / URL 等常用编码互转");
        TOOL_SUBTITLES.put("文件 Base64", "文件与图片转 Base64，支持 Data URI 与互转");
        TOOL_SUBTITLES.put("JSON 工具", "格式化、压缩、校验、转义");
        TOOL_SUBTITLES.put("转换工具", "时间戳 / 进制 / UUID / 雪花 ID");
        TOOL_SUBTITLES.put("二维码工具", "生成与识别二维码");
        TOOL_SUBTITLES.put("文件批量校验", "MD5 / SHA / SM3 批量校验与校验清单");
        TOOL_SUBTITLES.put("密码生成器", "随机密码与随机密钥批量生成");
        TOOL_SUBTITLES.put("假数据生成", "身份证、手机号、姓名、邮箱、银行卡等测试数据");
        TOOL_SUBTITLES.put("设置", "主题、字体、存储位置");

        TOOL_HELP.put("对称加解密", "明文/密文按 UTF-8 处理，二进制内容格式选 Hex；密文编码用于加密输出与解密输入；密钥支持明文/Base64/Hex 或从密钥库选择。\n文件加解密输出 CBFX 自有格式（随机 IV），仅能由本工具解密。\nJasypt 配置页与 Spring 配置加密互通（ENC 值）：支持旧版默认 PBEWithMD5AndDES（盐 8 字节、口令须 ASCII）与 3.x 默认 PBEWithHmacSHA512AndAES_256（盐 16 + IV 16，PBKDF2-SHA512），迭代次数默认 1000，需与应用配置一致。IV 生成器下拉仅对旧算法生效（旧默认无独立 IV；RandomIvGenerator 为博客同款配置）；解密时自动尝试旧默认/随机 IV/内嵌 IV 三种布局并取可读性最优，无需手选。");
        TOOL_HELP.put("非对称加解密", "公钥加密、私钥解密；SM2 支持裸点公钥 04|X|Y；处理二进制数据时内容格式选 Hex。\n密钥可从密钥库选择，或点「生成密钥对…」生成后保存到密钥库；RSA 长文本按密钥长度自动分段加密。");
        TOOL_HELP.put("加签 / 验签", "SM2 签名者 ID 默认 1234567812345678，需与对方一致；验签时签名值取自结果区；对二进制数据加签时原文格式选 Hex。\nHMAC 密钥支持明文/Base64/Hex 或从密钥库选择。");
        TOOL_HELP.put("摘要与 HMAC", "勾选 HMAC 模式后需要密钥：支持明文/Base64/Hex 或从密钥库选择。\n对二进制数据计算摘要时原文格式选 Hex。");
        TOOL_HELP.put("JSON 字段加解密", "字段值 = Base64(随机IV ‖ GCM密文)；解密按同一格式解析，其余字段原样保留；密钥可从密钥库选择。");
        TOOL_HELP.put("报文格式加解密", "按模板前后缀定位报文内容并加解密；模板直接在面板内维护：选模板后修改前后缀，再以同名「保存为模板」覆盖更新。复用对称算法参数。");
        TOOL_HELP.put("密钥库", "密钥按场景分组保存；设置密码后加密存储，主密码遗忘将无法找回密钥；支持 PEM 导出与加密备份。");
        TOOL_HELP.put("编码转换", "Base64 的 URL 安全变体用 -_ 替代 +/；按行分割仅对 Base64 编码生效。");
        TOOL_HELP.put("文件 Base64", "文件/图片编码为 Base64，可勾选输出 Data URI 前缀直接用于 HTML/CSS；\n把文件拖进本页即可编码；粘贴 Base64 后内容为图片时自动预览，「另存为…」解码保存，文件名按 Data URI 类型自动推荐；文件:/// 形式的路径会自动转换。");
        TOOL_HELP.put("JSON 工具", "校验仅检查语法合法性，不产生输出；转义/去转义针对 JSON 字符串字面量。");
        TOOL_HELP.put("转换工具", "时间戳秒/毫秒自动识别，也支持日期时间；进制转换支持 2~36 与大整数。\n雪花 ID：41 位毫秒时间戳 + 5 位数据中心 + 5 位机器 + 12 位序列，纪元 1288834974657，与 MyBatis-Plus 一致；解析支持十进制与 0x 十六进制。");
        TOOL_HELP.put("二维码工具", "纠错级别越高越容易扫出，但容量越小；识别支持 PNG/JPG/BMP 等常见格式，可拖入文件或粘贴（Ctrl+V 全页可用，包括焦点在结果区时）；选择文件后立即显示预览，点击预览可放大查看。");
        TOOL_HELP.put("文件批量校验", "支持从资源管理器拖入文件；清单为 GNU 标准格式（哈希 + 文件名）；校验以清单所在目录为基准目录。");
        TOOL_HELP.put("密码生成器", "密码保证每个勾选类别至少出现一次；随机密钥使用密码学安全随机数。");
        TOOL_HELP.put("假数据生成", "所有数据均由随机算法合成，仅供开发/测试环境填充，与真实人员无关。\n身份证号为校验位合法（GB 11643 MOD 11-2）的测试号码，属地可按省/市/区指定或随机；银行卡号通过 Luhn 校验；MAC 地址带本地管理位，不会撞真实厂商。");
        TOOL_HELP.put("设置", "配置保存在 ~/.kitbox/config.json，密钥库保存在 ~/.kitbox/keystore.dat。");
    }

    private final CardLayout cards = new CardLayout();
    private final JPanel contentPanel = new JPanel(cards);
    private final Map<String, JPanel> panels = new LinkedHashMap<>();
    private javax.swing.JTree navTree;
    private JPanel navPanel;

    public MainWindow() {
        super("KitBox 工具箱");
        applyWindowIcons();

        panels.put("主页", new HomePanel(NAV_GROUPS, TOOL_SUBTITLES, this::selectTool));
        for (String[] tools : NAV_GROUPS.values()) {
            for (String tool : tools) {
                panels.put(tool, createPanel(tool));
            }
        }
        for (Map.Entry<String, JPanel> e : panels.entrySet()) {
            contentPanel.add(wrapWithHeader(e.getKey(), e.getValue()), e.getKey());
        }

        JPanel nav = buildNav();
        nav.setPreferredSize(new Dimension(170, 0));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        root.add(nav, BorderLayout.WEST);
        root.add(contentPanel, BorderLayout.CENTER);
        setContentPane(root);
        installGlobalShortcuts(root);

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
        // 主页作为首个叶子节点置于所有分组之前
        root.add(new DefaultMutableTreeNode("主页"));
        for (Map.Entry<String, String[]> group : NAV_GROUPS.entrySet()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(group.getKey());
            for (String tool : group.getValue()) {
                groupNode.add(new DefaultMutableTreeNode(tool));
            }
            root.add(groupNode);
        }
        JTree tree = new JTree(root);
        navTree = tree;
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(26);
        tree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            {
                // 隐藏默认的文件夹/文件图标，叶子节点用自绘工具图标
                setLeafIcon(null);
                setOpenIcon(null);
                setClosedIcon(null);
            }

            @Override
            public java.awt.Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                                   boolean expanded, boolean leaf, int row,
                                                                   boolean focus) {
                java.awt.Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);
                if (c instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel) c;
                    if (leaf) {
                        label.setIcon(ToolIcons.of(String.valueOf(value)));
                    } else {
                        label.setIcon(null);
                        // 分组节点加粗，突出层级
                        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
                    }
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
        navPanel = panel;
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")));

        // 侧栏品牌头
        javax.swing.JLabel brandIcon = new javax.swing.JLabel();
        java.net.URL logoUrl = MainWindow.class.getResource("/icon/icon48.png");
        if (logoUrl != null) {
            javax.swing.ImageIcon logo = new javax.swing.ImageIcon(logoUrl);
            brandIcon.setIcon(new javax.swing.ImageIcon(
                    logo.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        }
        javax.swing.JLabel brandName = new javax.swing.JLabel("KitBox 工具箱");
        brandName.setFont(brandName.getFont().deriveFont(java.awt.Font.BOLD,
                brandName.getFont().getSize2D() + 2f));
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 8));
        brand.add(brandIcon);
        brand.add(brandName);

        panel.add(brand, BorderLayout.NORTH);
        panel.add(new JScrollPane(tree), BorderLayout.CENTER);
        tree.setSelectionRow(0);
        return panel;
    }

    /** 主题切换后刷新导航分隔线等直接持有颜色的部件。 */
    public void refreshTheme() {
        if (navPanel != null) {
            navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1,
                    UIManager.getColor("Component.borderColor")));
            navPanel.repaint();
        }
        refreshThemeButtons();
        if (panels.get("主页") instanceof HomePanel) {
            ((HomePanel) panels.get("主页")).refreshTheme();
        }
    }


    /** 内容区统一加「工具标题 + 副标题」header 条，右侧放主题切换与设置入口；主页用启动器式布局，不带头部标题。 */
    private final java.util.List<javax.swing.JButton> themeButtons = new java.util.ArrayList<>();

    private JPanel wrapWithHeader(String name, JPanel panel) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        if (!"主页".equals(name)) {
            javax.swing.JLabel title = new javax.swing.JLabel(name);
            title.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, title.getFont().getSize2D() + 4f));
            javax.swing.JLabel subtitle = new javax.swing.JLabel(TOOL_SUBTITLES.getOrDefault(name, ""));
            subtitle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            Color hintColor = UIManager.getColor("Label.disabledForeground");
            subtitle.setForeground(hintColor != null ? hintColor : Color.GRAY);

            // 副标题后跟「?」帮助图标：纯 JLabel，只有悬浮提示，无按钮的按下/焦点态
            JPanel subtitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            subtitleRow.setOpaque(false);
            subtitleRow.add(subtitle);
            String help = TOOL_HELP.get(name);
            if (help != null) {
                javax.swing.Icon helpIcon = SwingUtils.svgIcon("help", 14);
                javax.swing.JLabel helpLabel = helpIcon != null
                        ? new javax.swing.JLabel(helpIcon)
                        : new javax.swing.JLabel("?");
                helpLabel.setToolTipText("<html>" + help.replace("\n", "<br>") + "</html>");
                subtitleRow.add(helpLabel);
            }

            JPanel titleBlock = new JPanel(new java.awt.GridLayout(0, 1, 0, 2));
            titleBlock.setOpaque(false);
            titleBlock.setBorder(BorderFactory.createEmptyBorder(10, 12, 4, 12));
            titleBlock.add(title);
            titleBlock.add(subtitleRow);
            header.add(titleBlock, BorderLayout.WEST);
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        javax.swing.JButton themeToggle = SwingUtils.iconButton(
                UiTheme.isDarkTheme() ? "sun" : "moon", "主题",
                UiTheme.isDarkTheme() ? "切换到亮色主题" : "切换到暗色主题");
        themeToggle.addActionListener(e -> {
            AppContext.config.setTheme(UiTheme.isDarkTheme() ? "light" : "dark");
            AppContext.saveConfig();
            UiTheme.applyAndRefresh(AppContext.config);
        });
        themeButtons.add(themeToggle);
        javax.swing.JButton settingsJump = SwingUtils.iconButton("settings", "设置", "打开设置");
        settingsJump.addActionListener(e -> selectTool("设置"));
        right.add(themeToggle);
        right.add(settingsJump);
        header.add(right, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    /** 全局快捷键：Ctrl+K 唤起主页搜索框，Ctrl+H 打开最近使用的工具。 */
    private void installGlobalShortcuts(JPanel root) {
        javax.swing.InputMap im = root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = root.getActionMap();
        im.put(javax.swing.KeyStroke.getKeyStroke("control pressed K"), "homeSearch");
        am.put("homeSearch", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selectTool("主页");
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (panels.get("主页") instanceof HomePanel) {
                        ((HomePanel) panels.get("主页")).focusSearch();
                    }
                });
            }
        });
        im.put(javax.swing.KeyStroke.getKeyStroke("control pressed H"), "openRecent");
        am.put("openRecent", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                java.util.List<String> recent = AppContext.config.getRecentTools();
                if (!recent.isEmpty() && panels.containsKey(recent.get(0))) {
                    selectTool(recent.get(0));
                }
            }
        });
    }

    /** 选中导航树中的指定工具（叶子节点）。 */
    private void selectTool(String tool) {
        if (navTree == null) {
            return;
        }
        for (int i = 0; i < navTree.getRowCount(); i++) {
            Object node = navTree.getPathForRow(i).getLastPathComponent();
            if (tool.equals(String.valueOf(node)) && navTree.getModel().isLeaf(node)) {
                navTree.setSelectionRow(i);
                return;
            }
        }
    }

    /** 主题切换后刷新 header 右侧按钮的图标与提示。 */
    private void refreshThemeButtons() {
        javax.swing.Icon icon = SwingUtils.svgIcon(UiTheme.isDarkTheme() ? "sun" : "moon", 15);
        for (javax.swing.JButton button : themeButtons) {
            if (icon != null) {
                button.setIcon(icon);
            }
            button.setToolTipText(UiTheme.isDarkTheme() ? "切换到亮色主题" : "切换到暗色主题");
        }
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
            case "文件 Base64":
                return new FileBase64Panel();
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
            case "假数据生成":
                return new FakeDataPanel();
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
        recordRecent(name);
    }

    /** 记录工具使用顺序（主页历史按钮的数据源）。 */
    private void recordRecent(String name) {
        if ("主页".equals(name)) {
            return;
        }
        java.util.List<String> recent = AppContext.config.getRecentTools();
        recent.remove(name);
        recent.add(0, name);
        while (recent.size() > 5) {
            recent.remove(recent.size() - 1);
        }
        AppContext.saveConfig();
    }

    /** 显示主窗口。 */
    public void display() {
        int w = Math.max(960, AppContext.config.getWindowWidth());
        int h = Math.max(640, AppContext.config.getWindowHeight());
        setSize(w, h);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setVisible(true);
        // 冒烟测试钩子：-Dkitbox.tool=工具名 直接选中导航项（仅匹配叶子，避免误选同名分组）
        String tool = System.getProperty("kitbox.tool");
        if (tool != null && !tool.isEmpty()) {
            selectTool(tool);
        }
        SwingUtilities.invokeLater(() -> {
            JPanel panel = panels.get("密钥库");
            if (panel instanceof KeyManagerPanel) {
                ((KeyManagerPanel) panel).refreshState();
            }
            // 首帧布局（主页描述文字换行等）可能留下旧像素条带，整块重刷一次
            contentPanel.repaint();
        });
    }
}
