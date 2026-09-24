package com.kitbox.ui;

import com.kitbox.AppContext;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 主页：启动器风格。居中品牌标题 + 工具搜索框（结果列表与胶囊同位互换、Ctrl+K 唤起）、
 * 常用工具胶囊，右下角「最近使用」（Ctrl+H 打开最近一个），底部含每日推荐与统计提示。
 */
public class HomePanel extends JPanel {

    /** 快捷胶囊展示的常用工具。 */
    private static final String[] QUICK_TOOLS = {
            "对称加解密", "编码转换", "JSON 工具", "转换工具", "密码生成器", "二维码工具"};

    /** 底部「今日推荐」候选，按日期轮换。 */
    private static final String[] DAILY_PICKS = {
            "对称加解密", "假数据生成", "二维码工具", "密码生成器",
            "文件批量校验", "JSON 工具", "编码转换", "转换工具"};

    /** 英文关键词补充索引：搜索 "qr"、"aes" 等英文词也能命中。 */
    private static final Map<String, String> KEYWORDS = new LinkedHashMap<>();

    /** 搜索框占位词，每 3 秒轮换。 */
    private static final String[] PLACEHOLDERS = {
            "搜索工具…（Ctrl+K）",
            "试试输入 \"base64\"",
            "试试输入 \"加密\"",
            "试试输入 \"时间戳\"",
            "试试输入 \"sm4\"",
            "试试输入 \"二维码\"",
    };

    /** 搜索结果最多同时可见的行数。 */
    private static final int MAX_RESULT_ROWS = 8;
    /** 搜索结果行高。 */
    private static final int RESULT_ROW_HEIGHT = 28;

    static {
        KEYWORDS.put("对称加解密", "aes des 3des sm4 jasypt 文件加密");
        KEYWORDS.put("非对称加解密", "rsa sm2 公钥 私钥");
        KEYWORDS.put("加签 / 验签", "sign signature 签名");
        KEYWORDS.put("摘要与 HMAC", "md5 sha1 sha256 sm3 digest");
        KEYWORDS.put("JSON 字段加解密", "字段 路径 gcm");
        KEYWORDS.put("报文格式加解密", "报文 模板 iso8583");
        KEYWORDS.put("密钥库", "keystore key secret 密钥管理");
        KEYWORDS.put("编码转换", "base64 hex url unicode");
        KEYWORDS.put("文件 Base64", "data uri 图片编码");
        KEYWORDS.put("JSON 工具", "format 校验 压缩 转义");
        KEYWORDS.put("转换工具", "timestamp uuid snowflake 雪花 进制");
        KEYWORDS.put("二维码工具", "qr code scan 扫码");
        KEYWORDS.put("文件批量校验", "checksum 校验清单");
        KEYWORDS.put("密码生成器", "password random 随机");
        KEYWORDS.put("假数据生成", "fake mock 测试数据 身份证 手机号");
    }

    private final Consumer<String> onOpen;
    private final Map<String, String> subtitles;
    /** 全部工具名（按分组顺序）：搜索数据源与底部统计。 */
    private final List<String> tools = new ArrayList<>();

    private JTextField searchField;
    private JPanel centerPanel;
    /** 搜索结果容器：与胶囊标签同位互换，避免弹出式菜单抢焦点。 */
    private JPanel resultsWrapper;
    private JScrollPane resultsScroll;
    private JPanel chipsRow;
    private JList<String> resultList;
    private DefaultListModel<String> resultModel;
    private JButton recentButton;
    private JPopupMenu recentPopup;
    private JLabel brandName;
    private JLabel dailyLabel;
    private JLabel dailyNameLabel;
    private final List<JLabel> grayLabels = new ArrayList<>();
    private final List<JButton> chipButtons = new ArrayList<>();
    private int placeholderIndex;

    public HomePanel(Map<String, String[]> groups, Map<String, String> subtitles, Consumer<String> onOpen) {
        super(new BorderLayout());
        this.onOpen = onOpen;
        this.subtitles = subtitles;
        for (String[] names : groups.values()) {
            for (String name : names) {
                tools.add(name);
            }
        }

        setBorder(BorderFactory.createEmptyBorder(24, 24, 12, 24));
        add(buildCenter(), BorderLayout.CENTER);
        add(buildSouth(), BorderLayout.SOUTH);
        installSearch();
        applyPalette();
        // 占位词每 3 秒轮换（输入中时 FlatLaf 不显示占位词，轮换本身无副作用）
        int delayMs = 3000;
        new Timer(delayMs, e -> rotatePlaceholder()).start();
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.NONE;

        // 品牌行：图标 + 名称
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        brand.setOpaque(false);
        JLabel brandIcon = new JLabel();
        URL logo = HomePanel.class.getResource("/icon/icon48.png");
        if (logo != null) {
            ImageIcon image = new ImageIcon(logo);
            brandIcon.setIcon(new ImageIcon(
                    image.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        }
        brandName = new JLabel("KitBox 工具箱");
        brandName.setFont(brandName.getFont().deriveFont(Font.BOLD, 24f));
        brand.add(brandIcon);
        brand.add(brandName);
        gc.insets = new Insets(0, 0, 0, 0);
        center.add(brand, gc);
        gc.gridy++;

        // 搜索框：胶囊造型 + 微阴影浮起；焦点环用细描边避免大色块
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.leadingIcon", searchIcon(16));
        searchField.putClientProperty("FlatLaf.style", "arc: 999; focusWidth: 1");
        searchField.putClientProperty("JTextField.placeholderText", PLACEHOLDERS[0]);
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN,
                searchField.getFont().getSize2D() + 2f));
        searchField.setPreferredSize(new Dimension(430, 44));
        searchField.setMargin(new Insets(0, 16, 0, 16));
        JPanel searchHolder = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                // 用多层逐层缩小的半透明圆角矩形模拟 0 2px 6px 微阴影
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int margin = 8;
                int[] alphas = UiTheme.isDarkTheme() ? new int[]{3, 5, 7} : new int[]{1, 2, 3};
                for (int i = 0; i < alphas.length; i++) {
                    int grow = (alphas.length - i) * 2;
                    g.setColor(new Color(0, 0, 0, alphas[i]));
                    g.fillRoundRect(margin - grow, margin - grow + 2,
                            getWidth() - 2 * (margin - grow), getHeight() - 2 * (margin - grow) + 2,
                            88 + grow * 2, 88 + grow * 2);
                }
                g.dispose();
            }
        };
        searchHolder.setOpaque(false);
        searchHolder.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));
        searchHolder.add(searchField, BorderLayout.CENTER);
        gc.insets = new Insets(26, 0, 0, 0);
        center.add(searchHolder, gc);
        gc.gridy++;

        // 搜索结果行：与下方胶囊同位互换（输入时显示结果、清空后还原胶囊），
        // 不用 JPopupMenu——FlatLaf 的重量级弹窗会抢走输入框焦点导致无法继续输入
        resultsWrapper = new JPanel(new BorderLayout());
        resultsWrapper.setOpaque(false);
        resultsWrapper.setVisible(false);
        resultsWrapper.setPreferredSize(new Dimension(0, 0));
        gc.insets = new Insets(8, 0, 0, 0);
        center.add(resultsWrapper, gc);
        gc.gridy++;

        // 常用工具胶囊
        chipsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        chipsRow.setOpaque(false);
        for (String tool : QUICK_TOOLS) {
            chipsRow.add(chip(tool));
        }
        gc.insets = new Insets(28, 0, 0, 0);
        center.add(chipsRow, gc);
        gc.gridy++;

        centerPanel = center;
        return center;
    }

    private JButton chip(String tool) {
        JButton button = new JButton(tool);
        Icon icon = ToolIcons.of(tool);
        if (icon != null) {
            button.setIcon(icon);
            button.setIconTextGap(6);
        }
        button.setFont(button.getFont().deriveFont(Font.PLAIN, 13f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 12, 8, 12));
        button.setToolTipText(subtitles.getOrDefault(tool, ""));
        button.addActionListener(e -> open(tool));
        chipButtons.add(button);
        return button;
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new GridBagLayout());
        south.setOpaque(false);
        GridBagConstraints sc = new GridBagConstraints();
        sc.gridx = 0;
        sc.gridy = 0;
        sc.weightx = 1;
        sc.fill = GridBagConstraints.NONE;
        sc.anchor = GridBagConstraints.SOUTHEAST;
        sc.insets = new Insets(0, 0, 12, 12);
        recentPopup = new JPopupMenu();
        recentButton = new JButton(historyIcon(16));
        SwingUtils.styleToolbar(recentButton);
        recentButton.setToolTipText("最近使用（Ctrl+H 打开最近一个）");
        recentButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recentButton.addActionListener(e -> showRecentPopup());
        south.add(recentButton, sc);
        sc.gridy = 1;
        sc.anchor = GridBagConstraints.CENTER;
        sc.insets = new Insets(0, 0, 0, 0);

        // 底部状态栏：搜索提示 + 今日推荐（按日期轮换，可点击直达）+ 工具总数
        JPanel hintRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        hintRow.setOpaque(false);
        hintRow.add(grayHint("按 Ctrl+K 唤起搜索"));
        hintRow.add(grayHint("·"));
        dailyLabel = grayHint("今日推荐");
        attachDailyClick(dailyLabel);
        hintRow.add(dailyLabel);
        hintRow.add(grayHint(":"));
        dailyNameLabel = smallHint(DAILY_PICKS[dailyPick()]);
        attachDailyClick(dailyNameLabel);
        hintRow.add(dailyNameLabel);
        hintRow.add(grayHint("·"));
        hintRow.add(grayHint("共 " + tools.size() + " 个工具"));
        south.add(hintRow, sc);
        return south;
    }

    /** 点历史按钮弹出最近使用列表；无记录时展示灰色占位。 */
    private void showRecentPopup() {
        recentPopup.removeAll();
        List<String> valid = new ArrayList<>();
        for (String name : AppContext.config.getRecentTools()) {
            if (tools.contains(name) && !valid.contains(name)) {
                valid.add(name);
            }
        }
        if (valid.isEmpty()) {
            javax.swing.JMenuItem empty = new javax.swing.JMenuItem("暂无最近使用");
            empty.setEnabled(false);
            recentPopup.add(empty);
        } else {
            for (String name : valid) {
                javax.swing.JMenuItem item = new javax.swing.JMenuItem(name, ToolIcons.of(name));
                item.addActionListener(e -> open(name));
                recentPopup.add(item);
            }
        }
        // 在按钮上方弹出（按钮贴着窗口底部）
        int height = recentPopup.getPreferredSize().height;
        recentPopup.show(recentButton, 0, -height - 6);
    }

    private void attachDailyClick(JLabel label) {
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Font base = label.getFont();
        Map<java.awt.font.TextAttribute, Integer> underline = new java.util.HashMap<>();
        underline.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                label.setFont(base);
                open(DAILY_PICKS[dailyPick()]);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setFont(base.deriveFont(underline));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setFont(base);
            }
        });
    }

    private static int dailyPick() {
        return LocalDate.now().getDayOfYear() % DAILY_PICKS.length;
    }

    private void installSearch() {
        resultModel = new DefaultListModel<>();
        resultList = new JList<>(resultModel);
        resultList.setFixedCellHeight(RESULT_ROW_HEIGHT);
        resultList.setFocusable(false);
        resultList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focus);
                String tool = String.valueOf(value);
                String desc = subtitles.getOrDefault(tool, "");
                label.setText(desc.isEmpty() ? tool : tool + "  —  " + abbreviate(desc, 30));
                label.setIcon(ToolIcons.of(tool));
                label.setIconTextGap(8);
                label.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                return label;
            }
        });
        resultList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                String selected = resultList.getSelectedValue();
                if (selected != null) {
                    open(selected);
                }
            }
        });
        resultList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = resultList.locationToIndex(e.getPoint());
                if (index >= 0 && index != resultList.getSelectedIndex()) {
                    resultList.setSelectedIndex(index);
                }
            }
        });

        resultsScroll = new JScrollPane(resultList);
        resultsScroll.setBorder(null);
        resultsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultsWrapper.add(resultsScroll, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateResults();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateResults();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateResults();
            }
        });

        InputMap im = searchField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = searchField.getActionMap();
        im.put(KeyStroke.getKeyStroke("DOWN"), "resultDown");
        am.put("resultDown", moveSelection(1));
        im.put(KeyStroke.getKeyStroke("UP"), "resultUp");
        am.put("resultUp", moveSelection(-1));
        im.put(KeyStroke.getKeyStroke("ENTER"), "resultOpen");
        am.put("resultOpen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (resultsWrapper.isVisible()) {
                    String selected = resultList.getSelectedValue();
                    if (selected != null) {
                        open(selected);
                    }
                }
            }
        });
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "resultEscape");
        am.put("resultEscape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
            }
        });
    }

    private AbstractAction moveSelection(int delta) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!resultsWrapper.isVisible() || resultModel.isEmpty()) {
                    return;
                }
                int size = resultModel.getSize();
                int index = resultList.getSelectedIndex() + delta;
                if (index < 0) {
                    index = size - 1;
                } else if (index >= size) {
                    index = 0;
                }
                resultList.setSelectedIndex(index);
                resultList.ensureIndexIsVisible(index);
            }
        };
    }

    /** 依据输入刷新结果：有匹配时结果列表与胶囊同位互换，无匹配或为空还原胶囊。 */
    private void updateResults() {
        String query = searchField.getText().trim().toLowerCase();
        resultModel.clear();
        if (!query.isEmpty()) {
            for (String tool : tools) {
                if (matches(tool, query)) {
                    resultModel.addElement(tool);
                }
            }
        }
        boolean showResults = !resultModel.isEmpty();
        if (showResults) {
            resultList.setSelectedIndex(0);
            int rows = Math.min(resultModel.getSize(), MAX_RESULT_ROWS);
            resultsScroll.setPreferredSize(new Dimension(
                    searchField.getWidth(), rows * RESULT_ROW_HEIGHT + 6));
        }
        setRowVisible(resultsWrapper, showResults);
        setRowVisible(chipsRow, !showResults);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    /** 显示/隐藏布局行；隐藏时高度归零，避免 GridBag 仍保留占位。 */
    private static void setRowVisible(JComponent row, boolean visible) {
        row.setVisible(visible);
        row.setPreferredSize(visible ? null : new Dimension(0, 0));
    }

    private boolean matches(String tool, String query) {
        if (tool.toLowerCase().contains(query)) {
            return true;
        }
        String subtitle = subtitles.getOrDefault(tool, "");
        if (subtitle.toLowerCase().contains(query)) {
            return true;
        }
        return KEYWORDS.getOrDefault(tool, "").toLowerCase().contains(query);
    }

    private void open(String tool) {
        searchField.setText("");
        onOpen.accept(tool);
    }

    private void rotatePlaceholder() {
        placeholderIndex = (placeholderIndex + 1) % PLACEHOLDERS.length;
        searchField.putClientProperty("JTextField.placeholderText", PLACEHOLDERS[placeholderIndex]);
    }

    /** Ctrl+K：唤起搜索框（MainWindow 全局绑定后调用）。 */
    public void focusSearch() {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /** 主题切换后重设品牌、胶囊、最近使用与提示文字的颜色。 */
    public void refreshTheme() {
        applyPalette();
        repaint();
    }

    /** 按当前主题应用首页配色；构造与主题切换时各调用一次。 */
    private void applyPalette() {
        brandName.setForeground(inkColor());
        for (JButton chip : chipButtons) {
            chip.setForeground(inkColor());
            styleChip(chip);
        }
        // 先统一灰字，再覆盖「今日推荐」工具名的强调色，避免被灰字循环覆盖
        Color gray = grayColor();
        for (JLabel label : grayLabels) {
            label.setForeground(gray);
        }
        dailyNameLabel.setForeground(accentColor());
        revalidate();
        repaint();
    }

    private void styleChip(JButton chip) {
        // 无边框实底胶囊：底色比页面深一档成形，悬停变白“点亮”，避免细边框显得又淡又碎
        chip.putClientProperty("FlatLaf.style", "arc: 999; borderWidth: 0; background: " + hex(chipBgColor())
                + "; hoverBackground: " + hex(chipHoverColor())
                + "; pressedBackground: " + hex(chipPressedColor())
                + "; focusedBackground: " + hex(chipBgColor()));
    }

    /** 底部提示行的灰色小字。 */
    private JLabel grayHint(String text) {
        JLabel label = smallHint(text);
        label.setForeground(grayColor());
        grayLabels.add(label);
        return label;
    }

    /** 底部提示行的小字（颜色由调用方决定，不参与灰字统一）。 */
    private JLabel smallHint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, label.getFont().getSize2D() - 1f));
        return label;
    }

    /** 深灰主文字：浅色 #1F2328，深色主题用亮灰替代。 */
    private static Color inkColor() {
        return UiTheme.isDarkTheme() ? new Color(0xE8EAED) : new Color(0x1F2328);
    }

    private static Color chipBgColor() {
        // FlatDarkLaf 页面底约 #3C3F41，胶囊需比它亮一档才可见
        return UiTheme.isDarkTheme() ? new Color(0x45484F) : new Color(0xE4E7EB);
    }

    private static Color chipHoverColor() {
        return UiTheme.isDarkTheme() ? new Color(0x52565E) : Color.WHITE;
    }

    private static Color chipPressedColor() {
        return UiTheme.isDarkTheme() ? new Color(0x3A3D43) : new Color(0xDCDFE4);
    }

    private static Color accentColor() {
        return new Color(0x007AFF);
    }

    private static Color grayColor() {
        Color color = javax.swing.UIManager.getColor("Label.disabledForeground");
        return color != null ? color : Color.GRAY;
    }

    private static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    /** 线条风格放大镜图标，颜色随主题前景色。 */
    private static Icon searchIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                Color fg = javax.swing.UIManager.getColor("Label.foreground");
                g.setColor(fg != null ? fg : Color.DARK_GRAY);
                g.setStroke(new java.awt.BasicStroke(1.4f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g.translate(x, y);
                g.drawOval(2, 2, 8, 8);
                g.drawLine(10, 10, 14, 14);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    /** 线条风格时钟图标（最近使用按钮），颜色随主题前景色。 */
    private static Icon historyIcon(int size) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                Color fg = javax.swing.UIManager.getColor("Label.foreground");
                g.setColor(fg != null ? fg : Color.DARK_GRAY);
                g.setStroke(new java.awt.BasicStroke(1.4f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                g.translate(x, y);
                g.drawOval(2, 2, 12, 12);
                g.drawLine(8, 5, 8, 8);
                g.drawLine(8, 8, 11, 9);
                g.dispose();
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        };
    }

    private static String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
