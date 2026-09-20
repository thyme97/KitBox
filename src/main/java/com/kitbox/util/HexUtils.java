package com.kitbox.util;

/**
 * 十六进制编解码工具。
 */
public final class HexUtils {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private HexUtils() {
    }

    /** 转小写十六进制字符串。 */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = DIGITS[v >>> 4];
            out[i * 2 + 1] = DIGITS[v & 0x0F];
        }
        return new String(out);
    }

    /** 解析十六进制字符串（大小写均可，忽略空白字符）。 */
    public static byte[] decode(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("十六进制字符串为空");
        }
        String cleaned = hex.replaceAll("\\s+", "");
        if (cleaned.isEmpty()) {
            return new byte[0];
        }
        if (cleaned.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制字符串长度必须为偶数");
        }
        byte[] out = new byte[cleaned.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(cleaned.charAt(i * 2), 16);
            int lo = Character.digit(cleaned.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("包含非法的十六进制字符：" + cleaned.substring(i * 2, i * 2 + 2));
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
