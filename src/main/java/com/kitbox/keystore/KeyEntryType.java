package com.kitbox.keystore;

/**
 * 密钥条目类型及其用途分类（用于密钥选择器按需过滤）。
 */
public enum KeyEntryType {

    SYM_AES("AES 密钥", Kind.SYMMETRIC),
    SYM_DES("DES 密钥", Kind.SYMMETRIC),
    SYM_DESEDE("3DES 密钥", Kind.SYMMETRIC),
    SYM_SM4("SM4 密钥", Kind.SYMMETRIC),
    HMAC("HMAC 密钥", Kind.HMAC),
    RSA_KEYPAIR("RSA 密钥对", Kind.KEYPAIR),
    RSA_PUBLIC("RSA 公钥", Kind.PUBLIC),
    RSA_PRIVATE("RSA 私钥", Kind.PRIVATE),
    SM2_KEYPAIR("SM2 密钥对", Kind.KEYPAIR),
    SM2_PUBLIC("SM2 公钥", Kind.PUBLIC),
    SM2_PRIVATE("SM2 私钥", Kind.PRIVATE);

    /** 用途分类：密钥选择器按此过滤 */
    public enum Kind {
        SYMMETRIC, HMAC, KEYPAIR, PUBLIC, PRIVATE
    }

    private final String display;
    private final Kind kind;

    KeyEntryType(String display, Kind kind) {
        this.display = display;
        this.kind = kind;
    }

    public String getDisplay() {
        return display;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return display;
    }
}
