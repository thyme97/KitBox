package com.kitbox.crypto.model;

/** 非对称算法。 */
public enum AsymmetricAlgorithm {
    RSA("RSA"),
    SM2("SM2");

    private final String display;

    AsymmetricAlgorithm(String display) {
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
