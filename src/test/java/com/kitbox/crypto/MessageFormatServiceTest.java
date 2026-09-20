package com.kitbox.crypto;

import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormatServiceTest {

    private static final byte[] KEY = new byte[16];
    private static final byte[] IV = new byte[16];

    @BeforeAll
    static void setUp() {
        Security.addProvider(new BouncyCastleProvider());
        new SecureRandom().nextBytes(KEY);
        new SecureRandom().nextBytes(IV);
    }

    private MessageFormatService.TextCipher cipher() {
        return new MessageFormatService.TextCipher() {
            @Override
            public byte[] encrypt(byte[] plain) throws CryptoException {
                return SymmetricService.encrypt(plain, KEY, SymmetricAlgorithm.SM4, CryptoMode.CBC,
                        Padding.PKCS5, IV, 0);
            }

            @Override
            public byte[] decrypt(byte[] cipherBytes) throws CryptoException {
                return SymmetricService.decrypt(cipherBytes, KEY, SymmetricAlgorithm.SM4, CryptoMode.CBC,
                        Padding.PKCS5, IV, 0);
            }
        };
    }

    @Test
    void wrapAndUnwrap() throws Exception {
        String plain = "报文内容 message body 2026";
        String message = MessageFormatService.encrypt(plain, "DATA|", "|END",
                DataEncoding.BASE64, cipher());
        assertTrue(message.startsWith("DATA|") && message.endsWith("|END"));

        String decrypted = MessageFormatService.decrypt(message, "DATA|", "|END",
                DataEncoding.BASE64, cipher());
        assertEquals(plain, decrypted);
    }

    @Test
    void emptyPrefixSuffix() throws Exception {
        String plain = "plain text";
        String message = MessageFormatService.encrypt(plain, "", "", DataEncoding.HEX, cipher());
        String decrypted = MessageFormatService.decrypt(message, "", "", DataEncoding.HEX, cipher());
        assertEquals(plain, decrypted);
    }

    @Test
    void prefixNotFoundFails() throws Exception {
        String message = MessageFormatService.encrypt("x", "P:", ":S", DataEncoding.BASE64, cipher());
        assertThrows(CryptoException.class, () -> MessageFormatService.decrypt(message, "Q:", ":S",
                DataEncoding.BASE64, cipher()));
    }

    @Test
    void utf8Consistency() throws Exception {
        String plain = new String("中文内容".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        String message = MessageFormatService.encrypt(plain, "", "", DataEncoding.BASE64, cipher());
        assertEquals(plain, MessageFormatService.decrypt(message, "", "", DataEncoding.BASE64, cipher()));
    }
}
