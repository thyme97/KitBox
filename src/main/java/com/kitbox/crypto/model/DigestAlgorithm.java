package com.kitbox.crypto.model;

/** 摘要算法。 */
public enum DigestAlgorithm {

    MD5("MD5", "MD5"),
    SHA1("SHA-1", "SHA-1"),
    SHA224("SHA-224", "SHA-224"),
    SHA256("SHA-256", "SHA-256"),
    SHA384("SHA-384", "SHA-384"),
    SHA512("SHA-512", "SHA-512"),
    SM3("SM3", "SM3");

    private final String display;
    private final String jcaName;

    DigestAlgorithm(String display, String jcaName) {
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
        return this == SM3;
    }

    @Override
    public String toString() {
        return display;
    }
}
