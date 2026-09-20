package com.kitbox.keystore;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 密钥库管理器。
 * <p>
 * 文件格式（v2）：MAGIC(4B) "CBKS" | 版本(1B)=2 | 模式(1B) | 载荷
 * <ul>
 *   <li>模式 0x01 = 密码保护：盐(16B) | IV(12B) | AES-256-GCM 密文；
 *       主密码经 PBKDF2-HMAC-SHA256（120000 次迭代）派生密钥</li>
 *   <li>模式 0x00 = 无密码：JSON 明文（UTF-8）</li>
 * </ul>
 * 兼容 v1 旧格式（MAGIC | 版本=1 | 盐 | IV | 密文，固定密码保护）：解锁成功后保存时自动升级为 v2。
 */
public class KeyStoreManager {

    private static final byte[] MAGIC = {'C', 'B', 'K', 'S'};
    private static final byte VERSION = 2;
    private static final byte MODE_PLAIN = 0x00;
    private static final byte MODE_PASSWORD = 0x01;
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final byte LEGACY_VERSION = 1;

    private final Path file;
    private KeyStoreData data;
    private byte[] derivedKey;
    private byte[] salt;
    private byte mode = MODE_PASSWORD;

    public KeyStoreManager(Path file) {
        this.file = file;
    }

    // ---------------- 状态 ----------------

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    public boolean isUnlocked() {
        return data != null;
    }

    /** 当前是否为密码保护模式（需已解锁）。 */
    public boolean isProtected() {
        return data != null && mode == MODE_PASSWORD;
    }

    /** 文件是否为无密码（明文）模式；文件不存在或 v1 旧格式返回 false。 */
    public boolean isPlainFile() {
        if (!exists()) {
            return false;
        }
        try {
            byte[] head = new byte[6];
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file.toFile(), "r")) {
                if (raf.length() < 6) {
                    return false;
                }
                raf.readFully(head);
            }
            return head[4] == VERSION && head[5] == MODE_PLAIN;
        } catch (IOException e) {
            return false;
        }
    }

    public Path getFile() {
        return file;
    }

    // ---------------- 生命周期 ----------------

    /** 首次使用：创建新密钥库。密码为空表示不设置密码（明文保存）。 */
    public void initialize(char[] masterPassword) throws KeyStoreException {
        if (exists()) {
            throw new KeyStoreException("密钥库已存在，请直接解锁");
        }
        data = new KeyStoreData();
        for (String s : new String[]{"开发", "测试", "生产"}) {
            data.getScenarios().add(s);
        }
        if (masterPassword == null || masterPassword.length == 0) {
            mode = MODE_PLAIN;
            salt = null;
            derivedKey = null;
            writePlainFile();
        } else {
            if (masterPassword.length < 6) {
                throw new KeyStoreException("主密码至少 6 位，或全部留空表示不设置密码");
            }
            mode = MODE_PASSWORD;
            saveWithPassword(masterPassword);
        }
    }

    public void unlock(char[] masterPassword) throws KeyStoreException {
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new KeyStoreException("读取密钥库失败：" + e.getMessage(), e);
        }
        try {
            if (fileBytes.length < 6 || fileBytes[0] != MAGIC[0] || fileBytes[1] != MAGIC[1]
                    || fileBytes[2] != MAGIC[2] || fileBytes[3] != MAGIC[3]) {
                throw new KeyStoreException("文件格式不正确：不是本工具的密钥库文件");
            }
            byte ver = fileBytes[4];
            if (ver == LEGACY_VERSION) {
                unlockLegacyV1(fileBytes, masterPassword);
                // 解锁成功后立即升级为 v2 格式
                save();
            } else if (ver == VERSION) {
                unlockV2(fileBytes, masterPassword);
            } else {
                throw new KeyStoreException("不支持的密钥库文件版本：" + ver);
            }
        } catch (KeyStoreException e) {
            resetMemory();
            throw e;
        } catch (Exception e) {
            resetMemory();
            throw new KeyStoreException("解锁失败：" + e.getMessage(), e);
        }
    }

    private void unlockLegacyV1(byte[] fileBytes, char[] masterPassword) throws KeyStoreException {
        int offset = MAGIC.length + 1;
        byte[] saltBytes = Arrays.copyOfRange(fileBytes, offset, offset + SALT_LEN);
        offset += SALT_LEN;
        byte[] iv = Arrays.copyOfRange(fileBytes, offset, offset + IV_LEN);
        offset += IV_LEN;
        byte[] cipherBytes = Arrays.copyOfRange(fileBytes, offset, fileBytes.length);
        if (masterPassword == null || masterPassword.length == 0) {
            throw new KeyStoreException("该密钥库设置了密码，请输入主密码");
        }
        byte[] key = Pbkdf2Derive.derive(masterPassword, saltBytes);
        byte[] plain = gcm(Cipher.DECRYPT_MODE, key, iv, cipherBytes);
        data = com.kitbox.util.Gsons.gson()
                .fromJson(new String(plain, StandardCharsets.UTF_8), KeyStoreData.class);
        this.mode = MODE_PASSWORD;
        this.salt = saltBytes;
        this.derivedKey = key;
    }

    private void unlockV2(byte[] fileBytes, char[] masterPassword) throws KeyStoreException {
        byte modeByte = fileBytes[5];
        if (modeByte == MODE_PLAIN) {
            data = com.kitbox.util.Gsons.gson()
                    .fromJson(new String(fileBytes, 6, fileBytes.length - 6, StandardCharsets.UTF_8),
                            KeyStoreData.class);
            this.mode = MODE_PLAIN;
            this.salt = null;
            this.derivedKey = null;
            return;
        }
        if (modeByte == MODE_PASSWORD) {
            if (masterPassword == null || masterPassword.length == 0) {
                throw new KeyStoreException("该密钥库设置了密码，请输入主密码");
            }
            int offset = 6;
            byte[] saltBytes = Arrays.copyOfRange(fileBytes, offset, offset + SALT_LEN);
            offset += SALT_LEN;
            byte[] iv = Arrays.copyOfRange(fileBytes, offset, offset + IV_LEN);
            offset += IV_LEN;
            byte[] cipherBytes = Arrays.copyOfRange(fileBytes, offset, fileBytes.length);
            byte[] key = Pbkdf2Derive.derive(masterPassword, saltBytes);
            byte[] plain = gcm(Cipher.DECRYPT_MODE, key, iv, cipherBytes);
            data = com.kitbox.util.Gsons.gson()
                    .fromJson(new String(plain, StandardCharsets.UTF_8), KeyStoreData.class);
            this.mode = MODE_PASSWORD;
            this.salt = saltBytes;
            this.derivedKey = key;
            return;
        }
        throw new KeyStoreException("不支持的密钥库模式：" + modeByte);
    }

    public void lock() {
        resetMemory();
    }

    private void resetMemory() {
        data = null;
        derivedKey = null;
        salt = null;
    }

    // ---------------- 修改密码 ----------------

    /**
     * 修改主密码。
     * <ul>
     *   <li>当前为密码保护模式时须校验 oldPwd；</li>
     *   <li>newPwd 为空 → 切换为无密码（明文保存）；</li>
     *   <li>当前为无密码模式时可直接设置新密码（oldPwd 忽略）。</li>
     * </ul>
     */
    public void changePassword(char[] oldPwd, char[] newPwd) throws KeyStoreException {
        if (data == null) {
            throw new KeyStoreException("密钥库未解锁");
        }
        if (mode == MODE_PASSWORD) {
            if (oldPwd == null || oldPwd.length == 0) {
                throw new KeyStoreException("请输入原密码");
            }
            byte[] candidate = Pbkdf2Derive.derive(oldPwd, salt);
            if (!MessageDigest.isEqual(candidate, derivedKey)) {
                throw new KeyStoreException("原密码错误");
            }
        }
        if (newPwd == null || newPwd.length == 0) {
            mode = MODE_PLAIN;
            salt = null;
            derivedKey = null;
            writePlainFile();
            return;
        }
        if (newPwd.length < 6) {
            throw new KeyStoreException("新密码至少 6 位（留空则表示不设置密码）");
        }
        byte[] newSalt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(newSalt);
        this.salt = newSalt;
        this.derivedKey = Pbkdf2Derive.derive(newPwd, newSalt);
        this.mode = MODE_PASSWORD;
        writeEnvelope(derivedKey);
    }

    // ---------------- 场景 ----------------

    public List<String> getScenarios() {
        requireUnlockedRead();
        return new ArrayList<>(data.getScenarios());
    }

    public void addScenario(String name) throws KeyStoreException {
        requireUnlocked();
        String n = name.trim();
        if (n.isEmpty()) {
            throw new KeyStoreException("场景名不能为空");
        }
        if (data.getScenarios().contains(n)) {
            throw new KeyStoreException("场景已存在：" + n);
        }
        data.getScenarios().add(n);
        save();
    }

    public void renameScenario(String oldName, String newName) throws KeyStoreException {
        requireUnlocked();
        String n = newName.trim();
        if (n.isEmpty()) {
            throw new KeyStoreException("场景名不能为空");
        }
        int idx = data.getScenarios().indexOf(oldName);
        if (idx < 0) {
            throw new KeyStoreException("场景不存在：" + oldName);
        }
        if (data.getScenarios().contains(n) && !n.equals(oldName)) {
            throw new KeyStoreException("场景已存在：" + n);
        }
        data.getScenarios().set(idx, n);
        for (KeyEntry e : data.getEntries()) {
            if (oldName.equals(e.getScenario())) {
                e.setScenario(n);
            }
        }
        save();
    }

    public void deleteScenario(String name) throws KeyStoreException {
        requireUnlocked();
        long count = data.getEntries().stream().filter(e -> name.equals(e.getScenario())).count();
        if (count > 0) {
            throw new KeyStoreException("场景下还有 " + count + " 个密钥，请先删除或移动");
        }
        data.getScenarios().remove(name);
        save();
    }

    // ---------------- 密钥条目 ----------------

    public List<KeyEntry> getEntries() {
        requireUnlockedRead();
        return new ArrayList<>(data.getEntries());
    }

    public List<KeyEntry> getEntries(String scenario) {
        requireUnlockedRead();
        List<KeyEntry> result = new ArrayList<>();
        for (KeyEntry e : data.getEntries()) {
            if (scenario == null || scenario.equals(e.getScenario())) {
                result.add(e);
            }
        }
        return result;
    }

    public KeyEntry addEntry(String scenario, String name, KeyEntryType type, String value, String remark)
            throws KeyStoreException {
        requireUnlocked();
        validate(scenario, name, null);
        if (value == null || value.trim().isEmpty()) {
            throw new KeyStoreException("密钥值不能为空");
        }
        KeyEntry entry = new KeyEntry(
                java.util.UUID.randomUUID().toString(),
                scenario.trim(), name.trim(), type, value,
                remark == null ? "" : remark.trim(),
                java.time.LocalDateTime.now().toString());
        data.getEntries().add(entry);
        save();
        return entry;
    }

    public void updateEntry(KeyEntry entry) throws KeyStoreException {
        requireUnlocked();
        validate(entry.getScenario(), entry.getName(), entry.getId());
        boolean found = false;
        for (int i = 0; i < data.getEntries().size(); i++) {
            if (data.getEntries().get(i).getId().equals(entry.getId())) {
                data.getEntries().set(i, entry);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new KeyStoreException("密钥条目不存在");
        }
        save();
    }

    public void deleteEntry(String id) throws KeyStoreException {
        requireUnlocked();
        boolean removed = data.getEntries().removeIf(e -> e.getId().equals(id));
        if (!removed) {
            throw new KeyStoreException("密钥条目不存在");
        }
        save();
    }

    // ---------------- 备份 ----------------

    /** 导出加密备份（使用独立的备份密码，与主密码无关）。 */
    public void exportBackup(Path target, char[] backupPassword) throws KeyStoreException {
        requireUnlocked();
        byte[] backupSalt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(backupSalt);
        new SecureRandom().nextBytes(iv);
        byte[] key = Pbkdf2Derive.derive(backupPassword, backupSalt);
        String json = com.kitbox.util.Gsons.gson().toJson(data);
        byte[] cipher = gcm(Cipher.ENCRYPT_MODE, key, iv, json.getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(MAGIC.length + 1 + SALT_LEN + IV_LEN + cipher.length);
            out.write(MAGIC, 0, MAGIC.length);
            out.write(LEGACY_VERSION);
            out.write(backupSalt, 0, SALT_LEN);
            out.write(iv, 0, IV_LEN);
            out.write(cipher, 0, cipher.length);
            Files.write(target, out.toByteArray());
        } catch (IOException e) {
            throw new KeyStoreException("导出备份失败：" + e.getMessage(), e);
        }
    }

    /** 导入备份并合并（按 场景+名称 去重，已存在的跳过）。返回新增条数。 */
    public int importBackup(Path source, char[] backupPassword) throws KeyStoreException {
        requireUnlocked();
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(source);
        } catch (IOException e) {
            throw new KeyStoreException("读取备份失败：" + e.getMessage(), e);
        }
        KeyStoreData backup;
        try {
            int offset = MAGIC.length + 1;
            byte[] backupSalt = Arrays.copyOfRange(fileBytes, offset, offset + SALT_LEN);
            offset += SALT_LEN;
            byte[] iv = Arrays.copyOfRange(fileBytes, offset, offset + IV_LEN);
            offset += IV_LEN;
            byte[] cipherBytes = Arrays.copyOfRange(fileBytes, offset, fileBytes.length);
            byte[] key = Pbkdf2Derive.derive(backupPassword, backupSalt);
            byte[] plain = gcm(Cipher.DECRYPT_MODE, key, iv, cipherBytes);
            backup = com.kitbox.util.Gsons.gson()
                    .fromJson(new String(plain, StandardCharsets.UTF_8), KeyStoreData.class);
        } catch (KeyStoreException e) {
            throw new KeyStoreException("备份密码错误或备份文件损坏", e);
        } catch (Exception e) {
            throw new KeyStoreException("导入备份失败：" + e.getMessage(), e);
        }
        if (backup == null) {
            throw new KeyStoreException("备份内容为空");
        }
        int added = 0;
        for (String s : backup.getScenarios()) {
            if (!data.getScenarios().contains(s)) {
                data.getScenarios().add(s);
            }
        }
        for (KeyEntry e : backup.getEntries()) {
            boolean exists = data.getEntries().stream()
                    .anyMatch(x -> x.getScenario() != null && x.getScenario().equals(e.getScenario())
                            && x.getName() != null && x.getName().equals(e.getName()));
            if (!exists) {
                e.setId(java.util.UUID.randomUUID().toString());
                data.getEntries().add(e);
                added++;
            }
        }
        save();
        return added;
    }

    // ---------------- 持久化 ----------------

    /** 使用当前状态重新落盘（无密码模式写明文，密码模式重新加密）。 */
    public void save() throws KeyStoreException {
        if (data == null) {
            throw new KeyStoreException("密钥库未解锁");
        }
        if (mode == MODE_PLAIN) {
            writePlainFile();
            return;
        }
        if (derivedKey == null) {
            throw new KeyStoreException("密钥库未解锁");
        }
        writeEnvelope(derivedKey);
    }

    private void saveWithPassword(char[] masterPassword) throws KeyStoreException {
        byte[] newSalt = new byte[SALT_LEN];
        new SecureRandom().nextBytes(newSalt);
        this.salt = newSalt;
        this.derivedKey = Pbkdf2Derive.derive(masterPassword, newSalt);
        writeEnvelope(derivedKey);
    }

    private void writePlainFile() throws KeyStoreException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            String json = com.kitbox.util.Gsons.gson().toJson(data);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream(6 + jsonBytes.length);
            out.write(MAGIC, 0, MAGIC.length);
            out.write(VERSION);
            out.write(MODE_PLAIN);
            out.write(jsonBytes, 0, jsonBytes.length);
            Files.write(file, out.toByteArray());
        } catch (IOException e) {
            throw new KeyStoreException("保存密钥库失败：" + e.getMessage(), e);
        }
    }

    private void writeEnvelope(byte[] key) throws KeyStoreException {
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);
        String json = com.kitbox.util.Gsons.gson().toJson(data);
        byte[] cipher = gcm(Cipher.ENCRYPT_MODE, key, iv, json.getBytes(StandardCharsets.UTF_8));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream(MAGIC.length + 2 + SALT_LEN + IV_LEN + cipher.length);
            out.write(MAGIC, 0, MAGIC.length);
            out.write(VERSION);
            out.write(MODE_PASSWORD);
            out.write(salt, 0, SALT_LEN);
            out.write(iv, 0, IV_LEN);
            out.write(cipher, 0, cipher.length);
            Files.write(file, out.toByteArray());
        } catch (IOException e) {
            throw new KeyStoreException("保存密钥库失败：" + e.getMessage(), e);
        }
    }

    private byte[] gcm(int opMode, byte[] key, byte[] iv, byte[] input) throws KeyStoreException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(opMode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(input);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new KeyStoreException("校验失败（密码错误或数据损坏）", e);
        } catch (Exception e) {
            throw new KeyStoreException("AES-GCM 操作失败：" + e.getMessage(), e);
        }
    }

    private void validate(String scenario, String name, String exceptId) throws KeyStoreException {
        if (scenario == null || scenario.trim().isEmpty()) {
            throw new KeyStoreException("请选择场景");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new KeyStoreException("密钥名称不能为空");
        }
        for (KeyEntry e : data.getEntries()) {
            // 空安全：字段缺失的历史脏数据不参与判重
            if (e.getScenario() == null || e.getName() == null || e.getId() == null) {
                continue;
            }
            if (e.getScenario().equals(scenario.trim()) && e.getName().equals(name.trim())
                    && !e.getId().equals(exceptId)) {
                throw new KeyStoreException("该场景下已存在同名密钥：" + name.trim());
            }
        }
    }

    private void requireUnlocked() throws KeyStoreException {
        if (data == null) {
            throw new KeyStoreException("密钥库未解锁");
        }
    }

    /** 读取路径使用非受检异常，调用方通常已确保解锁。 */
    private void requireUnlockedRead() {
        if (data == null) {
            throw new IllegalStateException("密钥库未解锁");
        }
    }

    /** PBKDF2 参数集中管理。 */
    private static final class Pbkdf2Derive {
        static byte[] derive(char[] password, byte[] salt) {
            return com.kitbox.util.Pbkdf2.derive(password, salt, PBKDF2_ITERATIONS, 256);
        }
    }
}
