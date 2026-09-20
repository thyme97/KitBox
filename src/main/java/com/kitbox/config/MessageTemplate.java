package com.kitbox.config;

/**
 * 报文格式模板：前缀 + 密文(Base64/Hex) + 后缀。
 */
public class MessageTemplate {

    private String name;
    private String prefix = "";
    private String suffix = "";
    private String contentEncoding = "BASE64"; // DataEncoding name

    public MessageTemplate() {
    }

    public MessageTemplate(String name, String prefix, String suffix, String contentEncoding) {
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        this.contentEncoding = contentEncoding;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public void setContentEncoding(String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
}
