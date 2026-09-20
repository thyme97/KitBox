package com.kitbox.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecksumServiceTest {

    @TempDir
    Path dir;

    @Test
    void sha256KnownVector() throws Exception {
        Path file = dir.resolve("hello.txt");
        Files.write(file, "hello".getBytes("UTF-8"));
        String hash = ChecksumService.hashFile(file, ChecksumService.FileAlg.SHA256);
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    void sm3KnownVector() throws Exception {
        Path file = dir.resolve("abc.txt");
        Files.write(file, "abc".getBytes("UTF-8"));
        String hash = ChecksumService.hashFile(file, ChecksumService.FileAlg.SM3);
        assertEquals("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0", hash);
    }

    @Test
    void hashFileLargeThanBuffer() throws Exception {
        Path file = dir.resolve("big.bin");
        byte[] data = new byte[50_000];
        Arrays.fill(data, (byte) 0x5A);
        Files.write(file, data);
        String hash = ChecksumService.hashFile(file, ChecksumService.FileAlg.SHA256);
        assertEquals(64, hash.length());
    }

    @Test
    void manifestGenerateParseVerifyRoundTrip() throws Exception {
        Path a = dir.resolve("a.txt");
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        Path b = sub.resolve("b.txt");
        Files.write(a, "content-a".getBytes("UTF-8"));
        Files.write(b, "content-b".getBytes("UTF-8"));

        List<ChecksumService.FileHash> hashes =
                ChecksumService.hashFiles(Arrays.asList(a, b), ChecksumService.FileAlg.SHA256, dir);
        String manifest = ChecksumService.buildManifest(hashes);

        assertTrue(manifest.contains("a.txt"));
        assertTrue(manifest.contains("sub/b.txt") || manifest.contains("sub\\b.txt"));

        List<ChecksumService.ManifestEntry> entries = ChecksumService.parseManifest(manifest);
        assertEquals(2, entries.size());

        List<ChecksumService.VerifyItem> result = ChecksumService.verify(entries, dir, ChecksumService.FileAlg.SHA256);
        assertEquals(2, result.size());
        for (ChecksumService.VerifyItem item : result) {
            assertEquals(ChecksumService.VerifyStatus.OK, item.status, item.name);
        }
    }

    @Test
    void verifyDetectsTamperedAndMissing() throws Exception {
        Path a = dir.resolve("a.txt");
        Path gone = dir.resolve("gone.txt");
        Files.write(a, "original".getBytes("UTF-8"));
        Files.write(gone, "will be deleted".getBytes("UTF-8"));
        List<ChecksumService.FileHash> hashes =
                ChecksumService.hashFiles(Arrays.asList(a, gone), ChecksumService.FileAlg.SHA256, dir);
        List<ChecksumService.ManifestEntry> entries = ChecksumService.parseManifest(ChecksumService.buildManifest(hashes));

        Files.write(a, "tampered!".getBytes("UTF-8"));
        Files.delete(gone);
        List<ChecksumService.VerifyItem> result = ChecksumService.verify(entries, dir, ChecksumService.FileAlg.SHA256);

        ChecksumService.VerifyItem tampered = find(result, "a.txt");
        assertEquals(ChecksumService.VerifyStatus.MISMATCH, tampered.status);
        ChecksumService.VerifyItem missing = find(result, "gone.txt");
        assertEquals(ChecksumService.VerifyStatus.MISSING, missing.status);
    }

    @Test
    void parseAcceptsGnuBinaryFormAndSkipsComments() {
        String manifest = "# comment line\n"
                + "\n"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa  a.txt\n"
                + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb *b.bin\n";
        List<ChecksumService.ManifestEntry> entries = ChecksumService.parseManifest(manifest);
        assertEquals(2, entries.size());
        assertEquals("a.txt", entries.get(0).name);
        assertEquals("b.bin", entries.get(1).name);
    }

    @Test
    void parseRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> ChecksumService.parseManifest("not a manifest"));
        assertThrows(IllegalArgumentException.class, () -> ChecksumService.parseManifest("# only comments"));
    }

    private static ChecksumService.VerifyItem find(List<ChecksumService.VerifyItem> items, String name) {
        for (ChecksumService.VerifyItem item : items) {
            if (item.name.equals(name)) {
                return item;
            }
        }
        throw new AssertionError("未找到条目: " + name);
    }
}
