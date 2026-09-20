package com.kitbox.ui.panel;

import com.kitbox.tools.JsonToolsService;
import com.kitbox.ui.SwingUtils;
import com.kitbox.ui.components.TextIOPane;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * JSON 工具面板：格式化 / 压缩 / 校验 / 字符串转义与去转义。
 */
public class JsonToolsPanel extends JPanel {

    private enum Indent {
        TWO("缩进 2 空格", 2),
        FOUR("缩进 4 空格", 4);

        private final String display;
        private final int spaces;

        Indent(String display, int spaces) {
            this.display = display;
            this.spaces = spaces;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private final JComboBox<Indent> indentCombo = new JComboBox<>(Indent.values());
    private final TextIOPane io = new TextIOPane("输入 JSON / 字符串", "结果");

    public JsonToolsPanel() {
        setLayout(new BorderLayout());

        JPanel paramBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        paramBar.add(indentCombo);
        JButton format = new JButton("格式化");
        JButton compress = new JButton("压缩");
        JButton validate = new JButton("校验");
        JButton escape = new JButton("转义");
        JButton unescape = new JButton("去转义");
        JButton clear = new JButton("全部清空");
        SwingUtils.uniformSize(format, compress, validate, escape, unescape, clear);
        paramBar.add(format);
        paramBar.add(compress);
        paramBar.add(validate);
        paramBar.add(escape);
        paramBar.add(unescape);
        paramBar.add(clear);

        format.addActionListener(e -> SwingUtils.runWithCatch(this, this::runFormat));
        compress.addActionListener(e -> SwingUtils.runWithCatch(this, this::runCompress));
        validate.addActionListener(e -> SwingUtils.runWithCatch(this, this::runValidate));
        escape.addActionListener(e -> SwingUtils.runWithCatch(this, this::runEscape));
        unescape.addActionListener(e -> SwingUtils.runWithCatch(this, this::runUnescape));
        clear.addActionListener(e -> io.clearAll());

        add(paramBar, BorderLayout.NORTH);
        add(io, BorderLayout.CENTER);
    }

    private void requireInput() {
        if (io.getInput().trim().isEmpty()) {
            throw new IllegalArgumentException("请输入内容");
        }
    }

    private void runFormat() {
        requireInput();
        io.setOutput(JsonToolsService.format(io.getInput(), ((Indent) indentCombo.getSelectedItem()).spaces));
        io.note("格式化完成");
    }

    private void runCompress() {
        requireInput();
        io.setOutput(JsonToolsService.compress(io.getInput()));
        io.note("压缩完成");
    }

    private void runValidate() {
        requireInput();
        String error = JsonToolsService.validate(io.getInput());
        if (error == null) {
            io.setOutput("");
            io.note("✓ JSON 语法合法");
        } else {
            SwingUtils.error(this, error);
            io.note("✗ JSON 语法错误");
        }
    }

    private void runEscape() {
        requireInput();
        io.setOutput(JsonToolsService.escape(io.getInput()));
        io.note("已转义为 JSON 字符串字面量");
    }

    private void runUnescape() {
        requireInput();
        io.setOutput(JsonToolsService.unescape(io.getInput()));
        io.note("已去转义");
    }
}
