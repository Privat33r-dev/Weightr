package com.snhu.weightr.ui.login;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.snhu.weightr.data.LoginDataSource;
import com.snhu.weightr.data.LoginRepository;

/** Provides LoginViewModel instances with proper dependencies. */
public final class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final Context appContext;

    public LoginViewModelFactory(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            LoginDataSource dataSource = new LoginDataSource(appContext);
            LoginRepository repo = LoginRepository.getInstance(dataSource);
            return (T) new LoginViewModel(repo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}