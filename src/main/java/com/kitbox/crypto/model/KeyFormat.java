package com.kitbox.crypto.model;

import com.kitbox.util.HexUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 密钥文本输入格式：明文 / Base64 / Hex / PEM。
 */
public enum KeyFormat {

    PLAIN("明文") {
        @Override
        public byte[] decode(String text) {
            return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
        }
    },
    BASE64("Base64") {
        @Override
        public byte[] decode(String text) {
            String cleaned = text == null ? "" : text.replaceAll("\\s+", "");
            return Base64.getDecoder().decode(cleaned);
        }
    },
    HEX("Hex") {
        @Override
        public byte[] decode(String text) {
            return HexUtils.decode(text);
        }
    },
    PEM("PEM") {
        @Override
        public byte[] decode(String text) {
            return com.kitbox.util.PemUtils.readPemContent(text);
        }
    };

    private final String display;

    KeyFormat(String display) {
        this.display = display;
    }

    /** 将文本还原为原始字节（PEM 会剥离头尾并做 Base64 解码）。 */
    public abstract byte[] decode(String text);

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }
}
