package com.kitbox.ui.panel;

import com.kitbox.AppContext;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.UiTheme;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * 设置面板：外观（主题/字体）、默认行为、存储位置。
 */
public class SettingsPanel extends JPanel {

    private final JComboBox<String> themeCombo = new JComboBox<>(new String[]{"跟随系统", "亮色", "暗色"});
    private final JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(13, 11, 20, 1));
    private final JComboBox<DataEncoding> defaultEncodingCombo = new JComboBox<>(new DataEncoding[]{
            DataEncoding.BASE64, DataEncoding.HEX});
    private final JCheckBox uppercaseCheck = new JCheckBox("摘要输出默认大写");
    private final JCheckBox autoCopyCheck = new JCheckBox("操作成功后自动复制结果");
    private final JCheckBox prettyJsonCheck = new JCheckBox("JSON 处理结果格式化输出");

    public SettingsPanel() {
        super(new BorderLayout());

        // 外观
        FormPanel appearanceForm = new FormPanel();
        appearanceForm.addField("主题：", themeCombo, false);
        appearanceForm.addField("字体大小：", fontSizeSpinner, false);

        // 默认行为
        FormPanel behaviorForm = new FormPanel();
        behaviorForm.addField("默认密文输出编码：", defaultEncodingCombo, false);
        behaviorForm.addFull(uppercaseCheck);
        behaviorForm.addFull(autoCopyCheck);
        behaviorForm.addFull(prettyJsonCheck);

        // 存储
        javax.swing.JTextField dataDirField = new javax.swing.JTextField(28);
        dataDirField.setEditable(false);
        dataDirField.setText(AppContext.dataDir.toString());
        JButton changeDir = new JButton("更改存储目录…");
        changeDir.addActionListener(e -> changeDataDir());
        FormPanel storageForm = new FormPanel();
        storageForm.addField("存储位置：", rowOf(dataDirField, changeDir));

        JPanel cards = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(4, 10, 0, 10);
        gc.gridy = 0;
        cards.add(card("外观", appearanceForm), gc);
        gc.gridy = 1;
        cards.add(card("默认行为", behaviorForm), gc);
        gc.gridy = 2;
        gc.insets = new Insets(4, 10, 10, 10);
        cards.add(card("存储", storageForm), gc);

        JLabel about = new JLabel("<html><div style='margin:8px'>"
                + "<b>KitBox 工具箱 v1.1.0</b><br>"
                + "本地离线工具，配置保存在 ~/.kitbox/config.json，密钥库保存在 ~/.kitbox/keystore.dat（设密码时加密，无密码时明文）。<br>"
                + "基于 JDK 内置 JCE + BouncyCastle（国密）+ ZXing + FlatLaf。</div></html>");

        add(cards, BorderLayout.NORTH);
        add(about, BorderLayout.SOUTH);

        // 初始化 UI 状态
        themeCombo.setSelectedIndex(themeIndex(AppContext.config.getTheme()));
        fontSizeSpinner.setValue(AppContext.config.getFontSize());
        try {
            defaultEncodingCombo.setSelectedItem(
                    DataEncoding.valueOf(AppContext.config.getDefaultOutputEncoding()));
        } catch (Exception ignored) {
        }
        uppercaseCheck.setSelected(AppContext.config.isDigestUppercase());
        autoCopyCheck.setSelected(AppContext.config.isAutoCopyResult());
        prettyJsonCheck.setSelected(AppContext.config.isPrettyJson());

        // 保存行为（在初始化取值之后挂接，避免构造期间误触发应用）
        themeCombo.addActionListener(e -> {
            int index = themeCombo.getSelectedIndex();
            AppContext.config.setTheme(index == 2 ? "dark" : index == 1 ? "light" : "system");
            AppContext.saveConfig();
            UiTheme.applyAndRefresh(AppContext.config);
        });
        fontSizeSpinner.addChangeListener(e -> {
            try {
                fontSizeSpinner.commitEdit();
            } catch (java.text.ParseException ignored) {
            }
            AppContext.config.setFontSize((Integer) fontSizeSpinner.getValue());
            AppContext.saveConfig();
            UiTheme.applyAndRefresh(AppContext.config);
        });
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
    }

    private JPanel card(String title, javax.swing.JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(SwingUtils.cardBorder(title));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel rowOf(javax.swing.JComponent a, javax.swing.JComponent b) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(a);
        row.add(b);
        return row;
    }

    private static int themeIndex(String theme) {
        if ("dark".equals(theme)) {
            return 2;
        }
        if ("light".equals(theme)) {
            return 1;
        }
        return 0;
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
            SwingUtils.info(this, "迁移完成（复制 " + copied + " 个文件）。重启应用后生效。");
        } catch (Exception e) {
            SwingUtils.error(this, "迁移失败：" + e.getMessage());
        }
    }
}
