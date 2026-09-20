package com.kitbox.crypto;

import com.kitbox.crypto.model.CryptoMode;
import com.kitbox.crypto.model.Padding;
import com.kitbox.crypto.model.SymmetricAlgorithm;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 对称加解密服务（AES / DES / 3DES / SM4）。
 * <p>
 * AES/DES/3DES 使用 JDK 内置 JCE（SunJCE）；SM4 使用 BouncyCastle 轻量 API。
 * 注意：SM 系列刻意不走 JCA/JCE Cipher 通道——JCE 框架会校验 Provider 签名，
 * 打包为未签名 fat jar 时会报 "JCE cannot authenticate the provider BC"；
 * 轻量 API 不经过 JCE 框架，无此限制。
 */
public final class SymmetricService {

    public static final int DEFAULT_GCM_TAG_BITS = 128;

    private SymmetricService() {
    }

    public static byte[] encrypt(byte[] data, byte[] key, SymmetricAlgorithm algo, CryptoMode mode,
                                 Padding padding, byte[] iv, int gcmTagBits) throws CryptoException {
        return doCipher(true, data, key, algo, mode, padding, iv, gcmTagBits);
    }

    public static byte[] decrypt(byte[] data, byte[] key, SymmetricAlgorithm algo, CryptoMode mode,
                                 Padding padding, byte[] iv, int gcmTagBits) throws CryptoException {
        return doCipher(false, data, key, algo, mode, padding, iv, gcmTagBits);
    }

    /** 生成指定算法的随机密钥。 */
    public static byte[] randomKey(SymmetricAlgorithm algo, int keyLenBytes) {
        byte[] key = new byte[keyLenBytes];
        new SecureRandom().nextBytes(key);
        return key;
    }

    /** 生成随机 IV（GCM 用 12 字节，其余用分组长度）。 */
    public static byte[] randomIv(SymmetricAlgorithm algo, CryptoMode mode) {
        int len = mode == CryptoMode.GCM ? 12 : algo.getBlockSize();
        byte[] iv = new byte[len];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /** 生成随机字母数字字符串（按 UTF-8 解码正好 len 个字节），用于「明文」格式的随机密钥。 */
    public static String randomAlphanumeric(int len) {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static byte[] doCipher(boolean encrypt, byte[] data, byte[] key, SymmetricAlgorithm algo,
                                   CryptoMode mode, Padding padding, byte[] iv, int gcmTagBits)
            throws CryptoException {
        if (data == null || data.length == 0) {
            throw new CryptoException("数据为空");
        }
        validateKey(key, algo);
        if (algo == SymmetricAlgorithm.SM4) {
            return sm4Cipher(encrypt, data, key, mode, padding, iv, gcmTagBits);
        }
        return jcaCipher(encrypt, data, key, algo, mode, padding, iv, gcmTagBits);
    }

    // ---------------- SM4（BouncyCastle 轻量 API，无 JCE Provider 依赖） ----------------

    private static byte[] sm4Cipher(boolean encrypt, byte[] data, byte[] key, CryptoMode mode,
                                    Padding padding, byte[] iv, int gcmTagBits) throws CryptoException {
        try {
            if (mode != CryptoMode.ECB && (iv == null || iv.length == 0)) {
                throw new CryptoException(mode + " 模式必须提供 IV（GCM 推荐 12 字节）");
            }
            if (mode == CryptoMode.GCM) {
                GCMBlockCipher cipher = new GCMBlockCipher(new SM4Engine());
                int tagBits = gcmTagBits > 0 ? gcmTagBits : DEFAULT_GCM_TAG_BITS;
                cipher.init(encrypt, new AEADParameters(new KeyParameter(key), tagBits, iv));
                byte[] out = new byte[cipher.getOutputSize(data.length)];
                int produced = cipher.processBytes(data, 0, data.length, out, 0);
                produced += cipher.doFinal(out, produced);
                return Arrays.copyOf(out, produced);
            }

            SM4Engine engine = new SM4Engine();
            BufferedBlockCipher cipher;
            switch (mode) {
                case ECB:
                    cipher = padding == Padding.PKCS5
                            ? new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher(engine)
                            : new BufferedBlockCipher(engine);
                    cipher.init(encrypt, new KeyParameter(key));
                    break;
                case CBC:
                    cipher = padding == Padding.PKCS5
                            ? new org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher(new CBCBlockCipher(engine))
                            : new BufferedBlockCipher(new CBCBlockCipher(engine));
                    cipher.init(encrypt, new ParametersWithIV(new KeyParameter(key), iv));
                    break;
                case CTR:
                    cipher = new BufferedBlockCipher(new SICBlockCipher(engine));
                    cipher.init(encrypt, new ParametersWithIV(new KeyParameter(key), iv));
                    break;
                default:
                    throw new CryptoException("不支持的模式：" + mode);
            }
            byte[] out = new byte[cipher.getOutputSize(data.length)];
            int produced = cipher.processBytes(data, 0, data.length, out, 0);
            produced += cipher.doFinal(out, produced);
            return Arrays.copyOf(out, produced);
        } catch (CryptoException e) {
            throw e;
        } catch (InvalidCipherTextException e) {
            throw new CryptoException("解密失败：请检查密钥、模式、填充、IV 或密文格式是否正确（填充校验未通过）", e);
        } catch (org.bouncycastle.crypto.DataLengthException e) {
            throw new CryptoException("加解密失败：NoPadding 模式下数据长度必须为 16 字节的整数倍", e);
        } catch (IllegalArgumentException e) {
            throw new CryptoException("加解密失败：参数不正确（" + e.getMessage() + "）", e);
        } catch (Exception e) {
            throw new CryptoException("加解密失败：" + e.getMessage(), e);
        }
    }

    // ---------------- AES / DES / 3DES（JDK JCE） ----------------

    private static byte[] jcaCipher(boolean encrypt, byte[] data, byte[] key, SymmetricAlgorithm algo,
                                    CryptoMode mode, Padding padding, byte[] iv, int gcmTagBits)
            throws CryptoException {
        int opMode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        try {
            String paddingName = padding == Padding.NO_PADDING ? "NoPadding" : "PKCS5Padding";
            String transformation;
            java.security.spec.AlgorithmParameterSpec spec = null;
            switch (mode) {
                case ECB:
                    transformation = algo.getJcaName() + "/ECB/" + paddingName;
                    break;
                case CBC:
                    validateIv(iv, algo);
                    transformation = algo.getJcaName() + "/CBC/" + paddingName;
                    spec = new IvParameterSpec(iv);
                    break;
                case CTR:
                    validateIv(iv, algo);
                    if (padding != Padding.NO_PADDING) {
                        throw new CryptoException("CTR 模式仅支持 NoPadding 填充");
                    }
                    transformation = algo.getJcaName() + "/CTR/NoPadding";
                    spec = new IvParameterSpec(iv);
                    break;
                case GCM:
                    if (!algo.supportGcm()) {
                        throw new CryptoException(algo.getDisplay() + " 不支持 GCM 模式");
                    }
                    if (padding != Padding.NO_PADDING) {
                        throw new CryptoException("GCM 模式仅支持 NoPadding 填充");
                    }
                    if (iv == null || iv.length == 0) {
                        throw new CryptoException("GCM 模式必须提供 IV（推荐 12 字节）");
                    }
                    int tagBits = gcmTagBits > 0 ? gcmTagBits : DEFAULT_GCM_TAG_BITS;
                    transformation = algo.getJcaName() + "/GCM/NoPadding";
                    spec = new GCMParameterSpec(tagBits, iv);
                    break;
                default:
                    throw new CryptoException("不支持的模式：" + mode);
            }

            Cipher cipher = Cipher.getInstance(transformation);
            if (spec == null) {
                cipher.init(opMode, new SecretKeySpec(key, algo.getJcaName()));
            } else {
                cipher.init(opMode, new SecretKeySpec(key, algo.getJcaName()), spec);
            }
            return cipher.doFinal(data);
        } catch (CryptoException e) {
            throw e;
        } catch (javax.crypto.AEADBadTagException e) {
            throw new CryptoException("解密失败：GCM 完整性校验未通过，请检查密钥、IV 或密文是否正确", e);
        } catch (javax.crypto.IllegalBlockSizeException | javax.crypto.BadPaddingException e) {
            throw new CryptoException("加解密失败：请检查密钥、模式、填充、IV 或密文格式是否正确（"
                    + e.getClass().getSimpleName() + "）", e);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("加解密失败：" + e.getMessage(), e);
        }
    }

    private static void validateKey(byte[] key, SymmetricAlgorithm algo) throws CryptoException {
        if (key == null || key.length == 0) {
            throw new CryptoException("密钥为空");
        }
        if (!algo.isValidKeyLength(key.length)) {
            throw new CryptoException(algo.getDisplay() + " 密钥长度必须为 " + algo.keyLengthHint()
                    + "（当前 " + key.length * 8 + " 位）");
        }
    }

    private static void validateIv(byte[] iv, SymmetricAlgorithm algo) throws CryptoException {
        if (iv == null || iv.length == 0) {
            throw new CryptoException("该模式必须提供 IV");
        }
        if (iv.length != algo.getBlockSize()) {
            throw new CryptoException(algo.getDisplay() + " 的 IV 长度必须为 " + algo.getBlockSize()
                    + " 字节（当前 " + iv.length + " 字节）");
        }
    }
}
