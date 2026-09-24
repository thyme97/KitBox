package com.kitbox.ui.panel;

import com.kitbox.tools.FakeDataService;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.WrapLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Map;

/**
 * 假数据生成面板：身份证号（可选属地省/市/区）、手机号、姓名、邮箱、银行卡号、IP、MAC。
 * 所有数据均由随机算法合成，仅供开发测试使用。
 */
public class FakeDataPanel extends JPanel {

    private final JCheckBox idCheck = new JCheckBox("身份证号", true);
    private final JCheckBox phoneCheck = new JCheckBox("手机号", true);
    private final JCheckBox nameCheck = new JCheckBox("姓名", true);
    private final JCheckBox emailCheck = new JCheckBox("邮箱", true);
    private final JCheckBox bankCheck = new JCheckBox("银行卡号");
    private final JCheckBox ipv4Check = new JCheckBox("IPv4 地址");
    private final JCheckBox macCheck = new JCheckBox("MAC 地址");
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));

    private final JComboBox<String> provinceCombo = new JComboBox<>();
    private final JComboBox<String> cityCombo = new JComboBox<>();
    private final JComboBox<String> districtCombo = new JComboBox<>();
    private static final String RANDOM_ITEM = "随机";

    private final JTextArea resultArea = new JTextArea();

    public FakeDataPanel() {
        super(new BorderLayout());

        JPanel checkRow = new JPanel(new WrapLayout(WrapLayout.LEFT, 14, 4));
        checkRow.setBorder(BorderFactory.createEmptyBorder(3, 8, 0, 8));
        checkRow.add(idCheck);
        checkRow.add(phoneCheck);
        checkRow.add(nameCheck);
        checkRow.add(emailCheck);
        checkRow.add(bankCheck);
        checkRow.add(ipv4Check);
        checkRow.add(macCheck);

        // 身份证属地：省 / 市 / 区 三级联动，默认全随机
        provinceCombo.addItem(RANDOM_ITEM);
        for (String p : com.kitbox.tools.FakeDataService.provinces()) {
            provinceCombo.addItem(p);
        }
        rebuildCityCombo();
        rebuildDistrictCombo();
        provinceCombo.addActionListener(e -> {
            rebuildCityCombo();
            rebuildDistrictCombo();
        });
        cityCombo.addActionListener(e -> rebuildDistrictCombo());
        comboWidth(provinceCombo, 130);
        comboWidth(cityCombo, 110);
        comboWidth(districtCombo, 130);

        JPanel areaRow = new JPanel(new WrapLayout(WrapLayout.LEFT, 6, 2));
        areaRow.setBorder(BorderFactory.createEmptyBorder(2, 8, 3, 8));
        areaRow.add(new JLabel("身份证属地："));
        areaRow.add(provinceCombo);
        areaRow.add(cityCombo);
        areaRow.add(districtCombo);
        idCheck.addActionListener(e -> areaRow.setVisible(idCheck.isSelected()));

        FormPanel countForm = new FormPanel();
        countForm.addField("生成数量：", countSpinner, false);

        JPanel optionsCard = new JPanel(new BorderLayout());
        optionsCard.setBorder(SwingUtils.cardBorder("生成项"));
        optionsCard.add(checkRow, BorderLayout.NORTH);
        optionsCard.add(areaRow, BorderLayout.CENTER);
        optionsCard.add(countForm, BorderLayout.SOUTH);

        JButton generate = new JButton("生成");
        JButton copyAll = new JButton("复制全部");
        JButton clear = new JButton("清空");
        SwingUtils.stylePrimary(generate);
        SwingUtils.styleSecondary(copyAll);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::doGenerate));
        copyAll.addActionListener(e -> {
            if (resultArea.getText().isEmpty()) {
                SwingUtils.info(this, "暂无生成结果");
                return;
            }
            SwingUtils.copyToClipboard(resultArea.getText());
            SwingUtils.showToast(copyAll, "已复制到剪贴板");
        });
        clear.addActionListener(e -> resultArea.setText(""));

        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, resultArea.getFont().getSize()));
        JPanel resultCard = new JPanel(new BorderLayout());
        resultCard.setBorder(SwingUtils.cardBorder("生成结果"));
        resultCard.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        JPanel north = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        north.add(optionsCard, gc);
        gc.gridy = 1;
        north.add(SwingUtils.actionBar(
                new javax.swing.JComponent[]{generate, copyAll},
                new javax.swing.JComponent[]{clear}), gc);

        add(north, BorderLayout.NORTH);
        add(resultCard, BorderLayout.CENTER);
    }

    private void rebuildCityCombo() {
        cityCombo.removeAllItems();
        cityCombo.addItem(RANDOM_ITEM);
        String province = selected(provinceCombo);
        if (province != null && !RANDOM_ITEM.equals(province)) {
            for (String city : com.kitbox.tools.FakeDataService.citiesOf(province)) {
                cityCombo.addItem(city);
            }
        }
    }

    private void rebuildDistrictCombo() {
        districtCombo.removeAllItems();
        districtCombo.addItem(RANDOM_ITEM);
        String province = selected(provinceCombo);
        String city = selected(cityCombo);
        if (province != null && !RANDOM_ITEM.equals(province)) {
            for (String district : com.kitbox.tools.FakeDataService
                    .districtsOf(province, RANDOM_ITEM.equals(city) ? null : city).keySet()) {
                districtCombo.addItem(district);
            }
        }
    }

    private static String selected(JComboBox<String> combo) {
        Object item = combo.getSelectedItem();
        return item == null ? null : String.valueOf(item);
    }

    private static void comboWidth(JComboBox<String> combo, int width) {
        combo.setPreferredSize(new java.awt.Dimension(width, combo.getPreferredSize().height));
    }

    /** 按三级选择解析属地代码：区县优先，其次省内随机，再次全随机。 */
    private String resolveAreaCode() {
        String province = selected(provinceCombo);
        String city = selected(cityCombo);
        String district = selected(districtCombo);
        if (district != null && !RANDOM_ITEM.equals(district)) {
            return FakeDataService.districtsOf(province,
                    RANDOM_ITEM.equals(city) ? null : city).get(district);
        }
        if (city != null && !RANDOM_ITEM.equals(city)) {
            Map<String, String> districts = FakeDataService.districtsOf(province, city);
            return new java.util.ArrayList<>(districts.values())
                    .get(java.util.concurrent.ThreadLocalRandom.current().nextInt(districts.size()));
        }
        if (province != null && !RANDOM_ITEM.equals(province)) {
            Map<String, String> districts = FakeDataService.districtsOf(province, null);
            return new java.util.ArrayList<>(districts.values())
                    .get(java.util.concurrent.ThreadLocalRandom.current().nextInt(districts.size()));
        }
        return null;
    }

    private void doGenerate() throws IllegalArgumentException {
        int count = (Integer) countSpinner.getValue();
        String areaCode = idCheck.isSelected() ? resolveAreaCode() : null;
        StringBuilder sb = new StringBuilder();
        addBlock(sb, idCheck.isSelected(), "身份证号", count, areaCode);
        addBlock(sb, phoneCheck.isSelected(), "手机号", count, null);
        addBlock(sb, nameCheck.isSelected(), "姓名", count, null);
        addBlock(sb, emailCheck.isSelected(), "邮箱", count, null);
        addBlock(sb, bankCheck.isSelected(), "银行卡号", count, null);
        addBlock(sb, ipv4Check.isSelected(), "IPv4 地址", count, null);
        addBlock(sb, macCheck.isSelected(), "MAC 地址", count, null);
        if (sb.length() == 0) {
            throw new IllegalArgumentException("请至少勾选一种生成项");
        }
        resultArea.setText(sb.toString());
        resultArea.setCaretPosition(0);
    }

    private void addBlock(StringBuilder sb, boolean selected, String title, int count, String areaCode) {
        if (!selected) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append('【').append(title).append("】\n");
        for (int i = 0; i < count; i++) {
            sb.append(generateOne(title, areaCode)).append('\n');
        }
    }

    private String generateOne(String title, String areaCode) {
        switch (title) {
            case "身份证号":
                return FakeDataService.idCard(areaCode);
            case "手机号":
                return FakeDataService.phone();
            case "姓名":
                return FakeDataService.name();
            case "邮箱":
                return FakeDataService.email();
            case "银行卡号":
                return FakeDataService.bankCard();
            case "IPv4 地址":
                return FakeDataService.ipv4();
            case "MAC 地址":
                return FakeDataService.macAddress();
            default:
                return "";
        }
    }
}
