package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileBase64ServiceTest {

    @Test
    void encodeDecodeRoundTrip() throws Exception {
        Path file = Files.createTempFile("kitbox-b64", ".bin");
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        Files.write(file, data);
        String base64 = FileBase64Service.encode(file, false);
        assertEquals(0, base64.length() % 4);
        assertArrayEquals(data, FileBase64Service.decode(base64));
        Files.deleteIfExists(file);
    }

    @Test
    void dataUriPrefixAndStrip() throws Exception {
        Path png = Files.createTempFile("kitbox-pic", ".png");
        byte[] data = "PNGDATA".getBytes(StandardCharsets.UTF_8);
        Files.write(png, data);
        String uri = FileBase64Service.encode(png, true);
        assertTrue(uri.startsWith("data:image/png;base64,"));
        assertArrayEquals(data, FileBase64Service.decode(uri));
        Files.deleteIfExists(png);
    }

    @Test
    void decodeToleratesWhitespaceAndUnknownMime() {
        byte[] data = "hello world 你好".getBytes(StandardCharsets.UTF_8);
        String base64 = java.util.Base64.getEncoder().encodeToString(data);
        // 手动断行
        String wrapped = base64.substring(0, 8) + "\r\n  " + base64.substring(8);
        assertArrayEquals(data, FileBase64Service.decode(wrapped));
        // 未知扩展名走 application/octet-stream
        assertEquals("application/octet-stream", FileBase64Service.mimeOf("data.unknown"));
        assertEquals("image/jpeg", FileBase64Service.mimeOf("photo.JPG"));
        assertEquals("application/pdf", FileBase64Service.mimeOf("contract.pdf"));
    }

    @Test
    void decodeRejectsEmptyAndGarbage() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileBase64Service.decode("   "));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileBase64Service.decode("!!不是base64!!"));
    }
}
