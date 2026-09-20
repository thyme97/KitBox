package com.kitbox.crypto;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 纯编码工具：Base64（标准/URL 安全/换行）、Hex、URL。
 */
public final class EncodingService {

    private EncodingService() {
    }

    public static String base64Encode(byte[] data, boolean urlSafe, boolean withLineBreaks) {
        if (withLineBreaks && !urlSafe) {
            byte[] encoded = Base64.getMimeEncoder(76, "\r\n".getBytes(StandardCharsets.US_ASCII)).encode(data);
            return new String(encoded, StandardCharsets.US_ASCII);
        }
        Base64.Encoder enc = urlSafe ? Base64.getUrlEncoder().withoutPadding() : Base64.getEncoder();
        String s = enc.encodeToString(data);
        if (withLineBreaks) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i += 76) {
                if (i > 0) {
                    sb.append("\r\n");
                }
                sb.append(s, i, Math.min(i + 76, s.length()));
            }
            return sb.toString();
        }
        return s;
    }

    public static byte[] base64Decode(String text, boolean urlSafe) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", "");
        Base64.Decoder dec = urlSafe ? Base64.getUrlDecoder() : Base64.getDecoder();
        return dec.decode(cleaned);
    }

    public static String urlEncode(String text, String charset) throws CryptoException {
        try {
            return URLEncoder.encode(text, charset);
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("URL 编码失败：" + e.getMessage(), e);
        }
    }

    public static String urlDecode(String text, String charset) throws CryptoException {
        try {
            return URLDecoder.decode(text, charset);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("URL 解码失败：内容不是合法的编码串");
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException("URL 解码失败：" + e.getMessage(), e);
        }
    }
}
