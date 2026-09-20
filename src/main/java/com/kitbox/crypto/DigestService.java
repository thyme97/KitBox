package com.kitbox.crypto;

import com.kitbox.crypto.model.DigestAlgorithm;
import com.kitbox.crypto.model.HmacAlgorithm;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 摘要与 HMAC 服务（MD5 / SHA 系列 / SM3 / Hmac 系列）。
 * <p>
 * SM3 与 HmacSM3 使用 BouncyCastle 轻量 API（不经过 JCE 框架，避免未签名
 * fat jar 下 "JCE cannot authenticate the provider BC" 的问题）。
 */
public final class DigestService {

    private DigestService() {
    }

    public static byte[] digest(byte[] data, DigestAlgorithm algo) throws CryptoException {
        if (algo == DigestAlgorithm.SM3) {
            SM3Digest digest = new SM3Digest();
            digest.update(data, 0, data.length);
            byte[] out = new byte[digest.getDigestSize()];
            digest.doFinal(out, 0);
            return out;
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algo.getJcaName());
            return md.digest(data);
        } catch (Exception e) {
            throw new CryptoException("摘要计算失败：" + e.getMessage(), e);
        }
    }

    public static byte[] hmac(byte[] data, byte[] key, HmacAlgorithm algo) throws CryptoException {
        if (key == null || key.length == 0) {
            throw new CryptoException("HMAC 密钥不能为空");
        }
        if (algo == HmacAlgorithm.HMAC_SM3) {
            HMac mac = new HMac(new SM3Digest());
            mac.init(new KeyParameter(key));
            mac.update(data, 0, data.length);
            byte[] out = new byte[mac.getMacSize()];
            mac.doFinal(out, 0);
            return out;
        }
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algo.getJcaName());
            mac.init(new javax.crypto.spec.SecretKeySpec(key, algo.getJcaName()));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new CryptoException("HMAC 计算失败：" + e.getMessage(), e);
        }
    }

    /** 常量时间比较，避免时序侧信道。 */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(Arrays.copyOf(a, a.length), Arrays.copyOf(b, b.length));
    }
}
