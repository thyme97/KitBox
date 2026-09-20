package com.kitbox.crypto;

import com.kitbox.crypto.model.DataEncoding;

/**
 * 固定报文格式加解密服务。
 * <p>
 * 报文格式：{@code 前缀 + 编码(密文) + 后缀}，如 {@code DATA|<Base64>|END}。
 * 前后缀均可为空；底层加解密由上层注入（复用对称服务）。
 */
public final class MessageFormatService {

    private MessageFormatService() {
    }

    /** 文本加解密器（由上层基于对称服务实现）。 */
    public interface TextCipher {
        byte[] encrypt(byte[] plain) throws CryptoException;

        byte[] decrypt(byte[] cipherBytes) throws CryptoException;
    }

    /** 加密：明文 → 密文 → 按模板包裹为报文。 */
    public static String encrypt(String plaintext, String prefix, String suffix,
                                 DataEncoding encoding, TextCipher cipher) throws CryptoException {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new CryptoException("明文为空");
        }
        byte[] ct = cipher.encrypt(DataEncoding.utf8(plaintext));
        return nvl(prefix) + encoding.encode(ct) + nvl(suffix);
    }

    /** 解密：按模板提取密文 → 解码 → 还原明文。 */
    public static String decrypt(String message, String prefix, String suffix,
                                 DataEncoding encoding, TextCipher cipher) throws CryptoException {
        if (message == null || message.isEmpty()) {
            throw new CryptoException("报文为空");
        }
        String content = extract(message, prefix, suffix);
        byte[] ct;
        try {
            ct = encoding.decode(content);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密文不是合法的 " + encoding.getDisplay() + " 内容：" + e.getMessage());
        }
        return DataEncoding.utf8(cipher.decrypt(ct));
    }

    /** 按前缀首次出现与后缀最后一次出现提取密文段。 */
    private static String extract(String message, String prefix, String suffix) throws CryptoException {
        int start = 0;
        String p = nvl(prefix);
        String s = nvl(suffix);
        if (!p.isEmpty()) {
            int idx = message.indexOf(p);
            if (idx < 0) {
                throw new CryptoException("报文中未找到前缀：" + p);
            }
            start = idx + p.length();
        }
        int end = message.length();
        if (!s.isEmpty()) {
            int idx = message.lastIndexOf(s);
            if (idx < start) {
                throw new CryptoException("报文中未找到后缀：" + s);
            }
            end = idx;
        }
        if (start > end) {
            throw new CryptoException("报文格式不正确：前缀出现在后缀之后");
        }
        String content = message.substring(start, end).trim();
        if (content.isEmpty()) {
            throw new CryptoException("报文中未提取到密文内容");
        }
        return content;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
