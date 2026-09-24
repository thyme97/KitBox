package com.kitbox.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用配置（明文 JSON，不存储任何密钥材料）。
 */
public class AppConfig {

    /** 界面主题：system（跟随系统深浅色）/ light / dark */
    private String theme = "system";
    /** 全局字体大小 */
    private int fontSize = 13;
    /** 对称加解密默认输出编码：BASE64 / HEX */
    private String defaultOutputEncoding = "BASE64";
    /** 摘要输出默认大写 */
    private boolean digestUppercase = false;
    /** 操作成功后自动复制结果 */
    private boolean autoCopyResult = false;
    /** JSON 处理结果格式化输出 */
    private boolean prettyJson = true;
    /** 主窗口尺寸记忆 */
    private int windowWidth = 1080;
    private int windowHeight = 740;
    /** 报文格式模板列表 */
    private List<MessageTemplate> templates = new ArrayList<>();
    /** 最近使用的工具（主页右下角展示，Ctrl+H 打开最近一个），最多保留 5 个 */
    private List<String> recentTools = new ArrayList<>();

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getDefaultOutputEncoding() {
        return defaultOutputEncoding;
    }

    public void setDefaultOutputEncoding(String defaultOutputEncoding) {
        this.defaultOutputEncoding = defaultOutputEncoding;
    }

    public boolean isDigestUppercase() {
        return digestUppercase;
    }

    public void setDigestUppercase(boolean digestUppercase) {
        this.digestUppercase = digestUppercase;
    }

    public boolean isAutoCopyResult() {
        return autoCopyResult;
    }

    public void setAutoCopyResult(boolean autoCopyResult) {
        this.autoCopyResult = autoCopyResult;
    }

    public boolean isPrettyJson() {
        return prettyJson;
    }

    public void setPrettyJson(boolean prettyJson) {
        this.prettyJson = prettyJson;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
    }

    public List<MessageTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<MessageTemplate> templates) {
        this.templates = templates;
    }

    public List<String> getRecentTools() {
        return recentTools;
    }

    public void setRecentTools(List<String> recentTools) {
        this.recentTools = recentTools;
    }
}
