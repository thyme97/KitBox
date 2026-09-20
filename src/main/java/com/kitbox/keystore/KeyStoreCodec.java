package com.kitbox.keystore;

import com.google.gson.JsonObject;
import com.kitbox.crypto.CryptoException;
import com.kitbox.crypto.model.DataEncoding;
import com.kitbox.crypto.model.KeyFormat;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import com.kitbox.util.KeyCodec;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * 密钥条目值 ⇆ 可用密钥对象 的转换与规范化。
 * <p>
 * 条目 value 统一存储规范格式：
 * - 对称/HMAC：原始字节 Base64
 * - RSA 公钥/私钥：X.509 / PKCS8 DER 的 Base64
 * - SM2 公钥/私钥：X.509 / PKCS8 DER 的 Base64（导入时把裸点/裸私钥规范化）
 * - 密钥对：JSON {"publicKey":"<Base64>","privateKey":"<Base64>"}
 */
public final class KeyStoreCodec {

    private KeyStoreCodec() {
    }

    /** SM2 密钥对（轻量参数形式）。 */
    public static final class Sm2KeyPair {
        public final org.bouncycastle.crypto.params.ECPublicKeyParameters publicKey;
        public final org.bouncycastle.crypto.params.ECPrivateKeyParameters privateKey;

        Sm2KeyPair(org.bouncycastle.crypto.params.ECPublicKeyParameters publicKey,
                  org.bouncycastle.crypto.params.ECPrivateKeyParameters privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
    }

    // ---------------- 读取 ----------------

    public static byte[] symmetricKeyBytes(KeyEntry entry) {
        return Base64.getDecoder().decode(entry.getValue());
    }

    public static PublicKey rsaPublicKey(KeyEntry entry) throws CryptoException {
        if (entry.getType() == KeyEntryType.RSA_KEYPAIR) {
            return KeyCodec.parseRsaPublicKey(keyPairJson(entry.getValue()).get("publicKey").getAsString(), KeyFormat.BASE64);
        }
        return KeyCodec.parseRsaPublicKey(entry.getValue(), KeyFormat.BASE64);
    }

    public static PrivateKey rsaPrivateKey(KeyEntry entry) throws CryptoException {
        if (entry.getType() == KeyEntryType.RSA_KEYPAIR) {
            return KeyCodec.parseRsaPrivateKey(keyPairJson(entry.getValue()).get("privateKey").getAsString(), KeyFormat.BASE64);
        }
        return KeyCodec.parseRsaPrivateKey(entry.getValue(), KeyFormat.BASE64);
    }

    public static org.bouncycastle.crypto.params.ECPublicKeyParameters sm2PublicKey(KeyEntry entry)
            throws CryptoException {
        if (entry.getType() == KeyEntryType.SM2_KEYPAIR) {
            return KeyCodec.parseSm2PublicKey(keyPairJson(entry.getValue()).get("publicKey").getAsString(), KeyFormat.BASE64);
        }
        return KeyCodec.parseSm2PublicKey(entry.getValue(), KeyFormat.BASE64);
    }

    public static org.bouncycastle.crypto.params.ECPrivateKeyParameters sm2PrivateKey(KeyEntry entry)
            throws CryptoException {
        if (entry.getType() == KeyEntryType.SM2_KEYPAIR) {
            return KeyCodec.parseSm2PrivateKey(keyPairJson(entry.getValue()).get("privateKey").getAsString(), KeyFormat.BASE64);
        }
        return KeyCodec.parseSm2PrivateKey(entry.getValue(), KeyFormat.BASE64);
    }

    public static Sm2KeyPair sm2KeyPair(KeyEntry entry) throws CryptoException {
        JsonObject json = entry.getType() == KeyEntryType.SM2_KEYPAIR
                ? keyPairJson(entry.getValue())
                : null;
        org.bouncycastle.crypto.params.ECPublicKeyParameters pub = json != null
                ? KeyCodec.parseSm2PublicKey(json.get("publicKey").getAsString(), KeyFormat.BASE64)
                : KeyCodec.parseSm2PublicKey(entry.getValue(), KeyFormat.BASE64);
        org.bouncycastle.crypto.params.ECPrivateKeyParameters priv = json != null
                ? KeyCodec.parseSm2PrivateKey(json.get("privateKey").getAsString(), KeyFormat.BASE64)
                : KeyCodec.parseSm2PrivateKey(entry.getValue(), KeyFormat.BASE64);
        return new Sm2KeyPair(pub, priv);
    }

    /** 供密钥选择器展示/取值的便捷方法：按条目类型提取相应部分文本。 */
    public static String partValue(KeyEntry entry, KeyEntryType.Kind part) {
        if (entry.getType() == null) {
            return entry.getValue() == null ? "" : entry.getValue();
        }
        if (entry.getType().getKind() == KeyEntryType.Kind.KEYPAIR) {
            JsonObject json = keyPairJson(entry.getValue());
            if (part == KeyEntryType.Kind.PUBLIC) {
                return json.get("publicKey").getAsString();
            }
            if (part == KeyEntryType.Kind.PRIVATE) {
                return json.get("privateKey").getAsString();
            }
            return entry.getValue();
        }
        return entry.getValue();
    }

    // ---------------- 写入/规范化 ----------------

    /** 密钥对条目的 value JSON。 */
    public static String buildKeyPairValue(PublicKey publicKey, PrivateKey privateKey) {
        JsonObject json = new JsonObject();
        json.addProperty("publicKey", Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        json.addProperty("privateKey", Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        return com.kitbox.util.Gsons.compact().toJson(json);
    }

    /** 从密钥对 value JSON 中取某一部分（publicKey / privateKey）。 */
    public static String partValueJson(String keyPairValue, String partKey) {
        return keyPairJson(keyPairValue).get(partKey).getAsString();
    }

    public static String buildKeyPairValue(String publicBase64, String privateBase64) {
        JsonObject json = new JsonObject();
        json.addProperty("publicKey", publicBase64);
        json.addProperty("privateKey", privateBase64);
        return com.kitbox.util.Gsons.compact().toJson(json);
    }

    /**
     * 将用户粘贴的密钥文本规范化为存储格式；校验失败抛 CryptoException。
     *
     * @param type  条目类型
     * @param input 用户输入的密钥文本（可能是 Base64/Hex/PEM/明文）
     * @param fmt   输入格式
     */
    public static String normalize(KeyEntryType type, String input, KeyFormat fmt) throws CryptoException {
        if (input == null || input.trim().isEmpty()) {
            throw new CryptoException("密钥值为空");
        }
        String trimmed = input.trim();
        try {
            switch (type) {
                case SYM_AES:
                case SYM_DES:
                case SYM_DESEDE:
                case SYM_SM4: {
                    SymmetricAlgorithm algo = toSymAlgo(type);
                    byte[] key;
                    if (fmt == KeyFormat.HEX || fmt == KeyFormat.BASE64) {
                        key = fmt.decode(trimmed);
                    } else if (fmt == KeyFormat.PLAIN) {
                        key = DataEncoding.utf8(trimmed);
                    } else {
                        throw new CryptoException("对称密钥请使用 明文 / Base64 / Hex 格式");
                    }
                    if (!algo.isValidKeyLength(key.length)) {
                        throw new CryptoException(algo.getDisplay() + " 密钥长度必须为 " + algo.keyLengthHint()
                                + "（当前 " + key.length * 8 + " 位）");
                    }
                    return Base64.getEncoder().encodeToString(key);
                }
                case HMAC: {
                    byte[] key;
                    if (fmt == KeyFormat.HEX || fmt == KeyFormat.BASE64) {
                        key = fmt.decode(trimmed);
                    } else if (fmt == KeyFormat.PLAIN) {
                        key = DataEncoding.utf8(trimmed);
                    } else {
                        throw new CryptoException("HMAC 密钥请使用 明文 / Base64 / Hex 格式");
                    }
                    if (key.length == 0) {
                        throw new CryptoException("HMAC 密钥不能为空");
                    }
                    return Base64.getEncoder().encodeToString(key);
                }
                case RSA_PUBLIC:
                    return KeyCodec.toBase64(KeyCodec.parseRsaPublicKey(trimmed, fmt));
                case RSA_PRIVATE:
                    return KeyCodec.toBase64(KeyCodec.parseRsaPrivateKey(trimmed, fmt));
                case SM2_PUBLIC:
                    return KeyCodec.toBase64JcaPublic(KeyCodec.parseSm2PublicKey(trimmed, fmt));
                case SM2_PRIVATE:
                    return KeyCodec.toBase64JcaPrivate(KeyCodec.parseSm2PrivateKey(trimmed, fmt));
                case RSA_KEYPAIR:
                case SM2_KEYPAIR:
                    throw new CryptoException("密钥对请通过「生成密钥对」创建，不支持直接粘贴");
                default:
                    throw new CryptoException("不支持的类型");
            }
        } catch (IllegalArgumentException e) {
            throw new CryptoException("密钥格式解析失败：" + e.getMessage());
        }
    }

    /** 校验条目值与类型匹配（导入备份后抽查用）。 */
    public static SymmetricAlgorithm toSymAlgo(KeyEntryType type) throws CryptoException {
        switch (type) {
            case SYM_AES:
                return SymmetricAlgorithm.AES;
            case SYM_DES:
                return SymmetricAlgorithm.DES;
            case SYM_DESEDE:
                return SymmetricAlgorithm.DESEDE;
            case SYM_SM4:
                return SymmetricAlgorithm.SM4;
            default:
                throw new CryptoException("非对称密钥类型");
        }
    }

    private static JsonObject keyPairJson(String value) {
        JsonObject json = com.kitbox.util.Gsons.compact().fromJson(value, JsonObject.class);
        if (json == null || !json.has("publicKey") || !json.has("privateKey")) {
            throw new IllegalArgumentException("密钥对条目数据不完整");
        }
        return json;
    }
}
