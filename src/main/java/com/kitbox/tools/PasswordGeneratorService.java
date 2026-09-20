package com.kitbox.tools;

import com.kitbox.crypto.model.DataEncoding;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 密码 / 随机密钥生成：SecureRandom 驱动，
 * 密码按字符集策略保证每个勾选类别至少出现一次，随机密钥按字节长度输出 Base64 / Hex。
 */
public final class PasswordGeneratorService {

    private static final SecureRandom RNG = new SecureRandom();

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.<>?/";

    /** 易混淆字符：0/O、1/l/I 等等。 */
    private static final String AMBIGUOUS = "0O1lI";

    private PasswordGeneratorService() {
    }

    /**
     * 批量生成密码。
     *
     * @param length           密码长度（1~256）
     * @param upper            含大写字母
     * @param lower            含小写字母
     * @param digits           含数字
     * @param symbols          含符号
     * @param excludeAmbiguous 排除易混淆字符（如 0O1lI）
     * @param count            生成数量（1~100）
     * @return 密码列表
     * @throws IllegalArgumentException 参数不合法
     */
    public static List<String> generatePasswords(int length, boolean upper, boolean lower,
                                                 boolean digits, boolean symbols,
                                                 boolean excludeAmbiguous, int count) {
        List<String> pools = new ArrayList<>();
        if (upper) {
            pools.add(UPPER);
        }
        if (lower) {
            pools.add(LOWER);
        }
        if (digits) {
            pools.add(DIGITS);
        }
        if (symbols) {
            pools.add(SYMBOLS);
        }
        if (pools.isEmpty()) {
            throw new IllegalArgumentException("请至少勾选一种字符类别");
        }
        if (length < pools.size() || length > 256) {
            throw new IllegalArgumentException("长度需在 " + pools.size() + " ~ 256 之间");
        }
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("数量需在 1 ~ 100 之间");
        }

        List<String> effectivePools = new ArrayList<>();
        for (String pool : pools) {
            effectivePools.add(excludeAmbiguous ? removeChars(pool, AMBIGUOUS) : pool);
        }
        StringBuilder union = new StringBuilder();
        for (String pool : effectivePools) {
            union.append(pool);
        }

        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(generateOne(length, effectivePools, union.toString()));
        }
        return result;
    }

    /**
     * 批量生成随机密钥：numBytes 字节随机数按 Base64 / Hex 编码。
     *
     * @param numBytes 字节数（8~512）
     */
    public static List<String> generateKeys(int numBytes, DataEncoding encoding, int count) {
        if (numBytes < 8 || numBytes > 512) {
            throw new IllegalArgumentException("密钥字节数需在 8 ~ 512 之间");
        }
        if (encoding == null) {
            throw new IllegalArgumentException("请选择编码格式");
        }
        if (count < 1 || count > 100) {
            throw new IllegalArgumentException("数量需在 1 ~ 100 之间");
        }
        List<String> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] bytes = new byte[numBytes];
            RNG.nextBytes(bytes);
            result.add(encoding.encode(bytes));
        }
        return result;
    }

    /** 每个勾选类别先保证一个，剩余从并集随机取，最后 Fisher-Yates 洗牌。 */
    private static String generateOne(int length, List<String> pools, String union) {
        List<Character> chars = new ArrayList<>(length);
        for (String pool : pools) {
            chars.add(pool.charAt(RNG.nextInt(pool.length())));
        }
        for (int i = chars.size(); i < length; i++) {
            chars.add(union.charAt(RNG.nextInt(union.length())));
        }
        Collections.shuffle(chars, RNG);
        StringBuilder sb = new StringBuilder(length);
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static String removeChars(String source, String remove) {
        StringBuilder sb = new StringBuilder(source.length());
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (remove.indexOf(c) < 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
