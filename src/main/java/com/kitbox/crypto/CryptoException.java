package com.kitbox.crypto;

/**
 * 加解密操作异常，消息面向用户展示。
 */
public class CryptoException extends Exception {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
