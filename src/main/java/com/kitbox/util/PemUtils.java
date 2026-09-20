package com.kitbox.util;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;

/**
 * PEM 格式读写工具（基于 BouncyCastle）。
 */
public final class PemUtils {

    public static final String TYPE_PUBLIC_KEY = "PUBLIC KEY";
    public static final String TYPE_PRIVATE_KEY = "PRIVATE KEY";
    public static final String TYPE_RSA_PUBLIC_KEY = "RSA PUBLIC KEY";
    public static final String TYPE_RSA_PRIVATE_KEY = "RSA PRIVATE KEY";

    private PemUtils() {
    }

    /**
     * 读取 PEM 内容（剥离头尾后的 DER 字节）。
     * 若文本不含 PEM 头，则按 Base64 解析，方便粘贴裸 Base64 密钥。
     */
    public static byte[] readPemContent(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("密钥内容为空");
        }
        String trimmed = text.trim();
        if (!trimmed.contains("-----BEGIN")) {
            String cleaned = trimmed.replaceAll("\\s+", "");
            return Base64.getDecoder().decode(cleaned);
        }
        PemReader reader = new PemReader(new StringReader(trimmed));
        try {
            PemObject obj = reader.readPemObject();
            if (obj == null) {
                throw new IllegalArgumentException("无法解析 PEM 内容");
            }
            return obj.getContent();
        } catch (IOException e) {
            throw new IllegalArgumentException("PEM 解析失败：" + e.getMessage(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** 读取 PEM 类型（如 PUBLIC KEY / RSA PRIVATE KEY），非 PEM 返回 null。 */
    public static String readPemType(String text) {
        if (text == null || !text.contains("-----BEGIN")) {
            return null;
        }
        PemReader reader = new PemReader(new StringReader(text.trim()));
        try {
            PemObject obj = reader.readPemObject();
            return obj == null ? null : obj.getType();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    /** 将 DER 字节包装为 PEM 文本（64 字符换行）。 */
    public static String buildPem(String type, byte[] der) {
        StringWriter sw = new StringWriter();
        PemWriter writer = new PemWriter(sw);
        try {
            writer.writeObject(new PemObject(type, der));
            writer.flush();
        } catch (IOException e) {
            throw new IllegalStateException("PEM 生成失败", e);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        return sw.toString();
    }
}
