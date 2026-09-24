package com.kitbox.crypto.model;

/**
 * Jasypt 文本加密算法变体及其密文布局（字节）。
 * <p>
 * 两种变体的密文都是 Base64(盐 || IV || 密文)：
 * <ul>
 * <li>旧版默认 PBEWithMD5AndDES：盐 8 字节，无独立 IV（IV 由 PKCS#5 派生），
 *     密钥由 JCE 口令派生；口令必须为 ASCII（SunJCE 限制，Jasypt 同样如此）。</li>
 * <li>jasypt-spring-boot 3.x 默认 PBEWITHHMACSHA512ANDAES_256：
 *     盐 16 字节，IV 16 字节，PBKDF2WithHmacSHA512 派生 256 位密钥。</li>
 * </ul>
 * 迭代次数两者默认均为 1000。
 */
public enum JasyptAlgorithm {

    MD5_DES("PBEWithMD5AndDES（旧版默认）", "PBEWithMD5AndDES", "PBEWithMD5AndDES", 8, 0),
    HMAC_SHA512_AES256("PBEWithHmacSHA512AndAES_256（3.x 默认）",
            "PBKDF2WithHmacSHA512", "PBEWITHHMACSHA512ANDAES_256", 16, 16);

    /** 界面展示名 */
    private final String display;
    /** 密钥派生用的 JCA SecretKeyFactory 算法名 */
    private final String keyFactoryName;
    /** 透传给 jasypt SimplePBEConfig.setAlgorithm 的算法名 */
    private final String jasyptName;
    /** 盐长度（字节），嵌在密文最前 */
    private final int saltLength;
    /** IV 长度（字节），0 表示无独立 IV */
    private final int ivLength;

    JasyptAlgorithm(String display, String keyFactoryName, String jasyptName,
                    int saltLength, int ivLength) {
        this.display = display;
        this.keyFactoryName = keyFactoryName;
        this.jasyptName = jasyptName;
        this.saltLength = saltLength;
        this.ivLength = ivLength;
    }

    public String getDisplay() {
        return display;
    }

    public String getKeyFactoryName() {
        return keyFactoryName;
    }

    public String getJasyptName() {
        return jasyptName;
    }

    public int getSaltLength() {
        return saltLength;
    }

    public int getIvLength() {
        return ivLength;
    }

    @Override
    public String toString() {
        return display;
    }
}
