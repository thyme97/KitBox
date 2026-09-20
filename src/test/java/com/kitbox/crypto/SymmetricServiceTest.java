package com.kitbox.crypto;

import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.util.HexUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymmetricServiceTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void aesCbcNistVector() throws CryptoException {
        // NIST SP 800-38A AES-128-CBC
        byte[] key = HexUtils.decode("2b7e151628aed2a6abf7158809cf4f3c");
        byte[] iv = HexUtils.decode("000102030405060708090A0B0C0D0E0F");
        byte[] plain = HexUtils.decode(
                "6bc1bee22e409f96e93d7e117393172a"
                        + "ae2d8a571e03ac9c9eb76fac45af8e51"
                        + "30c81c46a35ce411e5fbc1191a0a52ef"
                        + "f69f2445df4f9b17ad2b417be66c3710");
        byte[] expected = HexUtils.decode(
                "7649abac8119b246cee98e9b12e9197d"
                        + "5086cb9b507219ee95db113a917678b2"
                        + "73bed6b8e3c1743b7116e69e22229516"
                        + "3ff1caa1681fac09120eca307586e1a7");
        byte[] cipher = SymmetricService.encrypt(plain, key, SymmetricAlgorithm.AES, CryptoMode.CBC,
                Padding.NO_PADDING, iv, 0);
        assertArrayEquals(expected, cipher);
        byte[] decrypted = SymmetricService.decrypt(cipher, key, SymmetricAlgorithm.AES, CryptoMode.CBC,
                Padding.NO_PADDING, iv, 0);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    void sm4EcbGbVector() throws CryptoException {
        // GB/T 32907-2016 SM4 标准向量
        byte[] key = HexUtils.decode("0123456789abcdeffedcba9876543210");
        byte[] plain = HexUtils.decode("0123456789abcdeffedcba9876543210");
        byte[] expected = HexUtils.decode("681edf34d206965e86b3e94f536e4246");
        byte[] cipher = SymmetricService.encrypt(plain, key, SymmetricAlgorithm.SM4, CryptoMode.ECB,
                Padding.NO_PADDING, null, 0);
        assertArrayEquals(expected, cipher);
        byte[] decrypted = SymmetricService.decrypt(cipher, key, SymmetricAlgorithm.SM4, CryptoMode.ECB,
                Padding.NO_PADDING, null, 0);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    void allAlgorithmsRoundtrip() throws CryptoException {
        byte[] data = "待加密的中英文混合文本 Hello 2026".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Object[][] cases = {
                {SymmetricAlgorithm.AES, 32},
                {SymmetricAlgorithm.AES, 16},
                {SymmetricAlgorithm.SM4, 16},
                {SymmetricAlgorithm.DES, 8},
                {SymmetricAlgorithm.DESEDE, 24},
        };
        for (Object[] c : cases) {
            SymmetricAlgorithm algo = (SymmetricAlgorithm) c[0];
            int keyLen = (Integer) c[1];
            byte[] key = SymmetricService.randomKey(algo, keyLen);
            CryptoMode[] modes = algo.supportGcm()
                    ? new CryptoMode[]{CryptoMode.ECB, CryptoMode.CBC, CryptoMode.CTR, CryptoMode.GCM}
                    : new CryptoMode[]{CryptoMode.ECB, CryptoMode.CBC, CryptoMode.CTR};
            for (CryptoMode mode : modes) {
                Padding padding = mode == CryptoMode.CTR || mode == CryptoMode.GCM
                        ? Padding.NO_PADDING : Padding.PKCS5;
                byte[] iv = mode.needIv() ? SymmetricService.randomIv(algo, mode) : null;
                byte[] cipher = SymmetricService.encrypt(data, key, algo, mode, padding, iv, 128);
                byte[] plain = SymmetricService.decrypt(cipher, key, algo, mode, padding, iv, 128);
                assertArrayEquals(data, plain, algo + "/" + mode + " 往返失败");
            }
        }
    }

    @Test
    void wrongKeyFails() throws CryptoException {
        byte[] data = "secret data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] key = SymmetricService.randomKey(SymmetricAlgorithm.AES, 16);
        byte[] cipher = SymmetricService.encrypt(data, key, SymmetricAlgorithm.AES, CryptoMode.GCM,
                Padding.NO_PADDING, SymmetricService.randomIv(SymmetricAlgorithm.AES, CryptoMode.GCM), 128);
        byte[] wrong = SymmetricService.randomKey(SymmetricAlgorithm.AES, 16);
        assertThrows(CryptoException.class, () -> SymmetricService.decrypt(cipher, wrong,
                SymmetricAlgorithm.AES, CryptoMode.GCM, Padding.NO_PADDING,
                SymmetricService.randomIv(SymmetricAlgorithm.AES, CryptoMode.GCM), 128));
    }

    @Test
    void invalidKeyLengthThrows() {
        byte[] key = new byte[10];
        assertThrows(CryptoException.class, () -> SymmetricService.encrypt(new byte[16], key,
                SymmetricAlgorithm.AES, CryptoMode.ECB, Padding.PKCS5, null, 0));
    }

    @Test
    void base64HexOutputInterop() throws CryptoException {
        byte[] data = "interoperability".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        byte[] cipher = SymmetricService.encrypt(data, key, SymmetricAlgorithm.AES, CryptoMode.CBC,
                Padding.PKCS5, iv, 0);
        String b64 = DataEncoding.BASE64.encode(cipher);
        assertArrayEquals(DataEncoding.BASE64.decode(b64), cipher);
        String hex = DataEncoding.HEX.encode(cipher);
        assertArrayEquals(DataEncoding.HEX.decode(hex), cipher);
    }
}
