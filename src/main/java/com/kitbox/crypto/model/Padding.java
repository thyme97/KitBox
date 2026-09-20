package com.kitbox.crypto.model;

/** 填充方式。 */
public enum Padding {
    PKCS5("PKCS5Padding"),
    NO_PADDING("NoPadding");

    private final String display;

    Padding(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }
}
