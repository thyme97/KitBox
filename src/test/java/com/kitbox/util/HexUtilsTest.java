package com.kitbox.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HexUtilsTest {

    @Test
    void encodeDecodeRoundtrip() {
        byte[] data = "你好，KitBox 123!".getBytes(StandardCharsets.UTF_8);
        String hex = HexUtils.encode(data);
        assertArrayEquals(data, HexUtils.decode(hex));
    }

    @Test
    void decodeAcceptsUppercaseAndSpaces() {
        byte[] expected = {0x12, 0x3A, (byte) 0xFF};
        assertArrayEquals(expected, HexUtils.decode("123aff"));
        assertArrayEquals(expected, HexUtils.decode("12 3A FF"));
    }

    @Test
    void decodeRejectsOddLengthAndIllegalChar() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("123"));
        assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("zz"));
    }

    @Test
    void emptyInput() {
        assertEquals("", HexUtils.encode(new byte[0]));
        assertEquals(0, HexUtils.decode("").length);
    }
}
