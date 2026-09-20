package com.kitbox.crypto;

import com.kitbox.crypto.model.DataEncoding;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingServiceTest {

    private byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void base64Standard() {
        assertEquals("5L2g5aW9", EncodingService.base64Encode(utf8("你好"), false, false));
        assertArrayEquals(utf8("你好"), EncodingService.base64Decode("5L2g5aW9", false));
    }

    @Test
    void base64UrlSafe() {
        byte[] data = {(byte) 0xFB, (byte) 0xFF, 0x7E};
        String std = EncodingService.base64Encode(data, false, false);
        String url = EncodingService.base64Encode(data, true, false);
        assertEquals(std.replace('+', '-').replace('/', '_').replace("=", ""), url);
        assertArrayEquals(data, EncodingService.base64Decode(url, true));
    }

    @Test
    void base64WithLineBreaks() {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        String wrapped = EncodingService.base64Encode(data, false, true);
        assertTrue(wrapped.contains("\r\n"));
        assertArrayEquals(data, EncodingService.base64Decode(wrapped, false));
    }

    @Test
    void urlEncodeDecode() throws CryptoException {
        assertEquals("%E4%BD%A0%E5%A5%BD+a%2Fb",
                EncodingService.urlEncode("你好 a/b", "UTF-8"));
        assertEquals("你好 a/b", EncodingService.urlDecode("%E4%BD%A0%E5%A5%BD+a%2Fb", "UTF-8"));
    }

    @Test
    void dataEncodingHelpers() {
        byte[] data = "混合内容 mixed 中文".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(data, DataEncoding.BASE64.decode(DataEncoding.BASE64.encode(data)));
        assertArrayEquals(data, DataEncoding.HEX.decode(DataEncoding.HEX.encode(data)));
    }
}
