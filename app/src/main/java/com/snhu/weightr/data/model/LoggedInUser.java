package com.snhu.weightr.data.model;

import org.jetbrains.annotations.NotNull;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private final Long userId;
    private final String userName;

    public LoggedInUser(Long userId, String userName) {
        this.userId = userId;
        this.userName = userName;
    }

    @NotNull
    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}