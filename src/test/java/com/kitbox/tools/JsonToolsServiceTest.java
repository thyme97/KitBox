package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonToolsServiceTest {

    @Test
    void formatUsesIndentAndKeepsChinese() {
        String json = "{\"name\":\"工具箱\",\"nested\":{\"ok\":true}}";
        String formatted = JsonToolsService.format(json, 2);

        assertTrue(formatted.contains("\n  \"name\""));
        assertTrue(formatted.contains("工具箱"));
        assertFalse(formatted.contains("\\u"));
        assertTrue(formatted.contains("\"nested\": {"));
    }

    @Test
    void formatFourSpaces() {
        String formatted = JsonToolsService.format("{\"a\":{\"b\":1}}", 4);
        assertTrue(formatted.contains("    \"b\": 1"));
    }

    @Test
    void compressRemovesWhitespaceButKeepsStringContent() {
        String compressed = JsonToolsService.compress("{ \"a\" : \"hello world\" ,\n \"b\" : [1, 2] }");
        assertEquals("{\"a\":\"hello world\",\"b\":[1,2]}", compressed);
    }

    @Test
    void validateReturnsNullForValidAndMessageForBroken() {
        assertNull(JsonToolsService.validate("{\"a\":1}"));
        String error = JsonToolsService.validate("{\"a\":}");
        assertNotNull(error);
        assertFalse(error.isEmpty());

        String trailing = JsonToolsService.validate("{\"a\":1} {\"b\":2}");
        assertNotNull(trailing);
    }

    @Test
    void validateRejectsNonJsonText() {
        assertNotNull(JsonToolsService.validate("just a plain text"));
        assertNotNull(JsonToolsService.validate(""));
    }

    @Test
    void escapeUnescapeRoundTrip() {
        String raw = "引号\" 反斜杠\\ 换行\n制表\t";
        String escaped = JsonToolsService.escape(raw);
        assertTrue(escaped.startsWith("\""));
        assertEquals(raw, JsonToolsService.unescape(escaped));
    }

    @Test
    void unescapeBareTextDecodesSequences() {
        assertEquals("line1\nline2", JsonToolsService.unescape("line1\\nline2"));
        assertEquals("C:\\temp", JsonToolsService.unescape("\"C:\\\\temp\""));
    }

    @Test
    void numbersKeepLexicalForm() {
        String formatted = JsonToolsService.format("{\"price\":1.50,\"n\":-3}", 2);
        assertTrue(formatted.contains("1.50"));
        assertTrue(formatted.contains("-3"));
    }

    @Test
    void compressRejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> JsonToolsService.compress("{a:1}"));
    }
}
