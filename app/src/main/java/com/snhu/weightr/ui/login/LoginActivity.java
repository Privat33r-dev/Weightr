package com.snhu.weightr.ui.login;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;
import com.snhu.weightr.MainActivity;
import com.snhu.weightr.R;
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

        if (checkSessionExists()) {
            sendToMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        @NonNull LoginViewModelFactory factory =
                new LoginViewModelFactory(getApplicationContext());
        loginViewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

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
            if (result.getError() != null) {
                showError(result.getError());
                return;
            }
            if (result.getSuccess() != null) {
                try {
                    onAuthSuccess();
                } catch (IllegalStateException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void hookUiEvents() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(
                        Utils.safeText(binding.username.getText()),
                        Utils.safeText(binding.password.getText()));
            }
        };
        binding.username.addTextChangedListener(watcher);
        binding.password.addTextChangedListener(watcher);
        binding.passwordRepeat.addTextChangedListener(watcher);

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
        if (mode == Mode.REGISTER) {
            final String pw = Utils.safeText(binding.password.getText());
            final String pw2 = Utils.safeText(binding.passwordRepeat.getText());
            if (!TextUtils.equals(pw, pw2)) {
                binding.passwordRepeatLayout.setError(getString(R.string.error_password_mismatch));
                return;
            }
            setLoading(true);
            loginViewModel.register(
                    Utils.safeText(binding.username.getText()),
                    pw);
            return;
        }

        setLoading(true);
        loginViewModel.login(
                Utils.safeText(binding.username.getText()),
                Utils.safeText(binding.password.getText()));
    }

    private void setLoading(boolean loading) {
        CircularProgressIndicator p = binding.loading;
        p.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginOrRegister.setEnabled(!loading);
        binding.usernameLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.passwordRepeatLayout.setEnabled(!loading);
    }


    private void onAuthSuccess() throws IllegalStateException {
        setResult(Activity.RESULT_OK);
        Toast.makeText(this, R.string.welcome, Toast.LENGTH_SHORT).show();

        LoginResult tmp = loginViewModel.getLoginResult().getValue();
        if (tmp == null) throw new IllegalStateException("Authorized, but unable to fetch user");
        LoggedInUser user = tmp.getSuccess();
        if (user == null) throw new IllegalStateException("Authorized, but unable to fetch user");

        SessionStore.get(this).save(user.getUserId(), user.getUserName());
        sendToMainActivity();
    }

    private void showError(@StringRes int msg) {
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