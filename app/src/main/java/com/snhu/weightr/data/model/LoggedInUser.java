package com.snhu.weightr.data.model;

import com.snhu.weightr.data.db.security.CypherUtility;

import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private final Long userId;
    private final String userName;
    private final String password;

    public LoggedInUser(Long userId, String userName, String password) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
    }

    @NotNull
    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public SecretKey getEncryptionKey() throws IllegalStateException {
        return CypherUtility.deriveKey(password.toCharArray(), null);
    }
}