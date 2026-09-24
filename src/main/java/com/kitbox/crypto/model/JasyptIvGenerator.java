package com.kitbox.crypto.model;

/**
 * Jasypt IV 生成器变体（透传给 jasypt 的 setIvGeneratorClassName）。
 * <p>
 * 仅对 PBES1 旧算法（PBEWithMD5AndDES）有意义；AES_256 变体的布局固定携带随机 IV。
 * <ul>
 * <li>旧默认 NoIvGenerator：密文 = Base64(盐(8) + 密文)，IV 由 PKCS#5 派生，存量 Spring 配置最常见。</li>
 * <li>RandomIvGenerator：密文 = Base64(盐(8) + IV(8) + 密文)。注意在 SunJCE（JDK 自带）下
 *     这个存储 IV 不参与运算（装饰性），真实 IV 仍由口令派生——解密时会自动尝试两种解释。</li>
 * </ul>
 */
public enum JasyptIvGenerator {

    NO_IV("旧默认（无独立 IV）", "org.jasypt.iv.NoIvGenerator"),
    RANDOM("RandomIvGenerator（随机 IV）", "org.jasypt.iv.RandomIvGenerator");

    /** 界面展示名 */
    private final String display;
    /** 透传给 jasypt SimplePBEConfig.setIvGeneratorClassName 的类名 */
    private final String className;

    JasyptIvGenerator(String display, String className) {
        this.display = display;
        this.className = className;
    }

    public String getDisplay() {
        return display;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return display;
    }
}
