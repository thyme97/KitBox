package com.kitbox.keystore;

/**
 * 密钥库操作异常。
 */
public class KeyStoreException extends Exception {

    public KeyStoreException(String message) {
        super(message);
    }

    public KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
