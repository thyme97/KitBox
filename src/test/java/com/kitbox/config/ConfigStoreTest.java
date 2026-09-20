package com.kitbox.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void saveLoadRoundtrip() throws Exception {
        ConfigStore store = new ConfigStore(tempDir.resolve("config.json"));
        AppConfig config = store.load();
        // 默认值
        assertEquals("light", config.getTheme());
        assertTrue(config.getTemplates().isEmpty());

        config.setTheme("dark");
        config.setFontSize(15);
        config.setAutoCopyResult(true);
        config.getTemplates().add(new MessageTemplate("银行模板", "DATA|", "|END", "BASE64"));
        config.getTemplates().add(new MessageTemplate("hex模板", "<C>", "</C>", "HEX"));
        store.save(config);

        AppConfig loaded = new ConfigStore(tempDir.resolve("config.json")).load();
        assertEquals("dark", loaded.getTheme());
        assertEquals(15, loaded.getFontSize());
        assertTrue(loaded.isAutoCopyResult());
        assertEquals(2, loaded.getTemplates().size());
        assertEquals("DATA|", loaded.getTemplates().get(0).getPrefix());
        assertEquals("HEX", loaded.getTemplates().get(1).getContentEncoding());
    }

    @Test
    void corruptedFileFallsBackToDefaults() throws Exception {
        Path file = tempDir.resolve("broken.json");
        java.nio.file.Files.write(file, "{ not valid json !!".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AppConfig config = new ConfigStore(file).load();
        assertEquals("light", config.getTheme());
    }
}
