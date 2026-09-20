package com.kitbox.crypto.model;

import com.kitbox.util.HexUtils;

import java.nio.charset.StandardCharsets;

/**
 * 明文/二进制内容的输入输出格式：文本(UTF-8) 或 Hex。
 * 用于把二进制数据（如 SM4/AES 加密的原始字节）以 Hex 形式直接输入输出。
 */
public enum ContentFormat {

    TEXT("文本(UTF-8)") {
        @Override
        public byte[] decode(String text) {
            return DataEncoding.utf8(text);
        }

        @Override
        public String encode(byte[] data) {
            return DataEncoding.utf8(data);
        }
    },
    HEX("Hex") {
        @Override
        public byte[] decode(String text) {
            return HexUtils.decode(text);
        }

        @Override
        public String encode(byte[] data) {
            return HexUtils.encode(data);
        }
    };

    private final String display;

    ContentFormat(String display) {
        this.display = display;
    }

    /** 文本 → 原始字节（HEX 会校验格式） */
    public abstract byte[] decode(String text);

    /** 原始字节 → 文本 */
    public abstract String encode(byte[] data);

    public String getDisplay() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }

    /** 判断字节序列按 UTF-8 还原后是否全部为可打印内容（含中文，无控制字符）。 */
    public static boolean isPrintableUtf8(byte[] data) {
        try {
            String text = new String(data, StandardCharsets.UTF_8);
            // 若包含 UTF-8 解码替换符说明不是合法 UTF-8
            if (text.indexOf('\uFFFD') >= 0) {
                return false;
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < 0x20 || c == 0x7F) {
                    return false;
                }
            }
            return data.length > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
