package com.snhu.weightr.ui.login;

import androidx.annotation.Nullable;

import com.snhu.weightr.data.model.LoggedInUser;

/**
 * Authentication result : success (user details) or error message.
 */
class LoginResult {
    @Nullable
    private LoggedInUser success;
    @Nullable
    private Integer error;

    private final String errorMessage;

    LoginResult(@Nullable Integer error) {
        this.error = error;
        this.errorMessage = null;
    }

    LoginResult(@Nullable LoggedInUser success) {
        this.success = success;
        this.errorMessage = null;
    }

    public LoginResult(String errorMessage) {
        this.success = null;
        this.errorMessage = errorMessage;
    }

    @Nullable
    LoggedInUser getSuccess() {
        return success;
    }

    @Nullable
    Integer getError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}