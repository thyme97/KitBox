package com.kitbox.crypto;

import com.kitbox.crypto.model.RsaPadding;
import com.kitbox.crypto.model.Sm2CipherMode;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * 非对称加解密服务。
 * - RSA：JDK Cipher，PKCS1 / OAEP，超长明文自动分段加解密
 * - SM2：BouncyCastle SM2Engine，支持 C1C3C2 / C1C2C3
 */
public final class AsymmetricService {

    private AsymmetricService() {
    }

    // ---------------- RSA ----------------

    public static byte[] rsaEncrypt(byte[] data, PublicKey publicKey, RsaPadding padding) throws CryptoException {
        return rsaCipher(javax.crypto.Cipher.ENCRYPT_MODE, data, publicKey, null, padding);
    }

    public static byte[] rsaDecrypt(byte[] data, PrivateKey privateKey, RsaPadding padding) throws CryptoException {
        return rsaCipher(javax.crypto.Cipher.DECRYPT_MODE, data, null, privateKey, padding);
    }

    private static byte[] rsaCipher(int opMode, byte[] data, PublicKey publicKey, PrivateKey privateKey,
                                    RsaPadding padding) throws CryptoException {
        if (data == null || data.length == 0) {
            throw new CryptoException("数据为空");
        }
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(padding.getTransformation());
            int keyBytes;
            if (opMode == javax.crypto.Cipher.ENCRYPT_MODE) {
                if (!(publicKey instanceof RSAPublicKey)) {
                    throw new CryptoException("请提供 RSA 公钥");
                }
                java.security.spec.AlgorithmParameterSpec spec = oaepSpec(padding);
                if (spec == null) {
                    cipher.init(opMode, publicKey);
                } else {
                    cipher.init(opMode, publicKey, spec);
                }
                keyBytes = ((RSAPublicKey) publicKey).getModulus().bitLength() / 8;
            } else {
                if (!(privateKey instanceof RSAPrivateKey)) {
                    throw new CryptoException("请提供 RSA 私钥");
                }
                java.security.spec.AlgorithmParameterSpec spec = oaepSpec(padding);
                if (spec == null) {
                    cipher.init(opMode, privateKey);
                } else {
                    cipher.init(opMode, privateKey, spec);
                }
                keyBytes = ((RSAPrivateKey) privateKey).getModulus().bitLength() / 8;
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            if (opMode == javax.crypto.Cipher.ENCRYPT_MODE) {
                int maxBlock = padding.maxBlock(keyBytes);
                for (int offset = 0; offset < data.length; offset += maxBlock) {
                    int len = Math.min(maxBlock, data.length - offset);
                    byte[] block = cipher.doFinal(data, offset, len);
                    out.write(block, 0, block.length);
                }
            } else {
                if (data.length % keyBytes != 0) {
                    throw new CryptoException("密文长度不是密钥长度的整数倍，可能不是分段 RSA 密文");
                }
                for (int offset = 0; offset < data.length; offset += keyBytes) {
                    byte[] block = cipher.doFinal(data, offset, keyBytes);
                    out.write(block, 0, block.length);
                }
            }
            return out.toByteArray();
        } catch (CryptoException e) {
            throw e;
        } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException e) {
            throw new CryptoException("RSA 加解密失败：请检查密钥与填充方式是否匹配（" + e.getClass().getSimpleName() + "）", e);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA 加解密失败：" + e.getMessage(), e);
        }
    }

    private static java.security.spec.AlgorithmParameterSpec oaepSpec(RsaPadding padding) {
        if (padding == RsaPadding.OAEP_SHA1) {
            return new javax.crypto.spec.OAEPParameterSpec("SHA-1", "MGF1",
                    java.security.spec.MGF1ParameterSpec.SHA1, javax.crypto.spec.PSource.PSpecified.DEFAULT);
        }
        if (padding == RsaPadding.OAEP_SHA256) {
            return new javax.crypto.spec.OAEPParameterSpec("SHA-256", "MGF1",
                    java.security.spec.MGF1ParameterSpec.SHA256, javax.crypto.spec.PSource.PSpecified.DEFAULT);
        }
        // PKCS1 无需参数
        return null;
    }

    // ---------------- SM2 ----------------

    public static byte[] sm2Encrypt(byte[] data, ECPublicKeyParameters publicKey, Sm2CipherMode mode)
            throws CryptoException {
        if (data == null || data.length == 0) {
            throw new CryptoException("数据为空");
        }
        try {
            SM2Engine engine = new SM2Engine(toEngineMode(mode));
            engine.init(true, new ParametersWithRandom(publicKey, new SecureRandom()));
            return engine.processBlock(data, 0, data.length);
        } catch (Exception e) {
            throw new CryptoException("SM2 加密失败：" + e.getMessage(), e);
        }
    }

    public static byte[] sm2Decrypt(byte[] data, ECPrivateKeyParameters privateKey, Sm2CipherMode mode)
            throws CryptoException {
        if (data == null || data.length == 0) {
            throw new CryptoException("数据为空");
        }
        try {
            SM2Engine engine = new SM2Engine(toEngineMode(mode));
            engine.init(false, privateKey);
            return engine.processBlock(data, 0, data.length);
        } catch (org.bouncycastle.crypto.InvalidCipherTextException e) {
            throw new CryptoException("SM2 解密失败：密文非法或密钥/密文排列方式（C1C3C2/C1C2C3）不匹配", e);
        } catch (Exception e) {
            throw new CryptoException("SM2 解密失败：" + e.getMessage(), e);
        }
    }

    private static SM2Engine.Mode toEngineMode(Sm2CipherMode mode) {
        return mode == Sm2CipherMode.C1C3C2 ? SM2Engine.Mode.C1C3C2 : SM2Engine.Mode.C1C2C3;
    }
}
