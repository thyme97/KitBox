package com.kitbox.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 手写 PBKDF2-HMAC-SHA256（RFC 2898 / RFC 8018）。
 * 不依赖 JCE 的 SecretKeyFactory，确保在所有 JDK 上行为一致、可测试。
 */
public final class Pbkdf2 {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final int HASH_LEN = 32;

    private Pbkdf2() {
    }

    /**
     * @param password   口令字符数组
     * @param salt       盐值
     * @param iterations 迭代次数（须 >= 1）
     * @param dkLenBits  派生密钥长度（bit，须为 8 的倍数）
     */
    public static byte[] derive(char[] password, byte[] salt, int iterations, int dkLenBits) {
        if (password == null || salt == null || iterations < 1 || dkLenBits <= 0 || dkLenBits % 8 != 0) {
            throw new IllegalArgumentException("PBKDF2 参数非法");
        }
        int dkLen = dkLenBits / 8;
        byte[] pwdBytes = toBytes(password);
        int blocks = (dkLen + HASH_LEN - 1) / HASH_LEN;
        byte[] result = new byte[blocks * HASH_LEN];
        Mac mac = createMac(pwdBytes);

        byte[] blockIndex = new byte[4];
        for (int block = 1; block <= blocks; block++) {
            blockIndex[0] = (byte) (block >>> 24);
            blockIndex[1] = (byte) (block >>> 16);
            blockIndex[2] = (byte) (block >>> 8);
            blockIndex[3] = (byte) block;
            // U1 = PRF(P, S || INT(block))
            mac.reset();
            mac.update(salt);
            mac.update(blockIndex);
            byte[] u = mac.doFinal();
            byte[] t = u.clone();
            for (int i = 1; i < iterations; i++) {
                mac.reset();
                u = mac.doFinal(u);
                for (int j = 0; j < HASH_LEN; j++) {
                    t[j] ^= u[j];
                }
            }
            System.arraycopy(t, 0, result, (block - 1) * HASH_LEN, HASH_LEN);
        }

        byte[] out = new byte[dkLen];
        System.arraycopy(result, 0, out, 0, dkLen);
        return out;
    }

    private static Mac createMac(byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(key, HMAC_ALG));
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("初始化 HmacSHA256 失败", e);
        }
    }

    /** 口令字符按 UTF-8 编码为字节。 */
    private static byte[] toBytes(char[] chars) {
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
