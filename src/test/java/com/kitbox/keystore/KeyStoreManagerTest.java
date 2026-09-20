package com.kitbox.keystore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyStoreManagerTest {

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setUp() {
        java.security.Security.addProvider(new BouncyCastleProvider());
    }

    private KeyStoreManager newStore() {
        return new KeyStoreManager(tempDir.resolve("keystore.dat"));
    }

    @Test
    void initializeAndPersist() throws Exception {
        KeyStoreManager store = newStore();
        assertFalse(store.exists());
        store.initialize("主密码123".toCharArray());
        assertTrue(store.exists());
        assertTrue(store.isUnlocked());
        assertEquals(3, store.getScenarios().size());

        byte[] key = new byte[16];
        new java.security.SecureRandom().nextBytes(key);
        store.addEntry("开发", "my-aes", KeyEntryType.SYM_AES, Base64.getEncoder().encodeToString(key), "测试密钥");
        store.lock();
        assertFalse(store.isUnlocked());

        // 重新解锁后数据完整
        KeyStoreManager store2 = newStore();
        store2.unlock("主密码123".toCharArray());
        List<KeyEntry> entries = store2.getEntries("开发");
        assertEquals(1, entries.size());
        assertEquals("my-aes", entries.get(0).getName());
        assertEquals("测试密钥", entries.get(0).getRemark());
        assertArrayEquals(key, Base64.getDecoder().decode(entries.get(0).getValue()));
    }

    @Test
    void wrongPasswordFails() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("correct-password".toCharArray());
        store.lock();

        KeyStoreManager store2 = newStore();
        assertThrows(KeyStoreException.class, () -> store2.unlock("wrong-password".toCharArray()));
        assertFalse(store2.isUnlocked());
    }

    @Test
    void scenarioCrud() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("p@ssword".toCharArray());
        store.addScenario("对接方A");
        assertEquals(4, store.getScenarios().size());
        store.renameScenario("对接方A", "对接方B");
        assertTrue(store.getScenarios().contains("对接方B"));
        assertFalse(store.getScenarios().contains("对接方A"));

        store.addEntry("对接方B", "k1", KeyEntryType.HMAC, "aaaa", "");
        // 场景非空不可删
        assertThrows(KeyStoreException.class, () -> store.deleteScenario("对接方B"));
        store.deleteEntry(store.getEntries("对接方B").get(0).getId());
        store.deleteScenario("对接方B");
        assertFalse(store.getScenarios().contains("对接方B"));
    }

    @Test
    void duplicateNameRejected() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("p@ssword".toCharArray());
        store.addEntry("测试", "k", KeyEntryType.SYM_AES, "aaaa", "");
        assertThrows(KeyStoreException.class, () -> store.addEntry("测试", "k",
                KeyEntryType.SYM_AES, "bbbb", ""));
    }

    @Test
    void backupExportImport() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("master".toCharArray());
        store.addEntry("生产", "rsa", KeyEntryType.RSA_KEYPAIR, "{\"publicKey\":\"p\",\"privateKey\":\"q\"}", "");

        Path backupFile = tempDir.resolve("backup.dat");
        store.exportBackup(backupFile, "backup-pass".toCharArray());
        assertTrue(Files.size(backupFile) > 0);

        // 另一个密钥库导入备份并合并
        KeyStoreManager other = new KeyStoreManager(tempDir.resolve("other.dat"));
        other.initialize("other-master".toCharArray());
        int added = other.importBackup(backupFile, "backup-pass".toCharArray());
        assertEquals(1, added);
        assertEquals(1, other.getEntries("生产").size());

        // 密码错误
        assertThrows(KeyStoreException.class, () -> other.importBackup(backupFile,
                "bad-pass".toCharArray()));
    }

    /** 回归：解锁后（非初始化会话）的第一次写操作必须能正常落盘（历史缺陷：盐值未保留导致 NPE）。 */
    @Test
    void mutateAfterUnlock() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("pppppp".toCharArray());
        store.lock();

        KeyStoreManager store2 = newStore();
        store2.unlock("pppppp".toCharArray());
        store2.addScenario("对接方C");
        store2.addEntry("对接方C", "k", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "");

        store2.lock();
        KeyStoreManager store3 = newStore();
        store3.unlock("pppppp".toCharArray());
        assertTrue(store3.getScenarios().contains("对接方C"));
        assertEquals(1, store3.getEntries("对接方C").size());
    }

    /** 回归：字段缺失的脏条目不应导致判重/保存/遍历空指针。 */
    @Test
    void tolerateEntriesWithNullFields() throws Exception {
        // 直接构造一个含脏条目（全空字段）的密钥库文件
        KeyStoreData dirty = new KeyStoreData();
        dirty.getScenarios().addAll(java.util.Arrays.asList("开发", "测试", "生产"));
        dirty.getEntries().add(new KeyEntry("bad-id", null, null, null, null, null, null));
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        byte[] key = com.kitbox.util.Pbkdf2.derive("pppppp".toCharArray(), salt, 120000, 256);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(com.kitbox.util.Gsons.gson().toJson(dirty)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.io.ByteArrayOutputStream fileBytes = new java.io.ByteArrayOutputStream();
        fileBytes.write(new byte[]{'C','B','K','S'});
        fileBytes.write(1);
        fileBytes.write(salt, 0, 16);
        fileBytes.write(iv, 0, 12);
        fileBytes.write(ct, 0, ct.length);
        java.nio.file.Files.write(tempDir.resolve("keystore.dat"), fileBytes.toByteArray());

        KeyStoreManager store = newStore();
        store.unlock("pppppp".toCharArray());
        // 含脏条目时：判重、新增、遍历都不应抛 NPE
        store.addEntry("开发", "k", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "");
        store.addEntry("开发", "k2", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "");
        assertEquals(3, store.getEntries().size());
        assertEquals("k", store.getEntries("开发").get(0).getName());
    }

    @Test
    void updateAndDeleteEntry() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("pppppp".toCharArray());
        KeyEntry entry = store.addEntry("开发", "k", KeyEntryType.SYM_SM4, "AAAA", "");
        entry.setName("k2");
        entry.setValue("BBBB");
        store.updateEntry(entry);
        assertEquals("k2", store.getEntries("开发").get(0).getName());
        store.deleteEntry(entry.getId());
        assertEquals(0, store.getEntries("开发").size());
    }
    /** 回归：无密码模式（明文保存）——创建、落盘、免密加载。 */
    @Test
    void noPasswordMode() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize(new char[0]);
        assertTrue(store.isPlainFile());
        assertFalse(store.isProtected());
        store.addEntry("开发", "k", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "");
        store.lock();
        assertTrue(store.isPlainFile());

        KeyStoreManager store2 = newStore();
        store2.unlock(new char[0]); // 无密码库免密加载
        assertEquals(1, store2.getEntries("开发").size());
    }

    /** 回归：密码修改 + 密码与无密码双向切换。 */
    @Test
    void changePasswordFlow() throws Exception {
        KeyStoreManager store = newStore();
        store.initialize("aaaaaa".toCharArray());
        store.addEntry("开发", "k", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "");

        assertThrows(KeyStoreException.class, () -> store.changePassword(
                "wrong-pwd".toCharArray(), "bbbbbb".toCharArray()));

        store.changePassword("aaaaaa".toCharArray(), "bbbbbb".toCharArray());
        assertTrue(store.isProtected());
        store.lock();
        store.unlock("bbbbbb".toCharArray());
        assertEquals(1, store.getEntries().size());

        // 密码改为无密码
        store.changePassword("bbbbbb".toCharArray(), new char[0]);
        assertFalse(store.isProtected());
        assertTrue(store.isPlainFile());
        store.lock();
        store.unlock(new char[0]);
        assertEquals(1, store.getEntries().size());

        // 无密码改为设置密码（无需原密码）
        store.changePassword(new char[0], "cccccc".toCharArray());
        assertTrue(store.isProtected());
        store.lock();
        store.unlock("cccccc".toCharArray());
        assertEquals(1, store.getEntries().size());
    }

    /** 回归：v1 旧格式文件兼容解锁，并自动升级为 v2。 */
    @Test
    void legacyV1FileCompatible() throws Exception {
        KeyStoreData legacyData = new KeyStoreData();
        legacyData.getScenarios().addAll(java.util.Arrays.asList("开发", "测试", "生产"));
        legacyData.getEntries().add(new KeyEntry("id1", "开发", "old-key", KeyEntryType.SYM_AES,
                java.util.Base64.getEncoder().encodeToString(new byte[16]), "", "2026-01-01T00:00:00"));
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] iv = new byte[12];
        new java.security.SecureRandom().nextBytes(iv);
        byte[] key = com.kitbox.util.Pbkdf2.derive("oldpwd6".toCharArray(), salt, 120000, 256);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(key, "AES"),
                new javax.crypto.spec.GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(com.kitbox.util.Gsons.gson().toJson(legacyData)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.io.ByteArrayOutputStream fileBytes = new java.io.ByteArrayOutputStream();
        fileBytes.write(new byte[]{'C','B','K','S'});
        fileBytes.write(1); // v1 版本号
        fileBytes.write(salt, 0, 16);
        fileBytes.write(iv, 0, 12);
        fileBytes.write(ct, 0, ct.length);
        java.nio.file.Files.write(tempDir.resolve("keystore.dat"), fileBytes.toByteArray());

        KeyStoreManager store = newStore();
        store.unlock("oldpwd6".toCharArray());
        assertEquals(1, store.getEntries("开发").size());
        // 已自动升级为 v2（版本字节=2，模式字节=1）
        byte[] saved = java.nio.file.Files.readAllBytes(tempDir.resolve("keystore.dat"));
        assertEquals(2, saved[4]);
        assertEquals(1, saved[5]);

        store.lock();
        KeyStoreManager store2 = newStore();
        store2.unlock("oldpwd6".toCharArray());
        assertEquals(1, store2.getEntries("开发").size());
    }
}
