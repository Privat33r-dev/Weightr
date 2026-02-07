package com.snhu.weightr.ui.login;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snhu.weightr.R;
import com.snhu.weightr.data.model.LoggedInUser;

public final class LoginViewModel extends ViewModel {

    private final MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private final MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();

    private final FirebaseAuth mAuth;
    private final int MIN_PASS_LENGTH = 8;
    private Integer currentUsernameError = null;
    private Integer currentPasswordError = null;

    public LoginViewModel() {
        mAuth = FirebaseAuth.getInstance();
    }

    public LiveData<LoginFormState> getLoginFormState() {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult() {
        return loginResult;
    }

    public void login(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            onAuthSuccess(firebaseUser, password);
                        } else {
                            loginResult.postValue(new LoginResult(R.string.login_failed));
                        }
                    } else {
                        Exception e = task.getException();
                        if (e == null) {
                            loginResult.postValue(new LoginResult(R.string.login_failed));
                        } else {
                            loginResult.postValue(new LoginResult(e.getMessage()));
                        }
                    }
                });
    }

    public void register(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            onAuthSuccess(firebaseUser, password);
                        } else {
                            loginResult.postValue(new LoginResult(R.string.login_failed));
                        }
                    } else {
                        loginResult.postValue(new LoginResult(R.string.login_failed));
                    }
                });
    }

    private void onAuthSuccess(FirebaseUser firebaseUser, String password) {
        String uid = firebaseUser.getUid();
        long legacyUserId = (long) uid.hashCode();
        String displayName = firebaseUser.getEmail();

        LoggedInUser user = new LoggedInUser(legacyUserId, displayName, password);
        loginResult.postValue(new LoginResult(user));
    }

    private void setFormState() {
        loginFormState.setValue(new LoginFormState(currentUsernameError, currentPasswordError));
    }

    public void usernameChanged(String username) {
        if (username == null || username.trim().isEmpty()) {
            currentUsernameError = R.string.error_invalid_username;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            currentUsernameError = R.string.error_invalid_email;
        } else {
            currentUsernameError = null;
        }
        setFormState();
    }

    public void passwordChanged(String password) {
        if (password != null && !password.isEmpty() && password.length() < MIN_PASS_LENGTH) {
            currentPasswordError = R.string.error_invalid_password;
        } else {
            currentPasswordError = null;
        }
        setFormState();
    }
}