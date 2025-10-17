// app/src/main/java/com/x/weightr/ui/login/LoginViewModelFactory.java  (replace)
package com.snhu.weightr.ui.login;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.repo.UserRepository;

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
            UserRepository userRepo =
                    new UserRepository(WeightrDb.get(appContext).userDao());
            return (T) new LoginViewModel(userRepo);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
