package com.snhu.weightr.data.model;

import com.snhu.weightr.data.db.security.CypherUtility;

import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private final String password;

    public LoggedInUser(String password) {
        this.password = password;
    }

    public SecretKey getEncryptionKey() throws IllegalStateException {
        return CypherUtility.deriveKey(password.toCharArray(), null);
    }
}