package com.kitbox.keystore;

/**
 * 密钥条目。
 * <p>
 * value 存储格式（均为文本）：
 * - 对称/HMAC：原始密钥字节的 Base64
 * - 公钥/私钥单条：DER 编码的 Base64（RSA: X.509/PKCS8；SM2 同）
 * - 密钥对：JSON {"publicKey":"<Base64>","privateKey":"<Base64>"}
 */
public class KeyEntry {

    private String id;
    private String scenario;
    private String name;
    private KeyEntryType type;
    private String value;
    private String remark;
    private String createdAt;

    public KeyEntry() {
    }

    public KeyEntry(String id, String scenario, String name, KeyEntryType type,
                    String value, String remark, String createdAt) {
        this.id = id;
        this.scenario = scenario;
        this.name = name;
        this.type = type;
        this.value = value;
        this.remark = remark;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KeyEntryType getType() {
        return type;
    }

    public void setType(KeyEntryType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    /** 树/列表展示用：名称 [类型] */
    @Override
    public String toString() {
        String name = this.name == null ? "(未命名)" : this.name;
        String typeDisplay = type == null ? "未知类型" : type.getDisplay();
        return name + "  [" + typeDisplay + "]";
    }
}
