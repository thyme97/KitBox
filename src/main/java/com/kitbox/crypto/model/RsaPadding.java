package com.kitbox.crypto.model;

/** RSA 加密填充方式。 */
public enum RsaPadding {

    PKCS1("PKCS1", "RSA/ECB/PKCS1Padding", 11),
    OAEP_SHA1("OAEP-SHA1", "RSA/ECB/OAEPWithSHA-1AndMGF1Padding", -1),
    OAEP_SHA256("OAEP-SHA256", "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", -1);

    private final String display;
    private final String transformation;
    /** PKCS1 每块开销字节数；OAEP 单独计算 */
    private final int overhead;

    RsaPadding(String display, String transformation, int overhead) {
        this.display = display;
        this.transformation = transformation;
        this.overhead = overhead;
    }

    public String getDisplay() {
        return display;
    }

    public String getTransformation() {
        return transformation;
    }

    /** 单块明文最大长度 = 密钥字节数 - 开销 */
    public int maxBlock(int keyBytes) {
        if (this == PKCS1) {
            return keyBytes - overhead;
        }
        if (this == OAEP_SHA1) {
            return keyBytes - 2 * 20 - 2;
        }
        return keyBytes - 2 * 32 - 2;
    }

    @Override
    public String toString() {
        return display;
    }
}
