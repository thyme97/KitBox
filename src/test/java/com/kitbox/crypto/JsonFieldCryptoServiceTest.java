package com.kitbox.crypto;

import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.Sm2CipherMode;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.keystore.KeyEntry;
import com.kitbox.keystore.KeyEntryType;
import com.kitbox.keystore.KeyStoreCodec;
import com.kitbox.util.KeyCodec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFieldCryptoServiceTest {

    private static final byte[] KEY = new byte[16];
    private static final byte[] KEY2 = new byte[16];

    static {
        Security.addProvider(new BouncyCastleProvider());
        new java.security.SecureRandom().nextBytes(KEY);
        new java.security.SecureRandom().nextBytes(KEY2);
    }

    private JsonFieldCryptoService.FieldValueCipher cipher() {
        return fieldCipherWithKey(KEY, SymmetricAlgorithm.SM4);
    }

    @Test
    void encryptDecryptRoundtripNestedAndWildcard() throws Exception {
        String json = "{\"name\":\"张三\",\"data\":{\"idCard\":\"110101199001011234\",\"phone\":\"13800138000\"},"
                + "\"list\":[{\"phone\":\"13900139000\"},{\"phone\":\"13700137000\"}],"
                + "\"extra\":\"保持不变\"}";
        List<String> paths = Arrays.asList("$.data.idCard", "$.data.phone", "$.list[*].phone");

        String encrypted = JsonFieldCryptoService.process(json, paths, true, cipher(), false);
        assertTrue(encrypted.contains("保持不变"));
        assertTrue(encrypted.contains("\"name\":\"张三\""));
        assertTrue(!encrypted.contains("110101199001011234"));
        assertTrue(!encrypted.contains("13800138000"));

        String decrypted = JsonFieldCryptoService.process(encrypted, paths, false, cipher(), true);
        assertTrue(decrypted.contains("110101199001011234"));
        assertTrue(decrypted.contains("13900139000"));
        assertTrue(decrypted.contains("13700137000"));
    }

    @Test
    void arrayIndexPath() throws Exception {
        String json = "{\"a\":[\"v0\",\"v1\",\"v2\"]}";
        String encrypted = JsonFieldCryptoService.process(json,
                java.util.Collections.singletonList("$.a[1]"), true, cipher(), false);
        assertTrue(encrypted.contains("v0") && encrypted.contains("v2"));
        String decrypted = JsonFieldCryptoService.process(encrypted,
                java.util.Collections.singletonList("$.a[1]"), false, cipher(), true);
        assertTrue(decrypted.contains("v1"));
    }

    @Test
    void missingPathFails() {
        assertThrows(CryptoException.class, () -> JsonFieldCryptoService.process(
                "{\"a\":\"b\"}", java.util.Collections.singletonList("$.x.y"), true, cipher(), false));
    }

    @Test
    void badJsonFails() {
        assertThrows(CryptoException.class, () -> JsonFieldCryptoService.process(
                "not json", java.util.Collections.singletonList("$.a"), true, cipher(), false));
    }

    /** 验证密钥库条目与 JSON 加解密的协作（真实使用路径）。 */
    @Test
    void interopWithKeyStoreEntry() throws Exception {
        // 对称条目直接用于 JSON 加密
        KeyEntry sym = new KeyEntry("id2", "测试", "aes", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(KEY), "", "2026-01-01T00:00:00");
        byte[] key = KeyStoreCodec.symmetricKeyBytes(sym);
        String encrypted = JsonFieldCryptoService.process("{\"msg\":\"hello\"}",
                java.util.Collections.singletonList("$.msg"), true,
                fieldCipherWithKey(key), false);
        String decrypted = JsonFieldCryptoService.process(encrypted,
                java.util.Collections.singletonList("$.msg"), false, fieldCipherWithKey(key), true);
        assertTrue(decrypted.contains("hello"));

        // SM2 密钥对条目可正确解析出公私钥参数
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        KeyEntry pair = new KeyEntry("id", "测试", "sm2", KeyEntryType.SM2_KEYPAIR,
                KeyStoreCodec.buildKeyPairValue(kp.getPublic(), kp.getPrivate()), "", "2026-01-01T00:00:00");
        org.bouncycastle.crypto.params.ECPublicKeyParameters pub = KeyStoreCodec.sm2PublicKey(pair);
        org.bouncycastle.crypto.params.ECPrivateKeyParameters priv = KeyStoreCodec.sm2PrivateKey(pair);
        byte[] plain = "你好".getBytes(StandardCharsets.UTF_8);
        byte[] ct = AsymmetricService.sm2Encrypt(plain, pub, Sm2CipherMode.C1C3C2);
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                plain, AsymmetricService.sm2Decrypt(ct, priv, Sm2CipherMode.C1C3C2));
    }

    private JsonFieldCryptoService.FieldValueCipher fieldCipherWithKey(byte[] key) {
        return fieldCipherWithKey(key, SymmetricAlgorithm.AES);
    }

    /** 字段值：Base64(iv(12B) ‖ GCM 密文)，与 JsonFieldPanel 的实现保持一致。 */
    private JsonFieldCryptoService.FieldValueCipher fieldCipherWithKey(byte[] key, SymmetricAlgorithm algo) {
        return new JsonFieldCryptoService.FieldValueCipher() {
            @Override
            public String encrypt(String plain) throws CryptoException {
                byte[] iv = new byte[12];
                new java.security.SecureRandom().nextBytes(iv);
                byte[] ct = SymmetricService.encrypt(DataEncoding.utf8(plain), key, algo, CryptoMode.GCM,
                        Padding.NO_PADDING, iv, 128);
                byte[] out = new byte[iv.length + ct.length];
                System.arraycopy(iv, 0, out, 0, iv.length);
                System.arraycopy(ct, 0, out, iv.length, ct.length);
                return DataEncoding.BASE64.encode(out);
            }

            @Override
            public String decrypt(String cipherText) throws CryptoException {
                byte[] packed = DataEncoding.BASE64.decode(cipherText.trim());
                byte[] iv = Arrays.copyOfRange(packed, 0, 12);
                byte[] ct = Arrays.copyOfRange(packed, 12, packed.length);
                return DataEncoding.utf8(SymmetricService.decrypt(ct, key, algo, CryptoMode.GCM,
                        Padding.NO_PADDING, iv, 128));
            }
        };
    }
}
