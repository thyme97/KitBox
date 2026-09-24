package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileSuggestAndNormalizeTest {

    @Test
    void suggestFileNameFromDataUri() {
        assertEquals("image.png", FileBase64Service.suggestFileName(
                "data:image/png;base64,iVBORw0KGgo="));
        assertEquals("image.jpg", FileBase64Service.suggestFileName(
                "data:image/jpeg;base64,/9j/4AAQ"));
        assertEquals("image.svg", FileBase64Service.suggestFileName(
                "data:image/svg+xml;base64,PHN2Zz4="));
        assertEquals("document.pdf", FileBase64Service.suggestFileName(
                "data:application/pdf;base64,JVBERi0="));
        assertEquals("download.bin", FileBase64Service.suggestFileName(
                "SGVsbG8="));
        assertEquals("download.bin", FileBase64Service.suggestFileName(null));
        assertEquals("download.bin", FileBase64Service.suggestFileName("  "));
    }

    @Test
    void normalizeFilePathStripsFileScheme() {
        assertEquals("E:/x/a.png", com.kitbox.ui.SwingUtils.normalizeFilePath("file:///E:/x/a.png"));
        assertEquals("E:/x/a b.png", com.kitbox.ui.SwingUtils.normalizeFilePath("file:///E:/x/a%20b.png"));
        assertEquals("C:\\x\\a.png", com.kitbox.ui.SwingUtils.normalizeFilePath("\"C:\\x\\a.png\""));
        assertEquals("D:/a.png", com.kitbox.ui.SwingUtils.normalizeFilePath("D:/a.png"));
        // file:/E:/... 单斜杠形式走 URI→Paths 解析（Windows 下得到反斜杠路径）
        assertEquals("E:\\x\\a.png", com.kitbox.ui.SwingUtils.normalizeFilePath("file:/E:/x/a.png"));
    }
}
