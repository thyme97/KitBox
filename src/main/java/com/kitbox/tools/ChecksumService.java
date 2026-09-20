package com.kitbox.tools;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件批量校验：流式计算 MD5 / SHA-1 / SHA-256 / SM3，
 * 生成与解析 GNU 校验和清单（如 .sha256），并按目录逐文件比对。
 */
public final class ChecksumService {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final int BUFFER_SIZE = 8 * 1024;

    /** 支持的文件摘要算法。 */
    public enum FileAlg {
        MD5("MD5", "MD5"),
        SHA1("SHA-1", "SHA-1"),
        SHA256("SHA-256", "SHA-256"),
        SM3("SM3", "SM3");

        private final String display;
        private final String jceName;

        FileAlg(String display, String jceName) {
            this.display = display;
            this.jceName = jceName;
        }

        public String getDisplay() {
            return display;
        }

        MessageDigest digest() throws NoSuchAlgorithmException {
            return MessageDigest.getInstance(jceName);
        }

        @Override
        public String toString() {
            return display;
        }
    }

    /** 单个文件的哈希结果。 */
    public static final class FileHash {
        public final String name;
        public final long size;
        public final String hash;

        public FileHash(String name, long size, String hash) {
            this.name = name;
            this.size = size;
            this.hash = hash;
        }
    }

    /** 清单条目：哈希 + 文件名。 */
    public static final class ManifestEntry {
        public final String hash;
        public final String name;

        public ManifestEntry(String hash, String name) {
            this.hash = hash;
            this.name = name;
        }
    }

    /** 校验状态。 */
    public enum VerifyStatus { OK, MISMATCH, MISSING }

    /** 单个文件的校验结果。 */
    public static final class VerifyItem {
        public final String name;
        public final String expected;
        public final String actual;
        public final VerifyStatus status;

        public VerifyItem(String name, String expected, String actual, VerifyStatus status) {
            this.name = name;
            this.expected = expected;
            this.actual = actual;
            this.status = status;
        }
    }

    private ChecksumService() {
    }

    /** 流式计算文件哈希，返回小写十六进制。 */
    public static String hashFile(Path file, FileAlg alg) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = alg.digest();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
        }
        return hex(md.digest());
    }

    /** 批量计算（name 取相对 baseDir 的路径，无法相对时取文件名）。 */
    public static List<FileHash> hashFiles(List<Path> files, FileAlg alg, Path baseDir)
            throws IOException, NoSuchAlgorithmException {
        List<FileHash> result = new ArrayList<>();
        for (Path file : files) {
            result.add(new FileHash(displayName(file, baseDir), Files.size(file), hashFile(file, alg)));
        }
        return result;
    }

    /** 生成 GNU 风格清单文本：<小写哈希>  <相对路径>。 */
    public static String buildManifest(List<FileHash> hashes) {
        StringBuilder sb = new StringBuilder();
        for (FileHash h : hashes) {
            sb.append(h.hash).append("  ").append(h.name).append('\n');
        }
        return sb.toString();
    }

    /**
     * 解析清单：支持「哈希␣␣文件名」「哈希␣*文件名」两种 GNU 形式，
     * 跳过空行与 # 注释；格式非法的行抛 IllegalArgumentException。
     */
    public static List<ManifestEntry> parseManifest(String text) {
        List<ManifestEntry> entries = new ArrayList<>();
        int lineNo = 0;
        for (String rawLine : text.split("\r?\n")) {
            lineNo++;
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String name;
            if (line.contains("  ")) {
                int idx = line.indexOf("  ");
                name = line.substring(idx + 2).trim();
            } else {
                int idx = line.indexOf(' ');
                if (idx <= 0) {
                    throw new IllegalArgumentException("第 " + lineNo + " 行不是「哈希  文件名」格式");
                }
                name = line.substring(idx + 1).trim();
                if (name.startsWith("*")) {
                    name = name.substring(1);
                }
            }
            String hash = line.substring(0, line.indexOf(' ')).trim();
            if (!hash.matches("[0-9a-fA-F]+") || hash.length() < 8 || name.isEmpty()) {
                throw new IllegalArgumentException("第 " + lineNo + " 行不是「哈希  文件名」格式");
            }
            entries.add(new ManifestEntry(hash.toLowerCase(), name));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("清单里没有可校验的条目");
        }
        return entries;
    }

    /**
     * 逐条校验：dir 为清单所在目录（相对目录的基准）。
     * 文件不存在 → MISSING；哈希不一致 → MISMATCH；否则 OK。
     */
    public static List<VerifyItem> verify(List<ManifestEntry> entries, Path dir, FileAlg alg)
            throws IOException, NoSuchAlgorithmException {
        List<VerifyItem> result = new ArrayList<>();
        for (ManifestEntry e : entries) {
            Path file = dir.resolve(e.name).normalize();
            if (!Files.isRegularFile(file)) {
                result.add(new VerifyItem(e.name, e.hash, "", VerifyStatus.MISSING));
                continue;
            }
            String actual = hashFile(file, alg);
            VerifyStatus status = actual.equalsIgnoreCase(e.hash) ? VerifyStatus.OK : VerifyStatus.MISMATCH;
            result.add(new VerifyItem(e.name, e.hash, actual, status));
        }
        return result;
    }

    private static String displayName(Path file, Path baseDir) {
        if (baseDir == null) {
            return file.getFileName().toString();
        }
        Path normalized = file.normalize();
        Path base = baseDir.normalize();
        if (normalized.startsWith(base)) {
            Path rel = base.relativize(normalized);
            return rel.toString().isEmpty() ? normalized.getFileName().toString() : rel.toString();
        }
        return normalized.getFileName().toString();
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
