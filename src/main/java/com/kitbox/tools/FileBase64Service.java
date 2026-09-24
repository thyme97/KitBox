package com.kitbox.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 文件与 Base64 互转：编码可附带 Data URI 前缀（data: 开头的 base64 URI），
 * 解码自动识别并剥离前缀，容忍换行与空白。
 */
public final class FileBase64Service {

    private static final Pattern DATA_URI_PREFIX = Pattern.compile("^data:[^;,]*;base64,");
    private static final Pattern DATA_URI_MIME = Pattern.compile("^data:([^;,]+)");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    /** 标准/URL 安全字母表 + 填充；MimeDecoder 会静默丢弃非法字符，须先校验 */
    private static final Pattern BASE64_ALPHABET = Pattern.compile("[A-Za-z0-9+/=_-]+");

    private FileBase64Service() {
    }

    /** 文件 → Base64（dataUri 为 true 时输出带 data: 前缀的 Data URI）。 */
    public static String encode(Path file, boolean dataUri) throws IOException {
        return encode(Files.readAllBytes(file), mimeOf(file.getFileName().toString()), dataUri);
    }

    /** 字节数组 → Base64（mime 决定 Data URI 前缀，dataUri 为 false 时忽略）。 */
    public static String encode(byte[] data, String mime, boolean dataUri) {
        String base64 = Base64.getEncoder().encodeToString(data);
        return dataUri ? "data:" + mime + ";base64," + base64 : base64;
    }

    /** Base64 → 字节。自动剥离 data: 前缀与换行/空白。 */
    public static byte[] decode(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入 Base64 内容");
        }
        String stripped = DATA_URI_PREFIX.matcher(text.trim()).replaceFirst("");
        String cleaned = WHITESPACE.matcher(stripped).replaceAll("");
        if (cleaned.isEmpty() || !BASE64_ALPHABET.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("不是合法的 Base64 内容");
        }
        return Base64.getMimeDecoder().decode(cleaned);
    }

    /** 按扩展名推断 MIME 类型（图片为主），未知类型为 application/octet-stream。 */
    public static String mimeOf(String fileName) {
        String name = fileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        switch (ext) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "tif":
            case "tiff":
                return "image/tiff";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "zip":
                return "application/zip";
            case "mp3":
                return "audio/mpeg";
            case "mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }

    /** 根据 Data URI 前缀推荐「另存为」的文件名（无前缀时为 download.bin）。 */
    public static String suggestFileName(String content) {
        String s = content == null ? "" : content.trim();
        java.util.regex.Matcher m = DATA_URI_MIME.matcher(s);
        if (m.find()) {
            switch (m.group(1).toLowerCase(Locale.ROOT)) {
                case "image/png":
                    return "image.png";
                case "image/jpeg":
                    return "image.jpg";
                case "image/gif":
                    return "image.gif";
                case "image/bmp":
                    return "image.bmp";
                case "image/webp":
                    return "image.webp";
                case "image/svg+xml":
                    return "image.svg";
                case "image/x-icon":
                    return "image.ico";
                case "image/tiff":
                    return "image.tiff";
                case "application/pdf":
                    return "document.pdf";
                case "application/zip":
                    return "archive.zip";
                case "text/plain":
                    return "file.txt";
                default: {
                    String mime = m.group(1).toLowerCase(Locale.ROOT);
                    int slash = mime.indexOf('/');
                    String sub = slash >= 0 ? mime.substring(slash + 1) : mime;
                    return "file." + sub.replaceAll("[^a-z0-9]", "");
                }
            }
        }
        return "download.bin";
    }
}
