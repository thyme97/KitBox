package com.kitbox.ui.components;

import com.kitbox.AppContext;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.keystore.KeyEntry;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.ui.SwingUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * 密钥输入组件：格式下拉 + 密钥文本框 + 「从密钥库选择」按钮。
 */
public class KeyPickerField extends JPanel {

    private final JComboBox<KeyFormat> formatCombo = new JComboBox<>();
    private final JTextField keyField = new JTextField(22);
    private final EnumSet<KeyEntryType.Kind> kinds;
    /** 从密钥对条目取哪一部分（null 表示整条/对称） */
    private final KeyEntryType.Kind pairPart;
    /** 选中密钥库条目后的回调 */
    private Consumer<KeyEntry> onPicked;

    public KeyPickerField(EnumSet<KeyEntryType.Kind> kinds, KeyEntryType.Kind pairPart, boolean withFormat) {
        this.kinds = kinds;
        this.pairPart = pairPart;
        setLayout(new BorderLayout(4, 0));

        if (withFormat) {
            formatCombo.setModel(new DefaultComboBoxModel<>(new KeyFormat[]{
                    KeyFormat.PLAIN, KeyFormat.BASE64, KeyFormat.HEX}));
            add(formatCombo, BorderLayout.WEST);
        }

        JButton pickButton = new JButton("密钥库…");
        pickButton.addActionListener(e -> pickFromStore());

        JPanel center = new JPanel(new BorderLayout(4, 0));
        center.add(keyField, BorderLayout.CENTER);
        center.add(pickButton, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);
    }

    private void pickFromStore() {
        if (!KeyStoreDialogs.ensureUnlocked(this)) {
            return;
        }
        KeyPickerDialog.pick(dialogOwner(), kinds, entry -> {
            keyField.setText(KeyStoreCodec.partValue(entry, pairPart));
            // 密钥库中存的是规范 Base64
            if (formatCombo.getItemCount() > 0) {
                formatCombo.setSelectedItem(KeyFormat.BASE64);
            }
            if (onPicked != null) {
                onPicked.accept(entry);
            }
        });
    }

    private JFrame dialogOwner() {
        java.awt.Window w = javax.swing.SwingUtilities.getWindowAncestor(this);
        return w instanceof JFrame ? (JFrame) w : null;
    }

    public String getKeyText() {
        return keyField.getText().trim();
    }

    public void setKeyText(String text) {
        keyField.setText(text);
    }

    /** 未提供格式下拉时默认按 Base64 解析。 */
    public KeyFormat getKeyFormat() {
        if (formatCombo.getItemCount() == 0) {
            return KeyFormat.BASE64;
        }
        return (KeyFormat) formatCombo.getSelectedItem();
    }

    public JTextField textField() {
        return keyField;
    }

    public void setOnPicked(Consumer<KeyEntry> onPicked) {
        this.onPicked = onPicked;
    }

    public JComboBox<KeyFormat> formatCombo() {
        return formatCombo;
    }
}
