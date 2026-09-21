package com.kitbox.ui.components;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.ui.SwingUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.EnumSet;

/**
 * 对称算法参数表单：算法 / 模式 / 填充 / 密钥 / IV / GCM tag，联动显隐。
 * 供「对称加解密」与「报文格式加解密」面板复用。
 */
public class SymmetricParamsForm extends JPanel {

    private final JComboBox<SymmetricAlgorithm> algoCombo = new JComboBox<>(SymmetricAlgorithm.values());
    private final JComboBox<CryptoMode> modeCombo = new JComboBox<>(CryptoMode.values());
    private final JComboBox<Padding> paddingCombo = new JComboBox<>(new Padding[]{Padding.PKCS5, Padding.NO_PADDING});
    private final KeyPickerField keyField = new KeyPickerField(
            EnumSet.of(KeyEntryType.Kind.SYMMETRIC), null, true);
    private final JTextField ivField = new JTextField(16);
    private final JComboBox<KeyFormat> ivFormatCombo = new JComboBox<>(new KeyFormat[]{
            KeyFormat.HEX, KeyFormat.BASE64, KeyFormat.PLAIN});
    private final JComboBox<Integer> tagCombo = new JComboBox<>(new Integer[]{128, 120, 112, 96});
    private final JLabel ivLabel = new JLabel("IV：");
    private final JLabel tagLabel = new JLabel("GCM Tag：");

    /** 参数读取结果。 */
    public static final class Params {
        public SymmetricAlgorithm algo;
        public CryptoMode mode;
        public Padding padding;
        public byte[] key;
        public byte[] iv;
        public int tagBits;
    }

    public SymmetricParamsForm() {
        super(new GridBagLayout());
        GridBagConstraints outer = new GridBagConstraints();
        outer.gridx = 0;
        outer.weightx = 1;
        outer.fill = GridBagConstraints.HORIZONTAL;

        outer.gridy = 0;
        add(algoRow(), outer);
        outer.gridy = 1;
        add(keyRow(), outer);
        outer.gridy = 2;
        add(ivRow(), outer);

        tagCombo.setSelectedIndex(0);
        modeCombo.addActionListener(e -> refreshState());
        algoCombo.addActionListener(e -> refreshState());
        refreshState();
    }

    /** 算法行：独立面板，避免跨行列宽互相拉扯。 */
    private JComponent algoRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        row.add(SwingUtils.groupLabel("算法"));
        row.add(wrapCombo(algoCombo, 90));
        row.add(new JLabel("模式："));
        row.add(wrapCombo(modeCombo, 80));
        row.add(new JLabel("填充："));
        row.add(wrapCombo(paddingCombo, 150));
        return row;
    }

    /** 密钥行：密钥输入拉伸占满，随机密钥按钮靠右。 */
    private JComponent keyRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        row.add(SwingUtils.groupLabel("密钥"), BorderLayout.WEST);
        row.add(keyField, BorderLayout.CENTER);
        JButton randomKey = randomKeyButton();
        row.add(randomKey, BorderLayout.EAST);
        return row;
    }

    /** IV / GCM Tag 行（按模式显隐）。 */
    private JComponent ivRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
        row.add(ivLabel, BorderLayout.WEST);

        JPanel ivCenter = new JPanel(new java.awt.BorderLayout(4, 0));
        ivCenter.setOpaque(false);
        ivCenter.add(ivField, java.awt.BorderLayout.CENTER);
        JPanel ivRight = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        JButton randomIv = new JButton("随机");
        randomIv.addActionListener(e -> randomIv());
        ivRight.add(randomIv);
        ivRight.add(wrapCombo(ivFormatCombo, 90));
        ivCenter.add(ivRight, java.awt.BorderLayout.EAST);
        row.add(ivCenter, BorderLayout.CENTER);

        JPanel tagPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tagPart.setOpaque(false);
        tagPart.add(tagLabel);
        tagPart.add(wrapCombo(tagCombo, 80));
        row.add(tagPart, BorderLayout.EAST);
        return row;
    }

    private JButton randomKeyButton() {
        JButton button = new JButton("随机密钥");
        button.addActionListener(e -> {
            SymmetricAlgorithm algo = selectedAlgo();
            int len = algo.getKeyLengths()[0];
            byte[] key = com.kitbox.crypto.SymmetricService.randomKey(algo, len);
            KeyFormat fmt = (KeyFormat) keyField.formatCombo().getSelectedItem();
            String text;
            if (fmt == KeyFormat.HEX) {
                text = com.kitbox.util.HexUtils.encode(key);
            } else if (fmt == KeyFormat.PLAIN) {
                // 随机字节无法作为可读明文展示：明文格式生成等长随机字母数字串（UTF-8 解码正好 len 字节）
                text = com.kitbox.crypto.SymmetricService.randomAlphanumeric(len);
            } else {
                text = java.util.Base64.getEncoder().encodeToString(key);
                keyField.formatCombo().setSelectedItem(KeyFormat.BASE64);
            }
            keyField.setKeyText(text);
            SwingUtils.info(this, "已生成 " + (len * 8) + " 位随机密钥（" + fmt.getDisplay() + "）。\n"
                    + "如需长期使用，建议保存到密钥库。");
        });
        return button;
    }

    private JComponent wrapCombo(JComboBox<?> combo, int width) {
        combo.setPreferredSize(new java.awt.Dimension(width, combo.getPreferredSize().height));
        return combo;
    }

    /** 根据算法/模式联动显隐与取值。 */
    public void refreshState() {
        SymmetricAlgorithm algo = selectedAlgo();
        CryptoMode mode = selectedMode();
        // GCM 支持性
        if (!algo.supportGcm() && mode == CryptoMode.GCM) {
            modeCombo.setSelectedItem(CryptoMode.CBC);
            mode = CryptoMode.CBC;
        }
        // 填充约束
        if (mode == CryptoMode.GCM || mode == CryptoMode.CTR) {
            paddingCombo.setSelectedItem(Padding.NO_PADDING);
            paddingCombo.setEnabled(false);
        } else {
            paddingCombo.setEnabled(true);
        }
        // IV / tag 显隐
        boolean needIv = mode.needIv();
        ivLabel.setVisible(needIv);
        ivField.setVisible(needIv);
        ivField.getParent().setVisible(needIv);
        boolean gcm = mode == CryptoMode.GCM;
        tagLabel.setVisible(gcm);
        tagCombo.setVisible(gcm);
    }

    private void randomIv() {
        byte[] iv = com.kitbox.crypto.SymmetricService.randomIv(selectedAlgo(), selectedMode());
        ivField.setText(com.kitbox.util.HexUtils.encode(iv));
        ivFormatCombo.setSelectedItem(KeyFormat.HEX);
    }

    private SymmetricAlgorithm selectedAlgo() {
        return (SymmetricAlgorithm) algoCombo.getSelectedItem();
    }

    private CryptoMode selectedMode() {
        return (CryptoMode) modeCombo.getSelectedItem();
    }

    /** 读取并校验全部参数（含密钥与 IV 解码）。 */
    public Params readParams() throws CryptoException {
        Params p = new Params();
        p.algo = selectedAlgo();
        p.mode = selectedMode();
        p.padding = (Padding) paddingCombo.getSelectedItem();
        String keyText = keyField.getKeyText();
        if (keyText.isEmpty()) {
            throw new CryptoException("请输入密钥或从密钥库选择");
        }
        try {
            p.key = keyField.getKeyFormat().decode(keyText);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密钥格式不正确（" + keyField.getKeyFormat().getDisplay() + "）：" + e.getMessage());
        }
        if (!p.algo.isValidKeyLength(p.key.length)) {
            throw new CryptoException(p.algo.getDisplay() + " 密钥长度必须为 " + p.algo.keyLengthHint()
                    + "（当前 " + p.key.length * 8 + " 位）");
        }
        p.iv = new byte[0];
        if (p.mode.needIv()) {
            String ivText = ivField.getText().trim();
            if (ivText.isEmpty()) {
                throw new CryptoException("请输入 IV 或点击「随机」生成");
            }
            try {
                p.iv = ((KeyFormat) ivFormatCombo.getSelectedItem()).decode(ivText);
            } catch (IllegalArgumentException e) {
                throw new CryptoException("IV 格式不正确：" + e.getMessage());
            }
            if (p.mode != CryptoMode.GCM && p.iv.length != p.algo.getBlockSize()) {
                throw new CryptoException(p.algo.getDisplay() + " 的 IV 长度必须为 " + p.algo.getBlockSize()
                        + " 字节（当前 " + p.iv.length + "）");
            }
        }
        p.tagBits = p.mode == CryptoMode.GCM ? (Integer) tagCombo.getSelectedItem() : 0;
        return p;
    }

    public KeyPickerField keyField() {
        return keyField;
    }

    public JComboBox<SymmetricAlgorithm> algoCombo() {
        return algoCombo;
    }

    public JComboBox<CryptoMode> modeCombo() {
        return modeCombo;
    }

    public JComboBox<Padding> paddingCombo() {
        return paddingCombo;
    }

    public JComboBox<KeyFormat> ivFormatCombo() {
        return ivFormatCombo;
    }

    public JTextField ivField() {
        return ivField;
    }
}
