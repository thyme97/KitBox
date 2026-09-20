package com.kitbox.crypto;

import com.kitbox.crypto.model.HmacAlgorithm;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithID;
import org.bouncycastle.crypto.signers.SM2Signer;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

/**
 * 加签 / 验签服务。
 * - RSA：MD5withRSA、SHA1withRSA、SHA256withRSA、SHA512withRSA
 * - SM2：SM3withSM2（基于 BouncyCastle SM2Signer，支持自定义签名者 ID）
 * - HMAC：见 {@link DigestService#hmac}
 */
public final class SignatureService {

    public static final String DEFAULT_SM2_ID = "1234567812345678";

    private SignatureService() {
    }

    // ---------------- RSA ----------------

    public static byte[] rsaSign(byte[] data, PrivateKey privateKey, String algorithm) throws CryptoException {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA 加签失败：" + e.getMessage(), e);
        }
    }

    public static boolean rsaVerify(byte[] data, byte[] sig, PublicKey publicKey, String algorithm)
            throws CryptoException {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(sig);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("RSA 验签失败：" + e.getMessage(), e);
        }
    }

    // ---------------- SM2 ----------------

    public static byte[] sm2Sign(byte[] data, ECPrivateKeyParameters privateKey, String signerId)
            throws CryptoException {
        try {
            SM2Signer signer = new SM2Signer();
            String id = signerId == null || signerId.isEmpty() ? DEFAULT_SM2_ID : signerId;
            signer.init(true, new ParametersWithID(
                    new org.bouncycastle.crypto.params.ParametersWithRandom(privateKey, new SecureRandom()),
                    id.getBytes(StandardCharsets.UTF_8)));
            signer.update(data, 0, data.length);
            return signer.generateSignature();
        } catch (org.bouncycastle.crypto.CryptoException e) {
            throw new CryptoException("SM2 加签失败：" + e.getMessage(), e);
        }
    }

    public static boolean sm2Verify(byte[] data, byte[] sig, ECPublicKeyParameters publicKey, String signerId) {
        try {
            SM2Signer signer = new SM2Signer();
            String id = signerId == null || signerId.isEmpty() ? DEFAULT_SM2_ID : signerId;
            signer.init(false, new ParametersWithID(publicKey, id.getBytes(StandardCharsets.UTF_8)));
            signer.update(data, 0, data.length);
            return signer.verifySignature(sig);
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- HMAC ----------------

    public static byte[] hmacSign(byte[] data, byte[] key, HmacAlgorithm algo) throws CryptoException {
        return DigestService.hmac(data, key, algo);
    }

    public static boolean hmacVerify(byte[] data, byte[] sig, byte[] key, HmacAlgorithm algo) throws CryptoException {
        return DigestService.constantTimeEquals(DigestService.hmac(data, key, algo), sig);
    }
}
