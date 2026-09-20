package com.kitbox.crypto.model;

/** HMAC 算法。 */
public enum HmacAlgorithm {

    HMAC_MD5("HmacMD5", "HmacMD5"),
    HMAC_SHA1("HmacSHA1", "HmacSHA1"),
    HMAC_SHA256("HmacSHA256", "HmacSHA256"),
    HMAC_SHA512("HmacSHA512", "HmacSHA512"),
    HMAC_SM3("HmacSM3", "HmacSM3");

    private final String display;
    private final String jcaName;

    HmacAlgorithm(String display, String jcaName) {
        this.display = display;
        this.jcaName = jcaName;
    }

    public String getDisplay() {
        return display;
    }

    public String getJcaName() {
        return jcaName;
    }

    /** 是否需要 BouncyCastle 提供者 */
    public boolean needBouncyCastle() {
        return this == HMAC_SM3;
    }

    @Override
    public String toString() {
        return display;
    }
}
