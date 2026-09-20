package com.kitbox.crypto;

import com.google.gson.JsonObject;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.util.Gsons;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 文件加解密服务（自有格式，AES/SM4-GCM 整文件加密）。
 * <p>
 * 文件结构：{@code MAGIC(4B) "CBFX" | 版本(1B) | 元数据长度(2B) | 元数据JSON(UTF-8) | 密文}
 * 元数据：{@code {"algo":"AES","iv":"<Base64>","tag":128}}
 */
public final class FileCryptoService {

    private static final byte[] MAGIC = {'C', 'B', 'F', 'X'};
    private static final byte VERSION = 1;

    private FileCryptoService() {
    }

    public static byte[] encryptFile(byte[] plain, byte[] key, SymmetricAlgorithm algo) throws CryptoException {
        if (!algo.supportGcm()) {
            throw new CryptoException("文件加密仅支持 AES / SM4");
        }
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);

        JsonObject meta = new JsonObject();
        meta.addProperty("algo", algo.name());
        meta.addProperty("iv", java.util.Base64.getEncoder().encodeToString(iv));
        meta.addProperty("tag", SymmetricService.DEFAULT_GCM_TAG_BITS);
        byte[] metaBytes = Gsons.compact().toJson(meta).getBytes(StandardCharsets.UTF_8);
        if (metaBytes.length > 0xFFFF) {
            throw new CryptoException("元数据过长");
        }

        byte[] cipher = SymmetricService.encrypt(plain, key, algo, com.kitbox.crypto.model.CryptoMode.GCM,
                com.kitbox.crypto.model.Padding.NO_PADDING, iv, SymmetricService.DEFAULT_GCM_TAG_BITS);

        ByteArrayOutputStream out = new ByteArrayOutputStream(MAGIC.length + 3 + metaBytes.length + cipher.length);
        out.write(MAGIC, 0, MAGIC.length);
        out.write(VERSION);
        out.write((metaBytes.length >>> 8) & 0xFF);
        out.write(metaBytes.length & 0xFF);
        out.write(metaBytes, 0, metaBytes.length);
        out.write(cipher, 0, cipher.length);
        return out.toByteArray();
    }

    public static byte[] decryptFile(byte[] fileBytes, byte[] key) throws CryptoException {
        if (fileBytes == null || fileBytes.length < MAGIC.length + 3) {
            throw new CryptoException("文件格式不正确（长度不足）");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (fileBytes[i] != MAGIC[i]) {
                throw new CryptoException("文件格式不正确：不是本工具加密的文件");
            }
        }
        if (fileBytes[MAGIC.length] != VERSION) {
            throw new CryptoException("文件版本不支持：" + fileBytes[MAGIC.length]);
        }
        int metaLen = ((fileBytes[MAGIC.length + 1] & 0xFF) << 8) | (fileBytes[MAGIC.length + 2] & 0xFF);
        int offset = MAGIC.length + 3;
        if (fileBytes.length < offset + metaLen) {
            throw new CryptoException("文件格式不正确（元数据不完整）");
        }
        JsonObject meta;
        try {
            meta = Gsons.compact().fromJson(
                    new String(fileBytes, offset, metaLen, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            throw new CryptoException("元数据解析失败：" + e.getMessage());
        }
        if (meta == null || !meta.has("algo") || !meta.has("iv")) {
            throw new CryptoException("元数据不完整");
        }
        SymmetricAlgorithm algo;
        try {
            algo = SymmetricAlgorithm.valueOf(meta.get("algo").getAsString());
        } catch (IllegalArgumentException e) {
            throw new CryptoException("不支持的算法：" + meta.get("algo").getAsString());
        }
        byte[] iv = java.util.Base64.getDecoder().decode(meta.get("iv").getAsString());
        int tag = meta.has("tag") ? meta.get("tag").getAsInt() : SymmetricService.DEFAULT_GCM_TAG_BITS;
        int cipherOffset = offset + metaLen;
        byte[] cipher = new byte[fileBytes.length - cipherOffset];
        System.arraycopy(fileBytes, cipherOffset, cipher, 0, cipher.length);
        return SymmetricService.decrypt(cipher, key, algo, com.kitbox.crypto.model.CryptoMode.GCM,
                com.kitbox.crypto.model.Padding.NO_PADDING, iv, tag);
    }
}
