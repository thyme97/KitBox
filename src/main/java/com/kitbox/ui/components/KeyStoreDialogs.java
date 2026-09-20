package com.kitbox.ui.components;

import com.kitbox.AppContext;
import com.kitbox.keystore.KeyStoreException;
import com.kitbox.keystore.KeyStoreManager;
import com.kitbox.ui.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

/**
 * 密钥库初始化 / 解锁 / 修改密码对话框。
 * 密码可留空 = 无密码模式（密钥库明文保存，界面会再次确认）。
 */
public final class KeyStoreDialogs {

    private KeyStoreDialogs() {
    }

    /** 确保密钥库已解锁；不存在则引导初始化；无密码库自动加载。返回是否已解锁。 */
    public static boolean ensureUnlocked(Component parent) {
        KeyStoreManager ks = AppContext.keyStore;
        if (ks.isUnlocked()) {
            return true;
        }
        if (!ks.exists()) {
            return showInitDialog(parent);
        }
        if (ks.isPlainFile()) {
            return loadPlain(parent);
        }
        return showUnlockDialog(parent);
    }

    private static boolean loadPlain(Component parent) {
        try {
            AppContext.keyStore.unlock(new char[0]);
            return true;
        } catch (KeyStoreException e) {
            SwingUtils.error(parent, e.getMessage());
            return false;
        }
    }

    public static boolean showUnlockDialog(Component parent) {
        KeyStoreManager ks = AppContext.keyStore;
        if (ks.exists() && ks.isPlainFile()) {
            return loadPlain(parent);
        }
        while (!ks.isUnlocked()) {
            JPanel panel = new JPanel(new GridBagLayout());
            JLabel tip = new JLabel("密钥库已锁定，请输入主密码解锁：");
            JPasswordField pwd = new JPasswordField(20);
            addRow(panel, tip, 0);
            addRow(panel, pwd, 1);
            int option = JOptionPane.showConfirmDialog(parent, panel, "解锁密钥库",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return false;
            }
            try {
                ks.unlock(pwd.getPassword());
            } catch (KeyStoreException e) {
                SwingUtils.error(parent, e.getMessage());
            }
        }
        return true;
    }

    public static boolean showInitDialog(Component parent) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel warn = new JLabel("<html>首次使用请设置主密码，也可全部留空跳过密码。<br>"
                + "<font color='#C62828'>设置密码后：主密码用于加密本地密钥库，一旦遗忘将无法找回密钥！</font><br>"
                + "<font color='#455A64'>留空不设置密码：密钥库以明文保存在本地，任何能访问该文件的人都能读取密钥。</font></html>");
        JPasswordField pwd = new JPasswordField(20);
        JPasswordField pwd2 = new JPasswordField(20);
        addRow(panel, warn, 0);
        addRow(panel, pwd, 1);
        addRow(panel, pwd2, 2);
        while (true) {
            int option = JOptionPane.showConfirmDialog(parent, panel, "初始化密钥库",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] p1 = pwd.getPassword();
            char[] p2 = pwd2.getPassword();
            try {
                if (p1.length == 0 && p2.length == 0) {
                    if (!SwingUtils.confirm(parent, "确定不设置密码吗？\n密钥库将以明文保存在本地，"
                            + "任何能访问该文件的人都能读取其中的密钥。")) {
                        continue;
                    }
                    AppContext.keyStore.initialize(new char[0]);
                    SwingUtils.info(parent, "密钥库已创建（未设置密码，明文保存）");
                    return true;
                }
                if (p1.length < 6) {
                    SwingUtils.error(parent, "主密码至少 6 位，或全部留空表示不设置密码");
                    continue;
                }
                if (!Arrays.equals(p1, p2)) {
                    SwingUtils.error(parent, "两次输入的密码不一致");
                    continue;
                }
                AppContext.keyStore.initialize(p1);
                SwingUtils.info(parent, "密钥库初始化成功");
                return true;
            } catch (KeyStoreException e) {
                SwingUtils.error(parent, e.getMessage());
                return false;
            }
        }
    }

    /** 修改主密码对话框：支持 密码⇆无密码 双向切换。 */
    public static boolean showChangePasswordDialog(Component parent) {
        KeyStoreManager ks = AppContext.keyStore;
        if (!ks.isUnlocked()) {
            SwingUtils.error(parent, "请先解锁密钥库");
            return false;
        }
        boolean protectedNow = ks.isProtected();
        JPanel panel = new JPanel(new GridBagLayout());
        JPasswordField oldPwd = new JPasswordField(20);
        JPasswordField newPwd = new JPasswordField(20);
        JPasswordField newPwd2 = new JPasswordField(20);
        int row = 0;
        if (protectedNow) {
            addRow(panel, labeled("原密码：", oldPwd), row++);
        }
        addRow(panel, labeled("新密码：", newPwd), row++);
        addRow(panel, labeled("确认新密码：", newPwd2), row++);
        addRow(panel, new JLabel("<html><font color='#455A64'>新密码留空 = 切换为无密码（密钥库明文保存）；"
                + "至少 6 位 = 设置/更换密码。</font></html>"), row);

        String title = protectedNow ? "修改主密码" : "设置主密码";
        while (true) {
            int option = JOptionPane.showConfirmDialog(parent, panel, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] oldP = protectedNow ? oldPwd.getPassword() : new char[0];
            char[] newP = newPwd.getPassword();
            if (protectedNow && oldP.length == 0) {
                SwingUtils.error(parent, "请输入原密码");
                continue;
            }
            if (!Arrays.equals(newP, newPwd2.getPassword())) {
                SwingUtils.error(parent, "两次输入的新密码不一致");
                continue;
            }
            try {
                ks.changePassword(oldP, newP);
            } catch (KeyStoreException e) {
                SwingUtils.error(parent, e.getMessage());
                continue;
            }
            SwingUtils.info(parent, newP.length == 0 ? "已切换为无密码模式（明文保存）" : "密码已修改");
            return true;
        }
    }

    private static JPanel labeled(String text, JComponent field) {
        JPanel row = new JPanel(new java.awt.BorderLayout(6, 0));
        JLabel label = new JLabel(text);
        label.setPreferredSize(new java.awt.Dimension(90, label.getPreferredSize().height));
        row.add(label, java.awt.BorderLayout.WEST);
        row.add(field, java.awt.BorderLayout.CENTER);
        return row;
    }

    private static void addRow(JPanel panel, JComponent c, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 10, 4, 10);
        panel.add(c, gbc);
    }
}
