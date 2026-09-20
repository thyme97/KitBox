package com.kitbox.crypto.model;

/** 对称加密工作模式。 */
public enum CryptoMode {
    ECB("ECB"),
    CBC("CBC"),
    CTR("CTR"),
    GCM("GCM");

    private final String display;

    CryptoMode(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    /** 是否需要 IV */
    public boolean needIv() {
        return this != ECB;
    }

    @Override
    public String toString() {
        return display;
    }
}
