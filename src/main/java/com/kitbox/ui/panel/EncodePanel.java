package com.kitbox.ui.panel;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.EncodingService;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.TextIOPane;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * 编码转换面板：Base64 / Hex / URL。
 */
public class EncodePanel extends JPanel {

    private enum EncodeType {
        BASE64("Base64"), HEX("Hex"), URL("URL");

        private final String display;

        EncodeType(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private enum Direction {
        ENCODE("编码"), DECODE("解码");

        private final String display;

        Direction(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private final JComboBox<EncodeType> typeCombo = new JComboBox<>(EncodeType.values());
    private final JComboBox<Direction> directionCombo = new JComboBox<>(Direction.values());
    private final JCheckBox urlSafeCheck = new JCheckBox("URL 安全（Base64）");
    private final JCheckBox lineBreakCheck = new JCheckBox("按行分割（Base64）");
    private final TextIOPane io = new TextIOPane("输入", "结果");

    public EncodePanel() {
        setLayout(new BorderLayout());

        JPanel paramBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        paramBar.add(typeCombo);
        paramBar.add(directionCombo);
        paramBar.add(urlSafeCheck);
        paramBar.add(lineBreakCheck);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton run = new JButton("执行");
        JButton clear = new JButton("全部清空");
        buttonBar.add(run);
        buttonBar.add(clear);
        run.addActionListener(e -> SwingUtils.runWithCatch(this, this::run));
        clear.addActionListener(e -> io.clearAll());

        JPanel north = new JPanel(new BorderLayout());
        north.add(paramBar, BorderLayout.NORTH);
        north.add(buttonBar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(io, BorderLayout.CENTER);
    }

    private void run() throws CryptoException {
        String input = io.getInput();
        if (input.isEmpty()) {
            throw new CryptoException("请输入内容");
        }
        EncodeType type = (EncodeType) typeCombo.getSelectedItem();
        Direction dir = (Direction) directionCombo.getSelectedItem();
        boolean urlSafe = urlSafeCheck.isSelected();
        boolean lineBreaks = lineBreakCheck.isSelected();
        String result;
        try {
            if (type == EncodeType.BASE64) {
                result = dir == Direction.ENCODE
                        ? EncodingService.base64Encode(DataEncoding.utf8(input), urlSafe, lineBreaks)
                        : DataEncoding.utf8(EncodingService.base64Decode(input, urlSafe));
            } else if (type == EncodeType.HEX) {
                result = dir == Direction.ENCODE
                        ? DataEncoding.HEX.encode(DataEncoding.utf8(input))
                        : DataEncoding.utf8(DataEncoding.HEX.decode(input));
            } else {
                result = dir == Direction.ENCODE
                        ? EncodingService.urlEncode(input, "UTF-8")
                        : EncodingService.urlDecode(input, "UTF-8");
            }
        } catch (IllegalArgumentException e) {
            throw new CryptoException("解码失败：" + e.getMessage());
        }
        io.setOutput(result);
        io.note("转换成功");
    }
}
