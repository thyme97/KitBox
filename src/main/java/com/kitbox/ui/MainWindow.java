package com.kitbox.ui;

import com.kitbox.AppContext;
import com.kitbox.ui.panel.AsymmetricPanel;
import com.kitbox.ui.panel.DigestPanel;
import com.kitbox.ui.panel.EncodePanel;
import com.kitbox.ui.panel.JsonFieldPanel;
import com.kitbox.ui.panel.KeyManagerPanel;
import com.kitbox.ui.panel.MessageFormatPanel;
import com.kitbox.ui.panel.QRPanel;
import com.kitbox.ui.panel.SettingsPanel;
import com.kitbox.ui.panel.SignaturePanel;
import com.kitbox.ui.panel.SymmetricPanel;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主窗口：左侧导航 + 右侧功能卡片。
 */
public class MainWindow extends JFrame {

    private static final String[] NAV_ITEMS = {
            "对称加解密", "非对称加解密", "JSON 字段加解密", "报文格式加解密",
            "加签 / 验签", "摘要与 HMAC", "编码转换", "二维码工具", "密钥库", "设置"
    };

    private final CardLayout cards = new CardLayout();
    private final JPanel contentPanel = new JPanel(cards);
    private final Map<String, JPanel> panels = new LinkedHashMap<>();

    public MainWindow() {
        super("KitBox 工具箱");
        applyWindowIcons();

        panels.put(NAV_ITEMS[0], new SymmetricPanel());
        panels.put(NAV_ITEMS[1], new AsymmetricPanel());
        panels.put(NAV_ITEMS[2], new JsonFieldPanel());
        panels.put(NAV_ITEMS[3], new MessageFormatPanel());
        panels.put(NAV_ITEMS[4], new SignaturePanel());
        panels.put(NAV_ITEMS[5], new DigestPanel());
        panels.put(NAV_ITEMS[6], new EncodePanel());
        panels.put(NAV_ITEMS[7], new QRPanel());
        panels.put(NAV_ITEMS[8], new KeyManagerPanel());
        panels.put(NAV_ITEMS[9], new SettingsPanel());
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
        DefaultListModel<String> model = new DefaultListModel<>();
        for (String item : NAV_ITEMS) {
            model.addElement(item);
        }
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                Component c = super.getListCellRendererComponent(list, value, index, selected, focused);
                if (c instanceof javax.swing.JLabel) {
                    javax.swing.JLabel label = (javax.swing.JLabel) c;
                    label.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));
                    Font font = label.getFont().deriveFont(Font.PLAIN, label.getFont().getSize2D() + 1f);
                    label.setFont(font);
                }
                return c;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    showCard(list.getSelectedValue());
                }
            }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showCard(list.getSelectedValue());
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));
        panel.add(new javax.swing.JScrollPane(list), BorderLayout.CENTER);
        list.setSelectedIndex(0);
        return panel;
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
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            JPanel panel = panels.get(NAV_ITEMS[8]);
            if (panel instanceof KeyManagerPanel) {
                ((KeyManagerPanel) panel).refreshState();
            }
        });
    }
}
