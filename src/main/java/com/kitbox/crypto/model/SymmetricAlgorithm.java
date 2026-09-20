package com.kitbox.crypto.model;

/**
 * 对称加密算法及其密钥长度约束（字节）。
 */
public enum SymmetricAlgorithm {

    AES("AES", "AES", new int[]{16, 24, 32}),
    DES("DES", "DES", new int[]{8}),
    DESEDE("3DES", "DESede", new int[]{16, 24}),
    SM4("SM4", "SM4", new int[]{16});

    /** 界面展示名 */
    private final String display;
    /** JCA 算法名 */
    private final String jcaName;
    /** 合法密钥长度（字节） */
    private final int[] keyLengths;

    SymmetricAlgorithm(String display, String jcaName, int[] keyLengths) {
        this.display = display;
        this.jcaName = jcaName;
        this.keyLengths = keyLengths;
    }

    public String getDisplay() {
        return display;
    }

    public String getJcaName() {
        return jcaName;
    }

    public int[] getKeyLengths() {
        return keyLengths;
    }

    /** 分组长度（字节） */
    public int getBlockSize() {
        return (this == DES || this == DESEDE) ? 8 : 16;
    }

    /** 是否支持 GCM 模式 */
    public boolean supportGcm() {
        return this == AES || this == SM4;
    }

    /** 该算法是否可用指定长度的密钥 */
    public boolean isValidKeyLength(int len) {
        for (int l : keyLengths) {
            if (l == len) {
                return true;
            }
        }
        return false;
    }

    public String keyLengthHint() {
        StringBuilder sb = new StringBuilder();
        for (int l : keyLengths) {
            if (sb.length() > 0) {
                sb.append("/");
            }
            sb.append(l * 8).append("位");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return display;
    }
}
