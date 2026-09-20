package com.kitbox.crypto;

import com.kitbox.crypto.model.DigestAlgorithm;
import com.kitbox.crypto.model.HmacAlgorithm;
import com.kitbox.util.HexUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DigestServiceTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void md5Vector() throws CryptoException {
        assertEquals("900150983cd24fb0d6963f7d28e17f72",
                HexUtils.encode(DigestService.digest(utf8("abc"), DigestAlgorithm.MD5)));
    }

    @Test
    void sha256Vector() throws CryptoException {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                HexUtils.encode(DigestService.digest(utf8("abc"), DigestAlgorithm.SHA256)));
    }

    @Test
    void sm3Vectors() throws CryptoException {
        // GB/T 32907 / GM/T 0004 标准向量
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0",
                HexUtils.encode(DigestService.digest(utf8("abc"), DigestAlgorithm.SM3)));
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            longInput.append("abcd");
        }
        assertEquals("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732",
                HexUtils.encode(DigestService.digest(utf8(longInput.toString()), DigestAlgorithm.SM3)));
    }

    @Test
    void hmacSha256Vector() throws CryptoException {
        byte[] mac = DigestService.hmac(utf8("The quick brown fox jumps over the lazy dog"),
                utf8("key"), HmacAlgorithm.HMAC_SHA256);
        assertEquals("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8",
                HexUtils.encode(mac));
        assertTrue(DigestService.constantTimeEquals(mac, mac));
    }

    @Test
    void hmacSm3Roundtrip() throws CryptoException {
        byte[] key = utf8("hmac-sm3-key");
        byte[] data = utf8("国密 HMAC 测试");
        byte[] mac = DigestService.hmac(data, key, HmacAlgorithm.HMAC_SM3);
        assertEquals(32, mac.length);
        assertTrue(DigestService.constantTimeEquals(mac, DigestService.hmac(data, key, HmacAlgorithm.HMAC_SM3)));
        assertFalse(DigestService.constantTimeEquals(mac, DigestService.hmac(utf8("other"), key,
                HmacAlgorithm.HMAC_SM3)));
    }
}
