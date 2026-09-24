package com.kitbox.crypto;

import com.kitbox.crypto.model.JasyptAlgorithm;
import com.kitbox.crypto.model.JasyptIvGenerator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Jasypt 兼容性测试。
 * <p>
 * 金标准密文由真实 jasypt 1.9.3（StandardPBEStringEncryptor，AES 变体附加
 * RandomIvGenerator，模拟 jasypt-spring-boot 3.x 默认配置）生成：
 * 口令 kitbox-pass-123，明文 db.password=abc123&amp;!{\n}中文OK。
 */
class JasyptServiceTest {

    private static final char[] PASSWORD = "kitbox-pass-123".toCharArray();
    private static final String PLAIN = "db.password=abc123&!\n中文OK";

    private static final String VEC_MD5_DES = "eClsmUXZ0JAGm12rkLOf6XAQZNutzqrpW0Y7mfq3R4B1BqZS8Amv8Q==";
    private static final String VEC_MD5_DES_B = "rCRpORukXB3VmLT5iypMz2mGiC7bVBAn";
    private static final String VEC_AES = "7YaL70bLQkSxCOyS2KewbPxHPPWM2zm5uoqXncjnHjqVIEyUtLmNRV4w/9zfiZnI6BReU6oiq1Zp7O31n24Z0g==";
    private static final String VEC_AES_B = "fnAn905QqCcNR5Wrzud3LwrbaVWNHHZYrGXqsNZmuc5XKNlQRj+k+bjctdb8wTFM";

    private static final String PLAIN_B = "another-secret";

    @Test
    void decryptLegacyJasyptVector() throws CryptoException {
        assertEquals(PLAIN, JasyptService.decrypt(VEC_MD5_DES, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
        assertEquals(PLAIN_B, JasyptService.decrypt(VEC_MD5_DES_B, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    @Test
    void decryptJasyptSpringBoot3Vector() throws CryptoException {
        assertEquals(PLAIN, JasyptService.decrypt(VEC_AES, PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
        assertEquals(PLAIN_B, JasyptService.decrypt(VEC_AES_B, PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
    }

    @Test
    void decryptAcceptsEncWrapper() throws CryptoException {
        assertEquals(PLAIN, JasyptService.decrypt("ENC(" + VEC_MD5_DES + ")",
                PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
        assertEquals(PLAIN, JasyptService.decrypt("ENC(" + VEC_AES + ")",
                PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
        // 带引号与空白
        assertEquals(PLAIN_B, JasyptService.decrypt("  \"ENC(" + VEC_MD5_DES_B + ")\"\n",
                PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    @Test
    void decryptToleratesLineBreaksInBase64() throws CryptoException {
        String wrapped = VEC_MD5_DES.substring(0, 20) + "\n" + VEC_MD5_DES.substring(20);
        assertEquals(PLAIN, JasyptService.decrypt(wrapped, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    @Test
    void roundTripLegacy() throws CryptoException {
        String cipher = JasyptService.encrypt(PLAIN, PASSWORD, JasyptAlgorithm.MD5_DES,
                JasyptIvGenerator.NO_IV, 1000, false);
        assertEquals(PLAIN, JasyptService.decrypt(cipher, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    @Test
    void roundTripLegacyWithRandomIv() throws CryptoException {
        // 博客同款配置：PBEWithMD5AndDES + RandomIvGenerator（SunJCE 下存储 IV 不参与运算）
        String cipher = JasyptService.encrypt(PLAIN_B, PASSWORD, JasyptAlgorithm.MD5_DES,
                JasyptIvGenerator.RANDOM, 1000, false);
        assertEquals(32, java.util.Base64.getMimeDecoder().decode(cipher).length);
        assertEquals(PLAIN_B, JasyptService.decrypt(cipher, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    @Test
    void decryptEmbeddedIvLayout() throws Exception {
        // 布局 3：盐(8) + 存储IV(8) + 密文，存储 IV 真正参与运算（真正实现内嵌 IV 的 Provider 产物）
        byte[] salt = new byte[8];
        byte[] storedIv = new byte[8];
        java.security.SecureRandom random = new java.security.SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(storedIv);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(pbkdf1Md5(PASSWORD, salt, 1000), 0, 8, "DES"),
                new javax.crypto.spec.IvParameterSpec(storedIv));
        byte[] cipherBytes = cipher.doFinal(PLAIN_B.getBytes(StandardCharsets.UTF_8));
        byte[] all = new byte[16 + cipherBytes.length];
        System.arraycopy(salt, 0, all, 0, 8);
        System.arraycopy(storedIv, 0, all, 8, 8);
        System.arraycopy(cipherBytes, 0, all, 16, cipherBytes.length);
        String base64 = java.util.Base64.getEncoder().encodeToString(all);
        assertEquals(PLAIN_B, JasyptService.decrypt(base64, PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
    }

    /** PKCS#5 PBKDF1（MD5）：DK = MD5(口令 || 盐) 后对结果继续迭代。 */
    private static byte[] pbkdf1Md5(char[] password, byte[] salt, int iterations) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        md.update(new String(password).getBytes(StandardCharsets.US_ASCII));
        md.update(salt);
        byte[] dk = md.digest();
        for (int i = 1; i < iterations; i++) {
            md.reset();
            dk = md.digest(dk);
        }
        return dk;
    }

    @Test
    void roundTripAesWithEncWrapper() throws CryptoException {
        String cipher = JasyptService.encrypt(PLAIN, PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256,
                JasyptIvGenerator.RANDOM, 1000, true);
        assertTrue(cipher.startsWith("ENC(") && cipher.endsWith(")"));
        assertEquals(PLAIN, JasyptService.decrypt(cipher, PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
    }

    @Test
    void encryptedOutputIsBase64OfSaltPlusCipher() throws CryptoException {
        // 旧版：盐 8 字节 + 密文；明文 14 字节 → PKCS5 填充 16 字节 → 24 字节
        String cipher = JasyptService.encrypt(PLAIN_B, PASSWORD, JasyptAlgorithm.MD5_DES,
                JasyptIvGenerator.NO_IV, 1000, false);
        assertEquals(24, java.util.Base64.getMimeDecoder().decode(cipher).length);
        // AES：盐 16 + IV 16 + 密文
        String aes = JasyptService.encrypt(PLAIN_B, PASSWORD, JasyptAlgorithm.HMAC_SHA512_AES256,
                JasyptIvGenerator.RANDOM, 1000, false);
        assertEquals(48, java.util.Base64.getMimeDecoder().decode(aes).length);
    }

    @Test
    void wrongPasswordFails() {
        char[] wrong = "wrong-password".toCharArray();
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt(VEC_MD5_DES, wrong, JasyptAlgorithm.MD5_DES, 1000));
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt(VEC_AES, wrong, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
    }

    @Test
    void invalidInputsFail() {
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt("not-base64!!", PASSWORD, JasyptAlgorithm.MD5_DES, 1000));
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt("QUJD", PASSWORD, JasyptAlgorithm.MD5_DES, 1000)); // 太短
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt(VEC_MD5_DES, new char[0], JasyptAlgorithm.MD5_DES, 1000));
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt(VEC_MD5_DES, PASSWORD, JasyptAlgorithm.MD5_DES, 0));
    }

    @Test
    void nonAsciiPasswordRejectedForLegacyOnly() throws CryptoException {
        char[] chinese = "中文口令".toCharArray();
        // PBES1（PBEWithMD5AndDES）：JCE 拒绝非 ASCII 口令，须提前报错
        assertThrows(CryptoException.class,
                () -> JasyptService.decrypt(VEC_MD5_DES, chinese, JasyptAlgorithm.MD5_DES, 1000));
        assertThrows(CryptoException.class,
                () -> JasyptService.encrypt(PLAIN, chinese, JasyptAlgorithm.MD5_DES,
                        JasyptIvGenerator.NO_IV, 1000, false));
        // PBES2（PBKDF2WithHmacSHA512）：允许非 ASCII 口令
        String cipher = JasyptService.encrypt(PLAIN_B, chinese, JasyptAlgorithm.HMAC_SHA512_AES256,
                JasyptIvGenerator.RANDOM, 1000, false);
        assertEquals(PLAIN_B, JasyptService.decrypt(cipher, chinese, JasyptAlgorithm.HMAC_SHA512_AES256, 1000));
    }

    @Test
    void stripWrapperCoversCommonForms() {
        assertEquals("abc", JasyptService.stripWrapper("ENC(abc)"));
        assertEquals("abc", JasyptService.stripWrapper("  ENC( abc )  "));
        assertEquals("abc", JasyptService.stripWrapper("\"abc\""));
        assertEquals("abc", JasyptService.stripWrapper("abc"));
        // 未闭合的 ENC( 不做剥离（无法确认是包裹还是内容本身）
        assertEquals("ENC(abc", JasyptService.stripWrapper("ENC(abc"));
    }
}
