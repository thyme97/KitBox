package com.kitbox.ui.panel;

import com.kitbox.tools.ConvertToolsService;
import com.kitbox.tools.SnowflakeService;
import com.kitbox.ui.FormPanel;
import com.kitbox.ui.SwingUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 转换工具面板：时间戳 ⇄ 日期时间、任意进制互转、批量 UUID。
 */
public class ConvertPanel extends JPanel {

    private final JTextField tsInput = new JTextField(24);
    private final JTextField tsDatetimeResult = new JTextField(24);
    private final JTextField tsSecondsResult = new JTextField(24);
    private final JTextField tsMillisResult = new JTextField(24);

    private final JTextField radixInput = new JTextField(24);
    private final JTextField radixResult = new JTextField(24);
    private final JComboBox<Integer> fromBaseCombo = new JComboBox<>(new Integer[]{2, 8, 10, 16, 32, 36});
    private final JComboBox<Integer> toBaseCombo = new JComboBox<>(new Integer[]{16, 10, 2, 8, 32, 36});

    private final JSpinner uuidCount = new JSpinner(new javax.swing.SpinnerNumberModel(5, 1, 1000, 1));
    private final JCheckBox uuidUpper = new JCheckBox("大写");
    private final JCheckBox uuidNoDashes = new JCheckBox("去连字符");
    private final JTextArea uuidOutput = new JTextArea(10, 40);

    private final JSpinner sfWorkerSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 31, 1));
    private final JSpinner sfDcSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(0, 0, 31, 1));
    private final JSpinner sfCountSpinner = new JSpinner(new javax.swing.SpinnerNumberModel(10, 1, 1000, 1));
    private final JTextArea sfOutput = new JTextArea(8, 40);
    private final JTextField sfParseInput = new JTextField(24);
    private final JTextField sfTimeResult = new JTextField(24);
    private final JTextField sfDcResult = new JTextField(24);
    private final JTextField sfWorkerResult = new JTextField(24);
    private final JTextField sfSeqResult = new JTextField(24);

    public ConvertPanel() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("时间戳", buildTimestampTab());
        tabs.addTab("进制转换", buildRadixTab());
        tabs.addTab("UUID", buildUuidTab());
        tabs.addTab("雪花 ID", buildSnowflakeTab());
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildTimestampTab() {
        FormPanel form = new FormPanel();
        form.addField("输入", tsInput);
        form.addHint("输入 Unix 时间戳（秒或毫秒，自动识别）或日期时间（yyyy-MM-dd HH:mm:ss），点击「转换」自动判向。");

        JButton convert = new JButton("转换");
        JButton now = new JButton("当前时间");
        convert.addActionListener(e -> SwingUtils.runWithCatch(this, this::convertTimestamp));
        now.addActionListener(e -> {
            tsInput.setText(ConvertToolsService.nowTimestamp(true));
            convertTimestamp();
        });
        form.addFull(SwingUtils.row(convert, now));

        FormPanel resultForm = new FormPanel();
        tsDatetimeResult.setEditable(false);
        tsSecondsResult.setEditable(false);
        tsMillisResult.setEditable(false);
        resultForm.addField("日期时间", tsDatetimeResult);
        resultForm.addField("时间戳(秒)", tsSecondsResult);
        resultForm.addField("时间戳(毫秒)", tsMillisResult);
        resultForm.addHint("双击结果框可复制。");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        panel.add(resultForm, BorderLayout.CENTER);
        for (JTextField field : new JTextField[]{tsDatetimeResult, tsSecondsResult, tsMillisResult}) {
            field.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && !field.getText().isEmpty()) {
                        SwingUtils.copyToClipboard(field.getText());
                        tsInput.requestFocus();
                    }
                }
            });
        }
        return panel;
    }

    private void convertTimestamp() {
        String input = tsInput.getText().trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("请输入时间戳或日期时间");
        }
        if (input.matches("-?\\d+")) {
            String datetime = ConvertToolsService.timestampToDatetime(input);
            tsDatetimeResult.setText(datetime);
            tsSecondsResult.setText(ConvertToolsService.datetimeToTimestamp(trimMillis(datetime), false));
            tsMillisResult.setText(ConvertToolsService.datetimeToTimestamp(trimMillis(datetime), true));
        } else {
            tsDatetimeResult.setText(ConvertToolsService.timestampToDatetime(
                    ConvertToolsService.datetimeToTimestamp(input, true)));
            tsSecondsResult.setText(ConvertToolsService.datetimeToTimestamp(input, false));
            tsMillisResult.setText(ConvertToolsService.datetimeToTimestamp(input, true));
        }
    }

    private static String trimMillis(String datetime) {
        int dot = datetime.indexOf('.');
        return dot > 0 ? datetime.substring(0, dot) : datetime;
    }

    private JPanel buildRadixTab() {
        FormPanel form = new FormPanel();
        form.addField("数值", radixInput);
        form.addField("源进制", fromBaseCombo);
        form.addField("目标进制", toBaseCombo);
        form.addHint("支持 2 ~ 36 进制与大整数，负数保留符号。");
        JButton convert = new JButton("转换");
        convert.addActionListener(e -> SwingUtils.runWithCatch(this, this::convertRadix));
        form.addFull(convert);

        radixResult.setEditable(false);
        FormPanel resultForm = new FormPanel();
        resultForm.addField("结果", radixResult);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        panel.add(resultForm, BorderLayout.CENTER);
        return panel;
    }

    private void convertRadix() {
        Integer from = (Integer) fromBaseCombo.getSelectedItem();
        Integer to = (Integer) toBaseCombo.getSelectedItem();
        radixResult.setText(ConvertToolsService.convertRadix(radixInput.getText(), from, to));
    }

    private JPanel buildUuidTab() {
        FormPanel form = new FormPanel();
        form.addField("数量", uuidCount);
        form.addFull(uuidUpper);
        form.addFull(uuidNoDashes);
        JButton generate = new JButton("生成");
        JButton copy = new JButton("复制全部");
        JButton clear = new JButton("清空");
        SwingUtils.uniformSize(generate, copy, clear);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generateUuids));
        copy.addActionListener(e -> {
            if (!uuidOutput.getText().isEmpty()) {
                SwingUtils.copyToClipboard(uuidOutput.getText());
            }
        });
        clear.addActionListener(e -> uuidOutput.setText(""));
        form.addFull(SwingUtils.row(generate, copy, clear));

        uuidOutput.setEditable(false);
        uuidOutput.setFont(mono());
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("结果"));
        outputPanel.add(new JScrollPane(uuidOutput), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);
        panel.add(outputPanel, BorderLayout.CENTER);
        return panel;
    }

    private void generateUuids() {
        int count = (Integer) uuidCount.getValue();
        List<String> uuids = ConvertToolsService.generateUuids(count, uuidUpper.isSelected(), uuidNoDashes.isSelected());
        StringBuilder sb = new StringBuilder();
        for (String uuid : uuids) {
            sb.append(uuid).append('\n');
        }
        uuidOutput.setText(sb.toString());
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(uuidOutput.getText().trim());
        }
    }

    // ---------------- 雪花 ID ----------------

    private JPanel buildSnowflakeTab() {
        FormPanel genForm = new FormPanel();
        genForm.addField("机器 ID", sfWorkerSpinner);
        genForm.addField("数据中心 ID", sfDcSpinner);
        genForm.addField("数量", sfCountSpinner);
        genForm.addHint("结构：1 位符号 + 41 位毫秒时间戳 + 5 位数据中心 + 5 位机器 + 12 位序列，纪元 1288834974657（与 MyBatis-Plus 等主流实现一致）。");
        JButton generate = new JButton("生成");
        JButton copy = new JButton("复制全部");
        JButton clear = new JButton("清空");
        SwingUtils.uniformSize(generate, copy, clear);
        generate.addActionListener(e -> SwingUtils.runWithCatch(this, this::generateSnowflake));
        copy.addActionListener(e -> {
            if (!sfOutput.getText().isEmpty()) {
                SwingUtils.copyToClipboard(sfOutput.getText());
            }
        });
        clear.addActionListener(e -> sfOutput.setText(""));
        genForm.addFull(SwingUtils.row(generate, copy, clear));

        sfOutput.setEditable(false);
        sfOutput.setFont(mono());
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("生成的 ID（每行一条，十进制）"));
        outputPanel.add(new JScrollPane(sfOutput), BorderLayout.CENTER);

        FormPanel parseForm = new FormPanel();
        parseForm.setBorder(BorderFactory.createTitledBorder("解析"));
        parseForm.addField("ID", sfParseInput);
        parseForm.addHint("支持十进制与 0x 开头的十六进制，可用于排查重复 ID 的来源与时间。");
        JButton parse = new JButton("解析");
        parse.addActionListener(e -> SwingUtils.runWithCatch(this, this::parseSnowflake));
        parseForm.addFull(parse);
        sfTimeResult.setEditable(false);
        sfDcResult.setEditable(false);
        sfWorkerResult.setEditable(false);
        sfSeqResult.setEditable(false);
        parseForm.addField("生成时间", sfTimeResult);
        parseForm.addField("数据中心 ID", sfDcResult);
        parseForm.addField("机器 ID", sfWorkerResult);
        parseForm.addField("序列号", sfSeqResult);

        JPanel parsePanel = new JPanel(new BorderLayout());
        parsePanel.add(parseForm, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.add(outputPanel, BorderLayout.NORTH);
        center.add(parsePanel, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(genForm, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private void generateSnowflake() {
        SnowflakeService.configure((Integer) sfWorkerSpinner.getValue(), (Integer) sfDcSpinner.getValue());
        List<String> ids = SnowflakeService.generate((Integer) sfCountSpinner.getValue());
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(id).append('\n');
        }
        sfOutput.setText(sb.toString());
        if (com.kitbox.AppContext.config.isAutoCopyResult()) {
            SwingUtils.copyToClipboard(sfOutput.getText().trim());
        }
    }

    private void parseSnowflake() {
        SnowflakeService.Parsed parsed = SnowflakeService.parse(sfParseInput.getText());
        sfTimeResult.setText(parsed.datetime);
        sfDcResult.setText(String.valueOf(parsed.datacenterId));
        sfWorkerResult.setText(String.valueOf(parsed.workerId));
        sfSeqResult.setText(String.valueOf(parsed.sequence));
    }

    private static Font mono() {
        return SwingUtils.monoFont(new JTextArea().getFont().getSize());
    }
}
