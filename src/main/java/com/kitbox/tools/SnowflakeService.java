package com.kitbox.tools;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 雪花 ID（Snowflake）：1 位符号 + 41 位毫秒时间戳 + 5 位数据中心 + 5 位机器 + 12 位序列。
 * 纪元与 MyBatis-Plus 等主流实现一致（1288834974657），生成的 ID 可被这些实现解析。
 */
public final class SnowflakeService {

    public static final long TWEPOCH = 1288834974657L;
    public static final long MAX_WORKER_ID = 31L;
    public static final long MAX_DATACENTER_ID = 31L;

    private static final long WORKER_ID_SHIFT = 12L;
    private static final long DATACENTER_ID_SHIFT = 17L;
    private static final long TIMESTAMP_SHIFT = 22L;
    private static final long SEQUENCE_MASK = 4095L;

    private static final DateTimeFormatter DATETIME_MS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Object LOCK = new Object();
    private static long workerId = 0L;
    private static long datacenterId = 0L;
    private static long sequence = 0L;
    private static long lastTimestamp = -1L;

    /** 一条雪花 ID 的解析结果。 */
    public static final class Parsed {
        public final long id;
        public final long epochMillis;
        public final long datacenterId;
        public final long workerId;
        public final long sequence;
        public final String datetime;

        public Parsed(long id, long epochMillis, long datacenterId, long workerId, long sequence, String datetime) {
            this.id = id;
            this.epochMillis = epochMillis;
            this.datacenterId = datacenterId;
            this.workerId = workerId;
            this.sequence = sequence;
            this.datetime = datetime;
        }
    }

    private SnowflakeService() {
    }

    /** 配置机器与数据中心编号（重置序列号，时间基准保留）。 */
    public static void configure(long workerId, long datacenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("机器 ID 需在 0 ~ 31 之间");
        }
        if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException("数据中心 ID 需在 0 ~ 31 之间");
        }
        synchronized (LOCK) {
            SnowflakeService.workerId = workerId;
            SnowflakeService.datacenterId = datacenterId;
            sequence = 0L;
        }
    }

    /** 批量生成（同毫秒内按序列号递增，整体单调）。 */
    public static List<String> generate(int count) {
        if (count < 1 || count > 1000) {
            throw new IllegalArgumentException("数量需在 1 ~ 1000 之间");
        }
        synchronized (LOCK) {
            List<String> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(String.valueOf(nextId()));
            }
            return ids;
        }
    }

    /** 解析雪花 ID：支持十进制与 0x 开头的十六进制。 */
    public static Parsed parse(String idText) {
        String text = idText.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("请输入要解析的雪花 ID");
        }
        String digits = text;
        int radix = 10;
        if (text.startsWith("0x") || text.startsWith("0X")) {
            digits = text.substring(2);
            radix = 16;
        }
        long id;
        try {
            id = Long.parseLong(digits, radix);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("「" + text + "」不是合法的雪花 ID");
        }
        if (id < 0) {
            throw new IllegalArgumentException("雪花 ID 不能为负数");
        }
        long epochMillis = (id >> TIMESTAMP_SHIFT) + TWEPOCH;
        long dc = (id >> DATACENTER_ID_SHIFT) & MAX_DATACENTER_ID;
        long worker = (id >> WORKER_ID_SHIFT) & MAX_WORKER_ID;
        long seq = id & SEQUENCE_MASK;
        String datetime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
                .format(DATETIME_MS);
        return new Parsed(id, epochMillis, dc, worker, seq, datetime);
    }

    private static long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("检测到时钟回拨 " + (lastTimestamp - timestamp) + " 毫秒，暂停生成");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                while ((timestamp = System.currentTimeMillis()) <= lastTimestamp) {
                    // 同毫秒序列耗尽，自旋等待下一毫秒
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - TWEPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
}
