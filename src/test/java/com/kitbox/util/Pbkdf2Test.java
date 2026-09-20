package com.kitbox.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Pbkdf2Test {

    private byte[] salt(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void rfcVectors() {
        // 公开标准测试向量（password/salt，HMAC-SHA256）
        assertArrayEquals(HexUtils.decode("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b"),
                Pbkdf2.derive("password".toCharArray(), salt("salt"), 1, 256));
        assertArrayEquals(HexUtils.decode("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43"),
                Pbkdf2.derive("password".toCharArray(), salt("salt"), 2, 256));
        assertArrayEquals(HexUtils.decode("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a"),
                Pbkdf2.derive("password".toCharArray(), salt("salt"), 4096, 256));
    }

    @Test
    void deterministicAndMultiBlock() {
        byte[] a = Pbkdf2.derive("密码123".toCharArray(), salt("0123456789abcdef"), 1000, 512);
        byte[] b = Pbkdf2.derive("密码123".toCharArray(), salt("0123456789abcdef"), 1000, 512);
        assertArrayEquals(a, b);
        assertEquals(64, a.length);
    }

    @Test
    void invalidParams() {
        assertThrows(IllegalArgumentException.class,
                () -> Pbkdf2.derive("x".toCharArray(), salt("s"), 0, 256));
        assertThrows(IllegalArgumentException.class,
                () -> Pbkdf2.derive("x".toCharArray(), salt("s"), 1, 250));
    }
}
