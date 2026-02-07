package com.snhu.weightr.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.snhu.weightr.MainActivity;
import com.snhu.weightr.R;
import com.snhu.weightr.data.db.security.PasswordHasher;
import com.snhu.weightr.data.model.LoggedInUser;
import com.snhu.weightr.data.session.SessionStore;
import com.snhu.weightr.databinding.ActivityLoginBinding;
import com.snhu.weightr.util.Utils;

public final class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel loginViewModel;

    private enum Mode {LOGIN, REGISTER}

    private Mode mode = Mode.LOGIN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && checkSessionExists()) {
            sendToMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        hookObservers();
        hookUiEvents();
        applyMode(Mode.LOGIN);
    }

    private void hookObservers() {
        final TextInputLayout usernameLayout = binding.usernameLayout;
        final TextInputLayout passwordLayout = binding.passwordLayout;
        final TextInputLayout passwordRepeatLayout = binding.passwordRepeatLayout;

        loginViewModel.getLoginFormState().observe(this, state -> {
            if (state == null) return;
            binding.loginOrRegister.setEnabled(state.isDataValid());
            usernameLayout.setError(state.getUsernameError() != null
                    ? getString(state.getUsernameError()) : null);
            passwordLayout.setError(state.getPasswordError() != null
                    ? getString(state.getPasswordError()) : null);

            if (mode == Mode.REGISTER) {
                boolean matches = TextUtils.equals(
                        Utils.safeText(binding.password.getText()),
                        Utils.safeText(binding.passwordRepeat.getText()));
                passwordRepeatLayout.setError(matches ? null : getString(R.string.error_password_mismatch));
            } else {
                passwordRepeatLayout.setError(null);
            }
        });

        loginViewModel.getLoginResult().observe(this, result -> {
            if (result == null) return;
            setLoading(false);
            if (result.getError() != null || result.getErrorMessage() != null) {
                showError(result.getError(), result.getErrorMessage());
                return;
            }
            LoggedInUser user = result.getSuccess();
            if (user != null) {
                onAuthSuccess(user);
            } else {
                showError(R.string.login_failed, "");
            }
        });
    }

    private void hookUiEvents() {
        binding.username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.usernameChanged(Utils.safeText(s));
            }
        });

        binding.password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.passwordChanged(Utils.safeText(s));
            }
        });

        binding.passwordRepeat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mode == Mode.REGISTER) {
                    binding.passwordRepeatLayout.setError(null);
                }
            }
        });

        binding.password.setOnEditorActionListener((v, actionId, event) -> {
            if (mode == Mode.LOGIN &&
                    (actionId == EditorInfo.IME_ACTION_DONE ||
                            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                    event.getAction() == KeyEvent.ACTION_UP))) {
                submit();
                return true;
            }
            return false;
        });

        binding.passwordRepeat.setOnEditorActionListener((v, actionId, event) -> {
            if (mode == Mode.REGISTER &&
                    (actionId == EditorInfo.IME_ACTION_DONE ||
                            (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                                    event.getAction() == KeyEvent.ACTION_UP))) {
                submit();
                return true;
            }
            return false;
        });

        binding.loginOrRegister.setOnClickListener(v -> submit());
        binding.switchMode.setOnClickListener(v ->
                applyMode(mode == Mode.LOGIN ? Mode.REGISTER : Mode.LOGIN));
    }

    private void applyMode(Mode newMode) {
        this.mode = newMode;
        final boolean isRegister = (newMode == Mode.REGISTER);

        binding.passwordRepeatLayout.setVisibility(isRegister ? View.VISIBLE : View.GONE);

        binding.loginOrRegister.setText(isRegister
                ? R.string.action_register_short
                : R.string.action_sign_in_short);

        binding.switchMode.setText(isRegister
                ? R.string.switch_to_login
                : R.string.switch_to_register);

        binding.username.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        binding.password.setImeOptions(isRegister ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
    }

    private void submit() {
        String email = Utils.safeText(binding.username.getText()).trim();
        String password = Utils.safeText(binding.password.getText());

        if (mode == Mode.REGISTER) {
            String pw2 = Utils.safeText(binding.passwordRepeat.getText());
            if (!TextUtils.equals(password, pw2)) {
                binding.passwordRepeatLayout.setError(getString(R.string.error_password_mismatch));
                return;
            }
        }

        setLoading(true);

        // Send hashed password instead of raw password to prevent data decryption
        // in case of MITM attacks
        String hashed_password = PasswordHasher.hash(password.toCharArray());
        if (mode == Mode.REGISTER) {
            loginViewModel.register(email, hashed_password);
        } else {
            loginViewModel.login(email, hashed_password);
        }
    }

    private void onAuthSuccess(LoggedInUser user) {
        Toast.makeText(this, R.string.welcome, Toast.LENGTH_SHORT).show();
        SessionStore.get(this).save(user.getUserId(), user.getUserName(), user.getEncryptionKey());
        sendToMainActivity();
    }

    private void setLoading(boolean loading) {
        CircularProgressIndicator p = binding.loading;
        p.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginOrRegister.setEnabled(!loading);
        binding.usernameLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.passwordRepeatLayout.setEnabled(!loading);
    }

    private void showError(Integer errorRes, String errorMessage) {
        String msg = (errorMessage != null)
                ? errorMessage
                : (errorRes != null ? getString(errorRes) : getString(R.string.login_failed));
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean checkSessionExists() {
        return SessionStore.get(this).userId() >= 0;
    }

    private void sendToMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}