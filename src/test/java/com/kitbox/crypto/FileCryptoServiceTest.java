package com.kitbox.crypto;

import com.kitbox.crypto.model.SymmetricAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCryptoServiceTest {

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void roundtripAesAndSm4() throws Exception {
        StringBuilder sb = new StringBuilder("文件内容 file content ");
        for (int i = 0; i < 2048; i++) {
            sb.append('x');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        for (SymmetricAlgorithm algo : new SymmetricAlgorithm[]{SymmetricAlgorithm.AES, SymmetricAlgorithm.SM4}) {
            byte[] key = new byte[algo.isValidKeyLength(32) ? 32 : 16];
            new SecureRandom().nextBytes(key);
            byte[] file = FileCryptoService.encryptFile(data, key, algo);
            assertTrue(file.length > data.length);
            assertArrayEquals(data, FileCryptoService.decryptFile(file, key), algo + " 文件往返失败");

            // 换一把密钥应解密失败（GCM 校验不过）
            byte[] wrong = new byte[key.length];
            new SecureRandom().nextBytes(wrong);
            assertThrows(CryptoException.class, () -> FileCryptoService.decryptFile(file, wrong));
        }
    }

    @Test
    void invalidMagicFails() {
        assertThrows(CryptoException.class, () -> FileCryptoService.decryptFile(
                "not a crypto box file".getBytes(StandardCharsets.UTF_8), new byte[16]));
    }
}
