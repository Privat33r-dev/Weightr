package com.snhu.weightr.data;

import android.content.Context;
import com.snhu.weightr.data.db.WeightrDb;
import com.snhu.weightr.data.db.dao.UserDao;
import com.snhu.weightr.data.db.entity.UserEntity;
import com.snhu.weightr.data.model.LoggedInUser;
import com.snhu.weightr.data.security.PasswordHasher;
import java.io.IOException;

/** Authenticates against Room-backed users table. */
public final class LoginDataSource {
    private final UserDao userDao;

    public LoginDataSource(Context context) {
        this.userDao = WeightrDb.get(context).userDao();
    }

    public Result<LoggedInUser> login(String username, String password) {
        try {
            final UserEntity user = userDao.findByUsername(username);
            if (user == null) {
                return new Result.Error(new IOException("User not found"));
            }
            final boolean ok = PasswordHasher.verify(password.toCharArray(), user.passwordHash);
            if (!ok) {
                return new Result.Error(new IOException("Invalid credentials"));
            }
            final LoggedInUser logged = new LoggedInUser(String.valueOf(user.id), user.username);
            return new Result.Success<>(logged);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() { /* stateless */ }
}