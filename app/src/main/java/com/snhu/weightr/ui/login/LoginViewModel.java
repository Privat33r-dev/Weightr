package com.snhu.weightr.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snhu.weightr.R;
import com.snhu.weightr.data.model.LoggedInUser;
import com.snhu.weightr.data.repo.UserRepository;

public final class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    private final UserRepository userRepository;

    public LoginViewModel(@NonNull UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    /**
     * Attempt to log in using local Room-backed user table.
     */
    public void login(String username, String password) {
        userRepository.authenticate(username, password, (user) -> {
            if (user != null) {
                // Successful login
                loginResult.postValue(new LoginResult(new LoggedInUser(user.id, user.username, password)));
            } else {
                loginResult.postValue(new LoginResult(R.string.login_failed));
            }
        });
    }

    /**
     * Register a new user and auto-login.
     */
    public void register(String username, String password) {
        userRepository.createUser(username, password, (userId) -> {
            try {
                loginResult.postValue(new LoginResult(new LoggedInUser(userId, username, password)));
            } catch (Exception e) {
                loginResult.postValue(new LoginResult(R.string.register_failed));
            }
        });
    }

    /**
     * Validate login form inputs.
     */
    public void loginDataChanged(String username, String password) {
        Integer usernameError = (username == null || username.trim().isEmpty())
                ? R.string.error_invalid_username : null;
        Integer passwordError = (password == null || password.length() < 8)
                ? R.string.error_invalid_password : null;
        loginFormState.setValue(new LoginFormState(usernameError, passwordError));
    }
}