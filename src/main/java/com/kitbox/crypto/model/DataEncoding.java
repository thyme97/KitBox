package com.kitbox.crypto.model;

import com.kitbox.util.HexUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密钥/数据的输入输出编码格式。
 */
public enum DataEncoding {

    BASE64("Base64") {
        @Override
        public String encode(byte[] data) {
            return Base64.getEncoder().encodeToString(data);
        }

        @Override
        public byte[] decode(String text) {
            String cleaned = text == null ? "" : text.replaceAll("\\s+", "");
            return Base64.getDecoder().decode(cleaned);
        }
    },
    HEX("Hex") {
        @Override
        public String encode(byte[] data) {
            return HexUtils.encode(data);
        }

        @Override
        public byte[] decode(String text) {
            return HexUtils.decode(text);
        }
    };

    private final String display;

    DataEncoding(String display) {
        this.display = display;
    }

    public abstract String encode(byte[] data);

    public abstract byte[] decode(String text);

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }

    /** 按 UTF-8 把文本还原为明文字节。 */
    public static byte[] utf8(String text) {
        return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
    }

    /** 明文字节按 UTF-8 还原为文本。 */
    public static String utf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
