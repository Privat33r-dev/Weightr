package com.snhu.weightr.ui.login;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Validation state of the login form.
 */
public final class LoginFormState {

    @Nullable @StringRes
    private final Integer usernameError;

    @Nullable @StringRes
    private final Integer passwordError;

    private final boolean isDataValid;

    /** Invalid state with specific input errors. */
    public LoginFormState(@Nullable Integer usernameError, @Nullable Integer passwordError) {
        this.usernameError = usernameError;
        this.passwordError = passwordError;
        this.isDataValid = (usernameError == null && passwordError == null);
    }

    /** Valid state. */
    public LoginFormState(boolean isDataValid) {
        this.usernameError = null;
        this.passwordError = null;
        this.isDataValid = isDataValid;
    }

    @Nullable
    public Integer getUsernameError() {
        return usernameError;
    }

    @Nullable
    public Integer getPasswordError() {
        return passwordError;
    }

    public boolean isDataValid() {
        return isDataValid;
    }
}
