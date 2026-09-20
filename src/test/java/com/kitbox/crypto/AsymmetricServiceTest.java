package com.kitbox.crypto;

import com.kitbox.crypto.model.RsaPadding;
import com.kitbox.crypto.model.Sm2CipherMode;
import com.kitbox.util.KeyCodec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsymmetricServiceTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void rsaRoundtripAllPaddings() throws Exception {
        KeyPair kp = KeyCodec.generateRsaKeyPair(2048);
        byte[] data = utf8("RSA 加解密往返测试 RSA roundtrip test");
        for (RsaPadding padding : RsaPadding.values()) {
            byte[] cipher = AsymmetricService.rsaEncrypt(data, kp.getPublic(), padding);
            byte[] plain = AsymmetricService.rsaDecrypt(cipher, kp.getPrivate(), padding);
            assertArrayEquals(data, plain, padding + " 往返失败");
        }
    }

    @Test
    void rsaSegmentedLongText() throws Exception {
        KeyPair kp = KeyCodec.generateRsaKeyPair(2048);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("超长文本分段加解密验证-").append(i).append(";");
        }
        byte[] data = utf8(sb.toString());
        assertTrue(data.length > 245, "明文应超过单块上限");

        byte[] cipher = AsymmetricService.rsaEncrypt(data, kp.getPublic(), RsaPadding.PKCS1);
        assertEquals(0, cipher.length % 256);
        byte[] plain = AsymmetricService.rsaDecrypt(cipher, kp.getPrivate(), RsaPadding.PKCS1);
        assertArrayEquals(data, plain);
    }

    @Test
    void rsaWrongKeyFails() throws Exception {
        KeyPair kp1 = KeyCodec.generateRsaKeyPair(2048);
        KeyPair kp2 = KeyCodec.generateRsaKeyPair(2048);
        byte[] cipher = AsymmetricService.rsaEncrypt(utf8("x"), kp1.getPublic(), RsaPadding.PKCS1);
        assertThrows(CryptoException.class, () -> AsymmetricService.rsaDecrypt(cipher, kp2.getPrivate(),
                RsaPadding.PKCS1));
    }

    @Test
    void sm2RoundtripBothModes() throws Exception {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        org.bouncycastle.jce.interfaces.ECPublicKey pubJca = (org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic();
        org.bouncycastle.jce.interfaces.ECPrivateKey privJca = (org.bouncycastle.jce.interfaces.ECPrivateKey) kp.getPrivate();
        org.bouncycastle.crypto.params.ECPublicKeyParameters pub =
                new org.bouncycastle.crypto.params.ECPublicKeyParameters(pubJca.getQ(), KeyCodec.SM2_DOMAIN);
        org.bouncycastle.crypto.params.ECPrivateKeyParameters priv =
                new org.bouncycastle.crypto.params.ECPrivateKeyParameters(privJca.getD(), KeyCodec.SM2_DOMAIN);

        byte[] data = utf8("SM2 国密加解密测试，含中文与 ASCII 123。");
        for (Sm2CipherMode mode : Sm2CipherMode.values()) {
            byte[] cipher = AsymmetricService.sm2Encrypt(data, pub, mode);
            byte[] plain = AsymmetricService.sm2Decrypt(cipher, priv, mode);
            assertArrayEquals(data, plain, mode + " 往返失败");
        }
        // 用错误排列方式解密应失败
        byte[] cipher = AsymmetricService.sm2Encrypt(data, pub, Sm2CipherMode.C1C3C2);
        assertThrows(CryptoException.class, () -> AsymmetricService.sm2Decrypt(cipher, priv,
                Sm2CipherMode.C1C2C3));
    }

    @Test
    void rsaSignVerify() throws Exception {
        KeyPair kp = KeyCodec.generateRsaKeyPair(2048);
        byte[] data = utf8("sign me");
        byte[] sig = SignatureService.rsaSign(data, kp.getPrivate(), "SHA256withRSA");
        assertTrue(SignatureService.rsaVerify(data, sig, kp.getPublic(), "SHA256withRSA"));
        assertFalse(SignatureService.rsaVerify(utf8("sign me tampered"), sig, kp.getPublic(),
                "SHA256withRSA"));
    }

    @Test
    void sm2SignVerifyWithId() throws Exception {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        org.bouncycastle.jce.interfaces.ECPublicKey pubJca = (org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic();
        org.bouncycastle.jce.interfaces.ECPrivateKey privJca = (org.bouncycastle.jce.interfaces.ECPrivateKey) kp.getPrivate();
        org.bouncycastle.crypto.params.ECPublicKeyParameters pub =
                new org.bouncycastle.crypto.params.ECPublicKeyParameters(pubJca.getQ(), KeyCodec.SM2_DOMAIN);
        org.bouncycastle.crypto.params.ECPrivateKeyParameters priv =
                new org.bouncycastle.crypto.params.ECPrivateKeyParameters(privJca.getD(), KeyCodec.SM2_DOMAIN);

        byte[] data = utf8("SM2 签名测试");
        String id = "1234567812345678";
        byte[] sig = SignatureService.sm2Sign(data, priv, id);
        assertTrue(SignatureService.sm2Verify(data, sig, pub, id));
        assertFalse(SignatureService.sm2Verify(data, sig, pub, "8888888812345678"));
        assertFalse(SignatureService.sm2Verify(utf8("tampered"), sig, pub, id));
    }

    @Test
    void hmacSignVerify() throws CryptoException {
        byte[] key = utf8("hmac-key");
        byte[] data = utf8("data");
        byte[] sig = SignatureService.hmacSign(data, key, com.kitbox.crypto.model.HmacAlgorithm.HMAC_SHA256);
        assertTrue(SignatureService.hmacVerify(data, sig, key,
                com.kitbox.crypto.model.HmacAlgorithm.HMAC_SHA256));
        assertFalse(SignatureService.hmacVerify(utf8("other"), sig, key,
                com.kitbox.crypto.model.HmacAlgorithm.HMAC_SHA256));
    }
}
