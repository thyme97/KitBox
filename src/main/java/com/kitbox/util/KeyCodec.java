package com.kitbox.util;

import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.model.KeyFormat;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
/**
 * 密钥编解码工具：
 * - RSA / SM2 密钥对生成
 * - RSA 公私钥解析（X.509 / PKCS8 / PKCS1 PEM / 裸 Base64 / Hex）
 * - SM2 公私钥解析（X.509 / PKCS8 / 裸点 65B(04|X|Y) / 裸私钥 32B）
 * - JCA 密钥与 PEM 的互转
 */
public final class KeyCodec {

    public static final String SM2_CURVE_NAME = "sm2p256v1";
    public static final ECDomainParameters SM2_DOMAIN;

    static {
        org.bouncycastle.asn1.x9.X9ECParameters x9 = GMNamedCurves.getByName(SM2_CURVE_NAME);
        SM2_DOMAIN = new ECDomainParameters(x9.getCurve(), x9.getG(), x9.getN(), x9.getH());
    }

    private KeyCodec() {
    }

    // ---------------- 生成 ----------------

    public static KeyPair generateRsaKeyPair(int keyBits) throws CryptoException {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keyBits, new SecureRandom());
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new CryptoException("生成 RSA 密钥对失败：" + e.getMessage(), e);
        }
    }

    public static KeyPair generateSm2KeyPair() throws CryptoException {
        try {
            org.bouncycastle.crypto.generators.ECKeyPairGenerator gen =
                    new org.bouncycastle.crypto.generators.ECKeyPairGenerator();
            gen.init(new org.bouncycastle.crypto.params.ECKeyGenerationParameters(SM2_DOMAIN, new SecureRandom()));
            org.bouncycastle.crypto.AsymmetricCipherKeyPair kp = gen.generateKeyPair();
            BigInteger d = ((ECPrivateKeyParameters) kp.getPrivate()).getD();
            org.bouncycastle.math.ec.ECPoint q = ((ECPublicKeyParameters) kp.getPublic()).getQ();
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SM2_CURVE_NAME);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            PrivateKey priv = kf.generatePrivate(new ECPrivateKeySpec(d, spec));
            PublicKey pub = kf.generatePublic(new ECPublicKeySpec(q, spec));
            return new KeyPair(pub, priv);
        } catch (Exception e) {
            throw new CryptoException("生成 SM2 密钥对失败：" + e.getMessage(), e);
        }
    }

    // ---------------- RSA 解析 ----------------

    public static PublicKey parseRsaPublicKey(String text, KeyFormat fmt) throws CryptoException {
        byte[] der = readKeyBytes(text, fmt);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new CryptoException("无法解析 RSA 公钥：请确认内容为 X.509 格式（Base64/PEM）或 PKCS1 PEM");
        }
    }

    public static PrivateKey parseRsaPrivateKey(String text, KeyFormat fmt) throws CryptoException {
        byte[] der = readKeyBytes(text, fmt);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new CryptoException("无法解析 RSA 私钥：请确认内容为 PKCS8 格式（Base64/PEM）或 PKCS1 PEM");
        }
    }

    // ---------------- SM2 解析 ----------------

    public static ECPublicKeyParameters parseSm2PublicKey(String text, KeyFormat fmt) throws CryptoException {
        byte[] raw = readKeyBytes(text, fmt);
        // 裸点格式：04|X|Y（65字节）或 X|Y（64字节）
        if (raw.length == 65 && raw[0] == 0x04) {
            try {
                ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SM2_CURVE_NAME);
                org.bouncycastle.math.ec.ECPoint q = spec.getCurve().decodePoint(raw);
                return new ECPublicKeyParameters(q, SM2_DOMAIN);
            } catch (IllegalArgumentException e) {
                throw new CryptoException("该公钥点不在 SM2（sm2p256v1）曲线上，请确认这对密钥是否为 SM2 曲线密钥");
            }
        }
        if (raw.length == 64) {
            try {
                ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SM2_CURVE_NAME);
                byte[] withPrefix = new byte[65];
                withPrefix[0] = 0x04;
                System.arraycopy(raw, 0, withPrefix, 1, 64);
                org.bouncycastle.math.ec.ECPoint q = spec.getCurve().decodePoint(withPrefix);
                return new ECPublicKeyParameters(q, SM2_DOMAIN);
            } catch (IllegalArgumentException ignored) {
            }
        }
        try {
            AsymmetricKeyParameter key = PublicKeyFactory.createKey(raw);
            if (key instanceof ECPublicKeyParameters) {
                ECPublicKeyParameters ec = (ECPublicKeyParameters) key;
                // 确认是 SM2 曲线
                if (!SM2_DOMAIN.getCurve().equals(ec.getParameters().getCurve())) {
                    throw new CryptoException("该椭圆曲线不是 SM2（sm2p256v1）");
                }
                return ec;
            }
            if (key instanceof RSAKeyParameters) {
                throw new CryptoException("解析到的是 RSA 公钥，请切换到 RSA 算法");
            }
            throw new CryptoException("不支持的公钥类型");
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("无法解析 SM2 公钥：支持 X.509(Base64/PEM) 或裸点 04|X|Y；"
                    + "若粘贴的是 Hex 请确认『格式』为 Hex");
        }
    }

    public static ECPrivateKeyParameters parseSm2PrivateKey(String text, KeyFormat fmt) throws CryptoException {
        byte[] raw = readKeyBytes(text, fmt);
        // 裸私钥：32 字节大端 d 值；容忍部分工具导出时补符号位的前导 00（33/34 字节）
        if (raw.length >= 32 && raw.length <= 34) {
            int first = 0;
            while (first < raw.length - 32 && raw[first] == 0x00) {
                first++;
            }
            if (raw.length - first == 32) {
                byte[] key32 = new byte[32];
                System.arraycopy(raw, first, key32, 0, 32);
                raw = key32;
            }
        }
        if (raw.length == 32) {
            BigInteger d = new BigInteger(1, raw);
            if (d.signum() <= 0) {
                throw new CryptoException("SM2 裸私钥值非法");
            }
            return new ECPrivateKeyParameters(d, SM2_DOMAIN);
        }
        try {
            AsymmetricKeyParameter key = PrivateKeyFactory.createKey(raw);
            if (key instanceof ECPrivateKeyParameters) {
                ECPrivateKeyParameters ec = (ECPrivateKeyParameters) key;
                if (!SM2_DOMAIN.getCurve().equals(ec.getParameters().getCurve())) {
                    throw new CryptoException("该椭圆曲线不是 SM2（sm2p256v1）");
                }
                return ec;
            }
            if (key instanceof RSAKeyParameters) {
                throw new CryptoException("解析到的是 RSA 私钥，请切换到 RSA 算法");
            }
            throw new CryptoException("不支持的私钥类型");
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("无法解析 SM2 私钥：支持 PKCS8(Base64/PEM) 或 32 字节裸私钥(Hex/Base64)；"
                    + "若粘贴的是 Hex 请确认『格式』为 Hex");
        }
    }

    // ---------------- 导出 ----------------

    public static String toBase64(PublicKey key) {
        return java.util.Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String toBase64(PrivateKey key) {
        return java.util.Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static String toPem(PublicKey key) {
        return PemUtils.buildPem(PemUtils.TYPE_PUBLIC_KEY, key.getEncoded());
    }

    public static String toPem(PrivateKey key) {
        return PemUtils.buildPem(PemUtils.TYPE_PRIVATE_KEY, key.getEncoded());
    }

    /** Base64(X.509) 公钥 → PEM。 */
    public static String pemFromPublicBase64(String base64) {
        byte[] der = java.util.Base64.getDecoder().decode(base64.replaceAll("\\s+", ""));
        return PemUtils.buildPem(PemUtils.TYPE_PUBLIC_KEY, der);
    }

    /** Base64(PKCS8) 私钥 → PEM。 */
    public static String pemFromPrivateBase64(String base64) {
        byte[] der = java.util.Base64.getDecoder().decode(base64.replaceAll("\\s+", ""));
        return PemUtils.buildPem(PemUtils.TYPE_PRIVATE_KEY, der);
    }

    /** SM2 公钥（X.509 Base64）→ 裸点编码（04|X|Y，65 字节）的 Hex。 */
    public static String sm2PublicRawHex(String jcaPublicBase64) throws CryptoException {
        try {
            return HexUtils.encode(parseSm2PublicKey(jcaPublicBase64, KeyFormat.BASE64).getQ().getEncoded(false));
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("SM2 公钥裸点编码失败：" + e.getMessage(), e);
        }
    }

    /** SM2 私钥（PKCS8 Base64）→ 裸私钥 D 的 Hex（左填充到 32 字节）。 */
    public static String sm2PrivateRawHex(String jcaPrivateBase64) throws CryptoException {
        try {
            byte[] raw = new byte[32];
            byte[] src = parseSm2PrivateKey(jcaPrivateBase64, KeyFormat.BASE64).getD().toByteArray();
            if (src.length == 32) {
                raw = src;
            } else if (src.length > 32) {
                System.arraycopy(src, src.length - 32, raw, 0, 32);
            } else {
                System.arraycopy(src, 0, raw, 32 - src.length, src.length);
            }
            return HexUtils.encode(raw);
        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("SM2 私钥裸值编码失败：" + e.getMessage(), e);
        }
    }

    /** SM2 轻量公钥参数 → X.509 DER 的 Base64（便于存储与交换）。 */
    public static String toBase64JcaPublic(ECPublicKeyParameters params) throws CryptoException {
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SM2_CURVE_NAME);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            PublicKey pub = kf.generatePublic(new ECPublicKeySpec(params.getQ(), spec));
            return toBase64(pub);
        } catch (Exception e) {
            throw new CryptoException("SM2 公钥编码失败：" + e.getMessage(), e);
        }
    }

    /** SM2 轻量私钥参数 → PKCS8 DER 的 Base64。 */
    public static String toBase64JcaPrivate(ECPrivateKeyParameters params) throws CryptoException {
        try {
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SM2_CURVE_NAME);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            PrivateKey priv = kf.generatePrivate(new ECPrivateKeySpec(params.getD(), spec));
            return toBase64(priv);
        } catch (Exception e) {
            throw new CryptoException("SM2 私钥编码失败：" + e.getMessage(), e);
        }
    }

    // ---------------- 内部工具 ----------------

    private static byte[] readKeyBytes(String text, KeyFormat fmt) throws CryptoException {
        if (text == null || text.trim().isEmpty()) {
            throw new CryptoException("密钥内容为空");
        }
        String trimmed = text.trim();
        if (trimmed.contains("-----BEGIN")) {
            String type = PemUtils.readPemType(trimmed);
            byte[] content = PemUtils.readPemContent(trimmed);
            return normalizePem(type, content);
        }
        switch (fmt) {
            case HEX:
                return hexThenBase64(trimmed);
            case BASE64:
                return base64ThenHex(trimmed);
            case PEM:
                // 无 PEM 头则按裸 Base64 解析
                return java.util.Base64.getDecoder().decode(trimmed.replaceAll("\\s+", ""));
            case PLAIN:
            default:
                throw new CryptoException("非对称密钥请使用 Base64 / Hex / PEM 格式");
        }
    }

    /** 用户常把 Hex 贴进 Base64（或反之），失败时按另一格式重试一次。 */
    private static byte[] hexThenBase64(String text) throws CryptoException {
        try {
            return HexUtils.decode(text);
        } catch (RuntimeException e) {
            try {
                return java.util.Base64.getDecoder().decode(text.replaceAll("\\s+", ""));
            } catch (RuntimeException e2) {
                throw new CryptoException("内容既不是合法 Hex 也不是 Base64，请检查『格式』选择");
            }
        }
    }

    private static byte[] base64ThenHex(String text) throws CryptoException {
        String compact = text.replaceAll("\\s+", "");
        // 纯十六进制字符串几乎总能被 Base64 解码器“静默”解出乱数据（0-9A-F 都是合法 Base64 字符），
        // 而真实密钥的 Base64 几乎必然含 M/I/G 等非 hex 字符，故纯 hex 内容优先按 Hex 解。
        if (compact.matches("(?i)[0-9a-f]+") && compact.length() % 2 == 0) {
            return HexUtils.decode(compact);
        }
        try {
            return java.util.Base64.getDecoder().decode(compact);
        } catch (RuntimeException e) {
            throw new CryptoException("内容不是合法 Base64；若是 Hex，请将『格式』切换为 Hex");
        }
    }

    /** PKCS1 PEM 转 X.509/PKCS8 DER，其余原样返回。 */
    private static byte[] normalizePem(String pemType, byte[] content) throws CryptoException {
        try {
            if (PemUtils.TYPE_RSA_PUBLIC_KEY.equals(pemType)) {
                org.bouncycastle.asn1.pkcs.RSAPublicKey pkcs1 =
                        org.bouncycastle.asn1.pkcs.RSAPublicKey.getInstance(ASN1Sequence.fromByteArray(content));
                SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(
                        new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
                        (ASN1Encodable) pkcs1);
                return spki.getEncoded(ASN1Encoding.DER);
            }
            if (PemUtils.TYPE_RSA_PRIVATE_KEY.equals(pemType)) {
                org.bouncycastle.asn1.pkcs.RSAPrivateKey pkcs1 =
                        org.bouncycastle.asn1.pkcs.RSAPrivateKey.getInstance(ASN1Sequence.fromByteArray(content));
                org.bouncycastle.asn1.pkcs.PrivateKeyInfo pki = new org.bouncycastle.asn1.pkcs.PrivateKeyInfo(
                        new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
                        (ASN1Encodable) pkcs1);
                return pki.getEncoded(ASN1Encoding.DER);
            }
            return content;
        } catch (Exception e) {
            throw new CryptoException("PEM(PKCS1) 转换失败：" + e.getMessage(), e);
        }
    }

    /** 便捷判断：JCA 密钥是否为 RSA。 */
    public static boolean isRsaKey(PublicKey key) {
        return key instanceof RSAPublicKey;
    }

    public static boolean isRsaKey(PrivateKey key) {
        return key instanceof RSAPrivateKey;
    }
}
