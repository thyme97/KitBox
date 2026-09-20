package com.kitbox.ui.panel;

import com.kitbox.AppContext;
import com.kitbox.config.MessageTemplate;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.UiTheme;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * 设置面板：主题、字体、默认参数、报文格式模板管理。
 */
public class SettingsPanel extends JPanel {

    private final JComboBox<String> themeCombo = new JComboBox<>(new String[]{"亮色", "暗色"});
    private final JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(13, 11, 20, 1));
    private final JComboBox<DataEncoding> defaultEncodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    private final JCheckBox uppercaseCheck = new JCheckBox("摘要输出默认大写");
    private final JCheckBox autoCopyCheck = new JCheckBox("操作成功后自动复制结果");
    private final JCheckBox prettyJsonCheck = new JCheckBox("JSON 处理结果格式化输出");
    private final JTextField dataDirField = new JTextField(28);

    private final DefaultTableModel templateModel = new DefaultTableModel(
            new Object[]{"模板名称", "前缀", "后缀", "内容编码"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable templateTable = new JTable(templateModel);

    public SettingsPanel() {
        setLayout(new BorderLayout());

        FormPanel form = new FormPanel();
        form.addField("主题：", rowOf(themeCombo, applyThemeButton()));
        form.addField("字体大小：", rowOf(fontSizeSpinner, applyFontButton()));
        form.addField("默认密文输出编码：", defaultEncodingCombo);
        dataDirField.setEditable(false);
        dataDirField.setText(com.kitbox.AppContext.dataDir.toString());
        JButton changeDir = new JButton("更改存储目录…");
        changeDir.addActionListener(e -> changeDataDir());
        form.addField("存储位置：", rowOf(dataDirField, changeDir));
        form.addFull(uppercaseCheck);
        form.addFull(autoCopyCheck);
        form.addFull(prettyJsonCheck);

        JPanel templatePanel = new JPanel(new BorderLayout());
        templatePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("报文格式模板（报文格式加解密面板使用）"));
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templatePanel.add(new JScrollPane(templateTable), BorderLayout.CENTER);
        JPanel templateButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton add = new JButton("新增");
        JButton edit = new JButton("编辑");
        JButton remove = new JButton("删除");
        templateButtons.add(add);
        templateButtons.add(edit);
        templateButtons.add(remove);
        templatePanel.add(templateButtons, BorderLayout.SOUTH);

        JLabel about = new JLabel("<html><div style='margin:8px'>"
                + "<b>KitBox 工具箱 v1.1.0</b><br>"
                + "本地离线工具，配置保存在 ~/.kitbox/config.json，密钥库保存在 ~/.kitbox/keystore.dat（设密码时加密，无密码时明文）。<br>"
                + "基于 JDK 内置 JCE + BouncyCastle（国密）+ ZXing + FlatLaf。</div></html>");

        JPanel center = new JPanel(new BorderLayout());
        center.add(templatePanel, BorderLayout.CENTER);

        add(form, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(about, BorderLayout.SOUTH);

        // 初始化 UI 状态
        themeCombo.setSelectedIndex("dark".equals(AppContext.config.getTheme()) ? 1 : 0);
        fontSizeSpinner.setValue(AppContext.config.getFontSize());
        try {
            defaultEncodingCombo.setSelectedItem(
                    DataEncoding.valueOf(AppContext.config.getDefaultOutputEncoding()));
        } catch (Exception ignored) {
        }
        uppercaseCheck.setSelected(AppContext.config.isDigestUppercase());
        autoCopyCheck.setSelected(AppContext.config.isAutoCopyResult());
        prettyJsonCheck.setSelected(AppContext.config.isPrettyJson());

        // 保存行为
        uppercaseCheck.addActionListener(e -> {
            AppContext.config.setDigestUppercase(uppercaseCheck.isSelected());
            AppContext.saveConfig();
        });
        autoCopyCheck.addActionListener(e -> {
            AppContext.config.setAutoCopyResult(autoCopyCheck.isSelected());
            AppContext.saveConfig();
        });
        prettyJsonCheck.addActionListener(e -> {
            AppContext.config.setPrettyJson(prettyJsonCheck.isSelected());
            AppContext.saveConfig();
        });
        defaultEncodingCombo.addActionListener(e -> {
            AppContext.config.setDefaultOutputEncoding(
                    String.valueOf(defaultEncodingCombo.getSelectedItem()));
            AppContext.saveConfig();
        });
        add.addActionListener(e -> editTemplate(null));
        edit.addActionListener(e -> {
            int row = templateTable.getSelectedRow();
            if (row < 0) {
                SwingUtils.info(this, "请先选择模板");
                return;
            }
            editTemplate(AppContext.config.getTemplates().get(row));
        });
        remove.addActionListener(e -> {
            int row = templateTable.getSelectedRow();
            if (row < 0) {
                SwingUtils.info(this, "请先选择模板");
                return;
            }
            if (SwingUtils.confirm(this, "确定删除所选模板？")) {
                AppContext.config.getTemplates().remove(row);
                AppContext.saveConfig();
                refreshTemplates();
            }
        });
        refreshTemplates();
    }

    private JPanel rowOf(javax.swing.JComponent a, javax.swing.JComponent b) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(a);
        row.add(b);
        return row;
    }

    private JButton applyThemeButton() {
        JButton button = new JButton("应用");
        button.addActionListener(e -> {
            AppContext.config.setTheme(themeCombo.getSelectedIndex() == 1 ? "dark" : "light");
            AppContext.saveConfig();
            UiTheme.apply(AppContext.config);
            Window window = windowAncestorOf();
            if (window != null) {
                javax.swing.SwingUtilities.updateComponentTreeUI(window);
            }
        });
        return button;
    }

    private JButton applyFontButton() {
        JButton button = new JButton("应用");
        button.addActionListener(e -> {
            AppContext.config.setFontSize((Integer) fontSizeSpinner.getValue());
            AppContext.saveConfig();
            UiTheme.apply(AppContext.config);
            Window window = windowAncestorOf();
            if (window != null) {
                javax.swing.SwingUtilities.updateComponentTreeUI(window);
            }
        });
        return button;
    }

    private void changeDataDir() {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(
                com.kitbox.AppContext.dataDir.toFile());
        chooser.setDialogTitle("选择新的存储目录（配置与密钥库将迁移到该目录）");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File dir = chooser.getSelectedFile();
        if (dir == null) {
            return;
        }
        try {
            java.nio.file.Path target = dir.toPath().toAbsolutePath();
            if (target.equals(com.kitbox.AppContext.dataDir)) {
                SwingUtils.info(this, "选择的目录就是当前存储目录");
                return;
            }
            String tip = "<html>将把配置与密钥库迁移到：<br><b>" + target
                    + "</b><br><br>迁移立即执行，<font color='#C62828'>重启应用后生效</font>。继续？</html>";
            if (!SwingUtils.confirm(this, tip)) {
                return;
            }
            int copied = com.kitbox.AppContext.changeDataDir(target);
            dataDirField.setText(target.toString());
            SwingUtils.info(this, "迁移完成（复制 " + copied + " 个文件）。重启应用后生效。");
        } catch (Exception e) {
            SwingUtils.error(this, "迁移失败：" + e.getMessage());
        }
    }

    private Window windowAncestorOf() {
        return javax.swing.SwingUtilities.getWindowAncestor(this);
    }

    private void refreshTemplates() {
        templateModel.setRowCount(0);
        for (MessageTemplate t : AppContext.config.getTemplates()) {
            templateModel.addRow(new Object[]{t.getName(), t.getPrefix(), t.getSuffix(), t.getContentEncoding()});
        }
    }

    private void editTemplate(MessageTemplate existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.getName(), 14);
        JTextField prefixField = new JTextField(existing == null ? "" : existing.getPrefix(), 14);
        JTextField suffixField = new JTextField(existing == null ? "" : existing.getSuffix(), 14);
        JComboBox<DataEncoding> encodingCombo = new JComboBox<>(new DataEncoding[]{
                DataEncoding.BASE64, DataEncoding.HEX});
        if (existing != null) {
            try {
                encodingCombo.setSelectedItem(DataEncoding.valueOf(existing.getContentEncoding()));
            } catch (Exception ignored) {
            }
        }
        nameField.setEditable(existing == null);

        JPanel panel = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(4, 8, 4, 8);
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("名称："), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("前缀："), gbc);
        gbc.gridx = 1;
        panel.add(prefixField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("后缀："), gbc);
        gbc.gridx = 1;
        panel.add(suffixField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("内容编码："), gbc);
        gbc.gridx = 1;
        panel.add(encodingCombo, gbc);

        while (true) {
            int option = JOptionPane.showConfirmDialog(this, panel, existing == null ? "新增模板" : "编辑模板",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return;
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                SwingUtils.error(this, "名称不能为空");
                continue;
            }
            if (existing == null && AppContext.config.getTemplates().stream()
                    .anyMatch(t -> t.getName().equals(name))) {
                SwingUtils.error(this, "模板已存在：" + name);
                continue;
            }
            MessageTemplate t = existing == null ? new MessageTemplate() : existing;
            t.setName(name);
            t.setPrefix(prefixField.getText());
            t.setSuffix(suffixField.getText());
            t.setContentEncoding(String.valueOf(encodingCombo.getSelectedItem()));
            if (existing == null) {
                AppContext.config.getTemplates().add(t);
            }
            AppContext.saveConfig();
            refreshTemplates();
            return;
        }
    }
}
