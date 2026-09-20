package com.kitbox.util;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.AsymmetricService;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.crypto.model.RsaPadding;
import com.kitbox.crypto.model.Sm2CipherMode;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyCodecTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void rsaPemRoundtrip() throws CryptoException {
        KeyPair kp = KeyCodec.generateRsaKeyPair(2048);
        String pubPem = KeyCodec.toPem(kp.getPublic());
        String privPem = KeyCodec.toPem(kp.getPrivate());
        assertTrue(pubPem.startsWith("-----BEGIN PUBLIC KEY-----"));
        assertTrue(privPem.startsWith("-----BEGIN PRIVATE KEY-----"));

        PublicKey pub = KeyCodec.parseRsaPublicKey(pubPem, KeyFormat.PEM);
        PrivateKey priv = KeyCodec.parseRsaPrivateKey(privPem, KeyFormat.PEM);
        assertArrayEquals(kp.getPublic().getEncoded(), pub.getEncoded());
        assertArrayEquals(kp.getPrivate().getEncoded(), priv.getEncoded());
    }

    @Test
    void rsaPkcs1PemImport() throws Exception {
        KeyPair kp = KeyCodec.generateRsaKeyPair(2048);
        // 用 BC 把 PKCS8 转成 PKCS1 PEM 导出，验证导入兼容性
        String pkcs8Base64 = KeyCodec.toBase64(kp.getPrivate());
        // PKCS1 PEM 由 BC ASN.1 生成
        org.bouncycastle.asn1.pkcs.RSAPrivateKey pkcs1 = org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo.getInstance(kp.getPrivate().getEncoded()).parsePrivateKey());
        String pkcs1Pem = PemUtils.buildPem("RSA PRIVATE KEY", pkcs1.getEncoded("DER"));

        PrivateKey parsed = KeyCodec.parseRsaPrivateKey(pkcs1Pem, KeyFormat.PEM);
        assertEquals(pkcs8Base64, KeyCodec.toBase64(parsed));
    }

    @Test
    void sm2KeyRoundtripAndRawFormats() throws CryptoException {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        String pubB64 = KeyCodec.toBase64(kp.getPublic());
        String privB64 = KeyCodec.toBase64(kp.getPrivate());

        ECPublicKeyParameters pubParams = KeyCodec.parseSm2PublicKey(pubB64, KeyFormat.BASE64);
        ECPrivateKeyParameters privParams = KeyCodec.parseSm2PrivateKey(privB64, KeyFormat.BASE64);
        assertNotNull(pubParams);
        assertNotNull(privParams);

        // 轻量参数 → JCA Base64（规范化），应与原始编码一致
        assertEquals(pubB64, KeyCodec.toBase64JcaPublic(pubParams));
        assertEquals(privB64, KeyCodec.toBase64JcaPrivate(privParams));

        // 裸点公钥 04|X|Y 与 32 字节裸私钥
        byte[] rawPoint = pubParams.getQ().getEncoded(false);
        String rawPointHex = HexUtils.encode(rawPoint);
        ECPublicKeyParameters pubFromRaw = KeyCodec.parseSm2PublicKey(rawPointHex, KeyFormat.HEX);
        assertArrayEquals(rawPoint, pubFromRaw.getQ().getEncoded(false));

        byte[] rawD = privParams.getD().toByteArray();
        byte[] d32 = new byte[32];
        System.arraycopy(rawD, rawD.length - 32, d32, 0, 32);
        ECPrivateKeyParameters privFromRaw = KeyCodec.parseSm2PrivateKey(HexUtils.encode(d32), KeyFormat.HEX);
        assertEquals(privParams.getD(), privFromRaw.getD());
    }

    @Test
    void sm2EncryptDecryptWithRawPoint() throws CryptoException {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        byte[] rawPoint = ((org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic()).getQ().getEncoded(false);
        ECPublicKeyParameters pub = KeyCodec.parseSm2PublicKey(HexUtils.encode(rawPoint), KeyFormat.HEX);
        ECPrivateKeyParameters priv = KeyCodec.parseSm2PrivateKey(KeyCodec.toBase64(kp.getPrivate()),
                KeyFormat.BASE64);
        byte[] data = utf8("使用裸点公钥加密");
        byte[] cipher = AsymmetricService.sm2Encrypt(data, pub, Sm2CipherMode.C1C3C2);
        assertArrayEquals(data, AsymmetricService.sm2Decrypt(cipher, priv, Sm2CipherMode.C1C3C2));
    }

    @Test
    void sm2RawHexViewsMatchOriginalKeys() throws CryptoException {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        String pubB64 = KeyCodec.toBase64(kp.getPublic());
        String privB64 = KeyCodec.toBase64(kp.getPrivate());

        String rawPub = KeyCodec.sm2PublicRawHex(pubB64);
        assertEquals(130, rawPub.length());
        assertTrue(rawPub.startsWith("04"));

        String rawPriv = KeyCodec.sm2PrivateRawHex(privB64);
        assertEquals(64, rawPriv.length());

        ECPublicKeyParameters pubFromRaw = KeyCodec.parseSm2PublicKey(rawPub, KeyFormat.HEX);
        ECPrivateKeyParameters privFromRaw = KeyCodec.parseSm2PrivateKey(rawPriv, KeyFormat.HEX);
        assertEquals(KeyCodec.parseSm2PublicKey(pubB64, KeyFormat.BASE64).getQ(), pubFromRaw.getQ());
        assertEquals(KeyCodec.parseSm2PrivateKey(privB64, KeyFormat.BASE64).getD(), privFromRaw.getD());
    }

    @Test
    void sm2BarePrivateKeyAcceptsLeadingZeroPadding() throws Exception {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        ECPrivateKeyParameters params =
                KeyCodec.parseSm2PrivateKey(KeyCodec.toBase64(kp.getPrivate()), KeyFormat.BASE64);
        byte[] d32 = new byte[32];
        byte[] src = params.getD().toByteArray();
        System.arraycopy(src, src.length - 32, d32, 0, 32);

        // 部分工具导出裸私钥时补符号位成 33 字节（前导 00），应可解析
        byte[] padded = new byte[33];
        System.arraycopy(d32, 0, padded, 1, 32);
        assertEquals(params.getD(), KeyCodec.parseSm2PrivateKey(HexUtils.encode(padded), KeyFormat.HEX).getD());
        // 格式误选 Base64 时也应通过 Hex 回退解析成功
        assertEquals(params.getD(), KeyCodec.parseSm2PrivateKey(HexUtils.encode(padded), KeyFormat.BASE64).getD());
    }

    @Test
    void sm2RawPointHexAcceptedEvenWhenFormatIsBase64() throws Exception {
        KeyPair kp = KeyCodec.generateSm2KeyPair();
        String rawPubHex = KeyCodec.sm2PublicRawHex(KeyCodec.toBase64(kp.getPublic()));
        ECPublicKeyParameters viaHex = KeyCodec.parseSm2PublicKey(rawPubHex, KeyFormat.HEX);
        ECPublicKeyParameters viaB64 = KeyCodec.parseSm2PublicKey(rawPubHex, KeyFormat.BASE64);
        assertEquals(viaHex.getQ(), viaB64.getQ());
    }

    @Test
    void sm2PublicKeyOffCurveGetsClearError() {
        // (1,1) 不在 sm2p256v1 曲线上（b ≠ 3），应给出明确诊断而非笼统“无法解析”
        byte[] bad = new byte[65];
        bad[0] = 0x04;
        bad[1] = 0x01;
        bad[33] = 0x01;
        CryptoException e = assertThrows(CryptoException.class,
                () -> KeyCodec.parseSm2PublicKey(HexUtils.encode(bad), KeyFormat.HEX));
        assertTrue(e.getMessage().contains("不在 SM2"));
    }
}
