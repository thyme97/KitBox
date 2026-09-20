package com.kitbox.tools;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 转换工具：Unix 时间戳 ⇄ 日期时间、任意进制互转、批量 UUID。
 */
public final class ConvertToolsService {

    /** 展示用日期时间格式（秒级精度；毫秒走带毫秒的解析）。 */
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATETIME_MS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** 绝对值达到 10^12 视为毫秒（覆盖到 33658 年的秒级时间戳）。 */
    private static final long MS_THRESHOLD = 1_000_000_000_000L;

    private ConvertToolsService() {
    }

    /** 当前时间的时间戳字符串。 */
    public static String nowTimestamp(boolean millis) {
        long now = System.currentTimeMillis();
        return String.valueOf(millis ? now : now / 1000);
    }

    /**
     * 时间戳 → 日期时间（本机时区）。10/13 位以外的长度会得到超出常规的时间，照样换算。
     *
     * @return 形如 2026-01-02 03:04:05（毫秒输入带 .SSS）
     */
    public static String timestampToDatetime(String timestamp) {
        long value = parseLong(timestamp.trim(), "时间戳");
        Instant instant = Math.abs(value) >= MS_THRESHOLD
                ? Instant.ofEpochMilli(value)
                : Instant.ofEpochSecond(value);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return Math.abs(value) >= MS_THRESHOLD
                ? dateTime.format(DATETIME_MS)
                : dateTime.format(DATETIME);
    }

    /**
     * 日期时间 → 时间戳。支持「yyyy-MM-dd HH:mm:ss」与带 .SSS 毫秒的写法。
     *
     * @param millis true 返回毫秒，false 返回秒
     */
    public static String datetimeToTimestamp(String datetime, boolean millis) {
        String text = datetime.trim();
        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(text, DATETIME_MS);
        } catch (DateTimeParseException ignored) {
            try {
                dateTime = LocalDateTime.parse(text, DATETIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("日期时间格式应为 yyyy-MM-dd HH:mm:ss");
            }
        }
        long epoch = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        return String.valueOf(millis ? epoch : epoch / 1000);
    }

    /**
     * 任意进制互转（2~36），支持负数与任意长度大整数。
     *
     * @return 目标进制字符串（小写）
     */
    public static String convertRadix(String value, int fromRadix, int toRadix) {
        if (fromRadix < 2 || fromRadix > 36 || toRadix < 2 || toRadix > 36) {
            throw new IllegalArgumentException("进制范围是 2 ~ 36");
        }
        String text = value.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("请输入要转换的数值");
        }
        try {
            return new BigInteger(text, fromRadix).toString(toRadix);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("「" + text + "」不是合法的 " + fromRadix + " 进制数值");
        }
    }

    /**
     * 批量生成 UUID v4。
     *
     * @param noDashes 去掉连字符
     * @param uppercase 大写
     */
    public static List<String> generateUuids(int count, boolean uppercase, boolean noDashes) {
        if (count < 1 || count > 1000) {
            throw new IllegalArgumentException("数量需在 1 ~ 1000 之间");
        }
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String uuid = UUID.randomUUID().toString();
            if (noDashes) {
                uuid = uuid.replace("-", "");
            }
            if (uppercase) {
                uuid = uuid.toUpperCase();
            }
            result.add(uuid);
        }
        return result;
    }

    private static long parseLong(String text, String label) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("「" + text + "」不是合法的" + label);
        }
    }
}
