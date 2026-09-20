package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorServiceTest {

    @Test
    void passwordsHaveExactLengthAndCount() {
        List<String> passwords = PasswordGeneratorService.generatePasswords(16, true, true, true, true, false, 5);
        assertEquals(5, passwords.size());
        for (String password : passwords) {
            assertEquals(16, password.length());
        }
    }

    @Test
    void everySelectedClassIsPresent() {
        List<String> passwords = PasswordGeneratorService.generatePasswords(32, true, true, true, true, false, 20);
        for (String password : passwords) {
            assertTrue(password.chars().anyMatch(c -> "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) >= 0), password);
            assertTrue(password.chars().anyMatch(c -> "abcdefghijklmnopqrstuvwxyz".indexOf(c) >= 0), password);
            assertTrue(password.chars().anyMatch(c -> "0123456789".indexOf(c) >= 0), password);
            assertTrue(password.chars().anyMatch(c -> "!@#$%^&*()-_=+[]{};:,.<>?/".indexOf(c) >= 0), password);
        }
    }

    @Test
    void charactersStayWithinSelectedPools() {
        List<String> passwords = PasswordGeneratorService.generatePasswords(64, true, false, true, false, false, 10);
        for (String password : passwords) {
            for (char c : password.toCharArray()) {
                boolean upperOrDigit =
                        "ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(c) >= 0 || "0123456789".indexOf(c) >= 0;
                assertTrue(upperOrDigit, "非法字符 " + c + " in " + password);
            }
        }
    }

    @Test
    void excludeAmbiguousRemovesConfusableChars() {
        List<String> passwords = PasswordGeneratorService.generatePasswords(40, true, true, true, false, true, 30);
        for (String password : passwords) {
            for (char c : password.toCharArray()) {
                assertTrue("0O1lI".indexOf(c) < 0, "包含易混淆字符 " + c + " in " + password);
            }
        }
    }

    @Test
    void generatedPasswordsDiffer() {
        List<String> passwords = PasswordGeneratorService.generatePasswords(24, true, true, true, true, false, 20);
        Set<String> distinct = new HashSet<>(passwords);
        assertEquals(20, distinct.size());
    }

    @Test
    void rejectsInvalidPolicies() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGeneratorService.generatePasswords(16, false, false, false, false, false, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGeneratorService.generatePasswords(2, true, true, true, true, false, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGeneratorService.generatePasswords(16, true, false, false, false, false, 0));
    }

    @Test
    void keysHaveExactEncodedLength() {
        List<String> hexKeys = PasswordGeneratorService.generateKeys(32, com.kitbox.crypto.model.DataEncoding.HEX, 3);
        for (String key : hexKeys) {
            assertEquals(64, key.length());
        }
        List<String> b64Keys = PasswordGeneratorService.generateKeys(32, com.kitbox.crypto.model.DataEncoding.BASE64, 3);
        for (String key : b64Keys) {
            assertEquals(44, key.length());
        }
    }

    @Test
    void keysAreDistinct() {
        List<String> keys = PasswordGeneratorService.generateKeys(32, com.kitbox.crypto.model.DataEncoding.HEX, 20);
        assertEquals(20, new HashSet<>(keys).size());
    }

    @Test
    void rejectsInvalidKeyParams() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGeneratorService.generateKeys(4, com.kitbox.crypto.model.DataEncoding.HEX, 1));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordGeneratorService.generateKeys(32, null, 1));
    }
}
