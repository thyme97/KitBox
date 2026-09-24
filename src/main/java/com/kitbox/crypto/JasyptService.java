package com.kitbox.crypto;

import com.kitbox.crypto.model.JasyptAlgorithm;
import com.kitbox.crypto.model.JasyptIvGenerator;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimplePBEConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jasypt 兼容的配置项文本加解密。
 * <p>
 * 加解密直接委托 jasypt 本体（StandardPBEStringEncryptor），保证与各版本/各 Provider
 * 的存量密文行为一致；解密时自动尝试三种密文布局，按 UTF-8/GBK 可读性打分取最优：
 * <ol>
 * <li>旧默认（NoIvGenerator）：Base64(盐(8) + 密文)，IV 由 PKCS#5 派生；</li>
 * <li>随机 IV（RandomIvGenerator，SunJCE）：Base64(盐(8) + 摆设IV(8) + 密文)，
 *     存储 IV 不参与运算，真实 IV 仍由口令派生（DK[8:16]）；</li>
 * <li>随机 IV（其他 Provider）：Base64(盐(8) + IV(8) + 密文)，存储 IV 真正参与运算。</li>
 * </ol>
 * 三种布局的密钥都是 PBKDF1-MD5 派生的 DK[:8]。jasypt 3.x 的
 * PBEWITHHMACSHA512ANDAES_256 是另一套布局（盐 16 + IV 16，IV 真正生效）。
 * 输入兼容 {@code ENC(...)} 包裹（算法变体见 {@link JasyptAlgorithm}）。
 */
public final class JasyptService {

    /** Jasypt 默认迭代次数 */
    public static final int DEFAULT_ITERATIONS = 1000;
    /** 迭代次数上限：防止在 UI 误填超大值导致卡死 */
    public static final int MAX_ITERATIONS = 10_000_000;

    private static final Pattern ENC_WRAPPER = Pattern.compile("^\\s*ENC\\((.*)\\)\\s*$", Pattern.DOTALL);

    private JasyptService() {
    }

    /** 去掉 ENC(...) 包裹与首尾引号/空白；无包裹则原样返回（trim 后）。 */
    public static String stripWrapper(String input) {
        String s = input.trim();
        for (int i = 0; i < 3; i++) {
            String prev = s;
            Matcher m = ENC_WRAPPER.matcher(s);
            if (m.matches()) {
                s = m.group(1).trim();
            } else if (s.length() >= 2
                    && ((s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"')
                    || (s.charAt(0) == '\'' && s.charAt(s.length() - 1) == '\''))) {
                s = s.substring(1, s.length() - 1).trim();
            }
            if (s.equals(prev)) {
                break;
            }
        }
        return s;
    }

    /**
     * 解密 Jasypt 文本密文（自动剥离 ENC() 包裹）。
     * <p>
     * 解密不依赖调用方指定 IV 生成器：旧算法自动尝试三种布局并取可读性最优者，
     * 供解密不知道（也不需要知道）对方用的是哪种配置的存量密文场景。
     */
    public static String decrypt(String cipherText, char[] password, JasyptAlgorithm algo, int iterations)
            throws CryptoException {
        validateIterations(iterations);
        validatePassword(password);
        if (algo == JasyptAlgorithm.MD5_DES) {
            checkAscii(password);
        }
        String base64 = stripWrapper(cipherText);
        byte[] all = base64Decode(base64);
        int overhead = algo.getSaltLength() + algo.getIvLength();
        if (all.length <= overhead) {
            throw new CryptoException("密文太短：Base64 解码后不足 " + (overhead + 1)
                    + " 字节，无法提取" + (algo.getIvLength() > 0 ? "盐和 IV" : "盐"));
        }

        // 候选顺序即优先级：旧默认布局 → 随机 IV（SunJCE 派生 IV）→ 随机 IV（内嵌 IV，其他 Provider）。
        // 多个候选可能同时填充合法（共享尾块的歧义），按可读性打分取最优，平局取先出现的旧布局。
        List<String> texts = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        List<JasyptIvGenerator> generators = algo == JasyptAlgorithm.MD5_DES
                ? Arrays.asList(JasyptIvGenerator.values())
                : Collections.singletonList(JasyptIvGenerator.RANDOM);
        for (JasyptIvGenerator generator : generators) {
            try {
                addCandidate(texts, scores,
                        createEncryptor(password, algo, generator, iterations).decrypt(base64));
            } catch (RuntimeException ignored) {
                // 口令或布局不匹配（jasypt 统一抛 EncryptionOperationNotPossibleException），尝试下一候选
            }
        }
        if (algo == JasyptAlgorithm.MD5_DES) {
            try {
                addCandidate(texts, scores, decryptWithStoredIv(password, all, iterations));
            } catch (Exception ignored) {
                // 最后一候选失败则按无候选处理
            }
        } else {
            try {
                // jasypt 在 SunJCE 的 PBKDF2 上拒绝非 ASCII 口令，回退手工 PBES2 路径（布局一致）
                addCandidate(texts, scores, decryptAesFallback(password, all, iterations));
            } catch (Exception ignored) {
                // 兜底候选失败则按无候选处理
            }
        }
        if (texts.isEmpty()) {
            throw new CryptoException("解密失败：口令、迭代次数或密文不正确"
                    + "（已尝试旧默认/随机 IV/内嵌 IV 三种布局，填充校验均未通过）");
        }
        int best = 0;
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(best)) {
                best = i;
            }
        }
        return texts.get(best);
    }

    /**
     * 加密为 Jasypt 文本密文；{@code ivGenerator} 决定旧算法（PBEWithMD5AndDES）的布局：
     * 旧默认输出 Base64(盐+密文)，RandomIvGenerator 输出 Base64(盐+IV+密文)（与博客同款配置互通）。
     * AES_256 变体固定携带随机 IV，忽略该参数。{@code wrap} 为 true 时结果用 {@code ENC(...)} 包裹。
     */
    public static String encrypt(String plainText, char[] password, JasyptAlgorithm algo,
                                 JasyptIvGenerator ivGenerator, int iterations, boolean wrap)
            throws CryptoException {
        validateIterations(iterations);
        validatePassword(password);
        if (plainText == null || plainText.isEmpty()) {
            throw new CryptoException("请输入明文");
        }
        if (algo == JasyptAlgorithm.MD5_DES) {
            checkAscii(password);
        }
        try {
            JasyptIvGenerator generator = algo == JasyptAlgorithm.MD5_DES
                    ? ivGenerator
                    : JasyptIvGenerator.RANDOM;
            String base64 = createEncryptor(password, algo, generator, iterations).encrypt(plainText);
            return wrap ? "ENC(" + base64 + ")" : base64;
        } catch (Exception e) {
            if (algo != JasyptAlgorithm.HMAC_SHA512_AES256) {
                throw new CryptoException("加密失败："
                        + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()), e);
            }
            // jasypt 在 SunJCE 的 PBKDF2 上拒绝非 ASCII 口令，回退手工 PBES2 路径（布局一致）
            try {
                byte[] salt = new byte[16];
                byte[] iv = new byte[16];
                new java.security.SecureRandom().nextBytes(salt);
                new java.security.SecureRandom().nextBytes(iv);
                byte[] cipherBytes = aesPbes2Cipher(Cipher.ENCRYPT_MODE,
                        plainText.getBytes(StandardCharsets.UTF_8), password, salt, iv, iterations);
                byte[] out = new byte[32 + cipherBytes.length];
                System.arraycopy(salt, 0, out, 0, 16);
                System.arraycopy(iv, 0, out, 16, 16);
                System.arraycopy(cipherBytes, 0, out, 32, cipherBytes.length);
                String base64 = Base64.getEncoder().encodeToString(out);
                return wrap ? "ENC(" + base64 + ")" : base64;
            } catch (Exception ex) {
                throw new CryptoException("加密失败："
                        + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()), ex);
            }
        }
    }

    private static StandardPBEStringEncryptor createEncryptor(char[] password, JasyptAlgorithm algo,
                                                              JasyptIvGenerator generator, int iterations) {
        SimplePBEConfig config = new SimplePBEConfig();
        config.setAlgorithm(algo.getJasyptName());
        // jasypt API 只接受 String 口令（char[] 无法透传，仅存在于此调用栈内）
        config.setPassword(new String(password));
        config.setKeyObtentionIterations(String.valueOf(iterations));
        config.setIvGeneratorClassName(generator.getClassName());
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 布局 3 兜底：密文 = 盐(8) + 存储IV(8) + 密文，存储 IV 真正参与 CBC 运算
     * （由真正实现内嵌 IV 的 Provider/其他 JDK 加密的存量数据）。密钥仍是 PBKDF1-MD5 的 DK[:8]。
     */
    private static String decryptWithStoredIv(char[] password, byte[] all, int iterations) throws Exception {
        if (all.length <= 16) {
            throw new IllegalStateException("密文太短，无法按内嵌 IV 布局解析");
        }
        byte[] salt = Arrays.copyOfRange(all, 0, 8);
        byte[] dk = pbkdf1Md5(password, salt, iterations);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(Arrays.copyOfRange(dk, 0, 8), "DES"),
                new IvParameterSpec(Arrays.copyOfRange(all, 8, 16)));
        return decodeText(cipher.doFinal(Arrays.copyOfRange(all, 16, all.length)));
    }

    /** PKCS#5 PBKDF1（MD5）：DK = MD5(口令 || 盐) 后再对结果迭代 (iterations-1) 次，输出 16 字节。 */
    private static byte[] pbkdf1Md5(char[] password, byte[] salt, int iterations) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(new String(password).getBytes(StandardCharsets.US_ASCII));
        md.update(salt);
        byte[] dk = md.digest();
        for (int i = 1; i < iterations; i++) {
            md.reset();
            dk = md.digest(dk);
        }
        return dk;
    }

    /** AES_256 手工 PBES2 路径：PBKDF2WithHmacSHA512 派生 256 位密钥 + AES/CBC/PKCS5Padding。 */
    private static byte[] aesPbes2Cipher(int opmode, byte[] input, char[] password,
                                         byte[] salt, byte[] iv, int iterations) throws Exception {
        javax.crypto.SecretKeyFactory factory =
                javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] key = factory.generateSecret(
                new javax.crypto.spec.PBEKeySpec(password, salt, iterations, 256)).getEncoded();
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(input);
    }

    /** AES_256 布局（盐 16 + IV 16 + 密文）的手工解密，作为非 ASCII 口令时 jasypt 的兜底。 */
    private static String decryptAesFallback(char[] password, byte[] all, int iterations) throws Exception {
        if (all.length <= 32) {
            throw new IllegalStateException("密文太短，无法按盐+IV+密文布局解析");
        }
        byte[] plain = aesPbes2Cipher(Cipher.DECRYPT_MODE,
                Arrays.copyOfRange(all, 32, all.length), password,
                Arrays.copyOfRange(all, 0, 16), Arrays.copyOfRange(all, 16, 32), iterations);
        return decodeText(plain);
    }

    /** 尽量可读地解码明文字节：严格 UTF-8 → 严格 GBK → 宽松 UTF-8。 */
    private static String decodeText(byte[] plain) {
        String utf8 = strictDecode(StandardCharsets.UTF_8, plain);
        if (utf8 != null) {
            return utf8;
        }
        String gbk = strictDecode(Charset.forName("GBK"), plain);
        return gbk != null ? gbk : new String(plain, StandardCharsets.UTF_8);
    }

    /** 候选打分：含 U+FFFD 视为乱码 0 分；可读（非控制字符占比 ≥ 90%）3 分；其余 1 分。 */
    private static void addCandidate(List<String> texts, List<Integer> scores, String text) {
        texts.add(text);
        scores.add(text.contains("\uFFFD") || printableRatio(text) < 0.9 ? 1 : 3);
    }

    private static double printableRatio(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        int printable = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isISOControl(c) && c != '\uFFFD') {
                printable++;
            }
        }
        return (double) printable / text.length();
    }

    private static String strictDecode(Charset charset, byte[] data) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data))
                    .toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static byte[] base64Decode(String text) throws CryptoException {
        try {
            return Base64.getMimeDecoder().decode(text);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密文不是合法的 Base64 内容", e);
        }
    }

    private static void validateIterations(int iterations) throws CryptoException {
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new CryptoException("迭代次数必须在 1 到 " + MAX_ITERATIONS + " 之间");
        }
    }

    private static void validatePassword(char[] password) throws CryptoException {
        if (password == null || password.length == 0) {
            throw new CryptoException("请输入口令");
        }
    }

    /** PBES1（PBEWithMD5AndDES）的 JCE 实现只接受 ASCII 口令，提前校验给出友好提示。 */
    private static void checkAscii(char[] password) throws CryptoException {
        for (char c : password) {
            if (c > 127) {
                throw new CryptoException("PBEWithMD5AndDES 的口令必须为 ASCII 字符（当前包含非 ASCII 字符）");
            }
        }
    }
}
