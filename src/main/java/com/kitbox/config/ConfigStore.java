package com.kitbox.config;

import com.kitbox.util.Gsons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置文件读写：~/.kitbox/config.json（UTF-8 JSON）。
 */
public final class ConfigStore {

    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    /** 默认配置目录：~/.kitbox */
    public static Path defaultDir() {
        return Paths.get(System.getProperty("user.home"), ".kitbox");
    }

    public static Path defaultFile() {
        return defaultDir().resolve("config.json");
    }

    public AppConfig load() {
        try {
            if (Files.isRegularFile(file)) {
                String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                AppConfig cfg = Gsons.gson().fromJson(json, AppConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            }
        } catch (Exception ignored) {
            // 配置损坏时回退默认值
        }
        return new AppConfig();
    }

    public void save(AppConfig config) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, Gsons.gson().toJson(config).getBytes(StandardCharsets.UTF_8));
    }
}
