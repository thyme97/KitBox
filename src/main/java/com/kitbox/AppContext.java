package com.kitbox;

import com.kitbox.config.AppConfig;
import com.kitbox.config.ConfigStore;
import com.kitbox.keystore.KeyStoreManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Security;

/**
 * 应用上下文：全局持有配置与密钥库。
 * 数据目录默认 ~/.kitbox，可通过 ~/.kitbox/data-dir.txt 指针文件重定向。
 */
public final class AppContext {

    /** 指针文件名：内容为数据目录的绝对路径（位于默认目录 ~/.kitbox 下，固定不可移动） */
    public static final String POINTER_FILE = "data-dir.txt";

    public static Path dataDir;
    public static AppConfig config;
    public static ConfigStore configStore;
    public static KeyStoreManager keyStore;

    private AppContext() {
    }

    public static void init() {
        Security.addProvider(new BouncyCastleProvider());
        dataDir = resolveDataDir();
        configStore = new ConfigStore(dataDir.resolve("config.json"));
        config = configStore.load();
        keyStore = new KeyStoreManager(dataDir.resolve("keystore.dat"));
    }

    /** 默认数据目录 ~/.kitbox */
    public static Path defaultDir() {
        return Paths.get(System.getProperty("user.home"), ".kitbox");
    }

    public static Path pointerFile() {
        return defaultDir().resolve(POINTER_FILE);
    }

    /** 解析当前数据目录：优先 ~/.kitbox/data-dir.txt 指针，否则 ~/.kitbox。 */
    private static Path resolveDataDir() {
        Path def = defaultDir();
        try {
            Path pointer = def.resolve(POINTER_FILE);
            if (Files.isRegularFile(pointer)) {
                Path pointed = readPointer(pointer);
                if (pointed != null) {
                    return pointed;
                }
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static Path readPointer(Path pointer) {
        try {
            String p = new String(Files.readAllBytes(pointer), StandardCharsets.UTF_8).trim();
            if (!p.isEmpty()) {
                Path dir = Paths.get(p);
                if (Files.isDirectory(dir)) {
                    return dir;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 迁移数据目录：把 config.json 与 keystore.dat 复制到新目录，并写入指针文件。
     * 立即复制完成，重启应用后从新目录读取。
     *
     * @return 实际复制过的文件数
     */
    public static int changeDataDir(Path newDir) throws IOException {
        if (newDir == null || !newDir.isAbsolute()) {
            throw new IOException("请选择一个绝对路径的目录");
        }
        Files.createDirectories(newDir);
        int copied = 0;
        String[] names = {"config.json", "keystore.dat"};
        for (String name : names) {
            Path source = dataDir.resolve(name);
            if (Files.isRegularFile(source)) {
                Files.copy(source, newDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
        }
        Files.write(pointerFile(), newDir.toString().getBytes(StandardCharsets.UTF_8));
        return copied;
    }

    /** 持久化配置（失败静默，避免打断操作）。 */
    public static void saveConfig() {
        try {
            configStore.save(config);
        } catch (Exception ignored) {
        }
    }
}
