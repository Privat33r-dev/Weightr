package com.snhu.weightr.data.repo;

import androidx.annotation.NonNull;
import com.snhu.weightr.data.db.dao.UserDao;
import com.snhu.weightr.data.db.entity.UserEntity;
import com.snhu.weightr.data.repo.util.DbExecutor;
import com.snhu.weightr.data.db.security.PasswordHasher;

/**
 * Handles user registration and authentication.
 * Thin layer over UserDao; keeps hashing logic separate from the ViewModel.
 */
public final class UserRepository {

    private final UserDao userDao;

    public UserRepository(@NonNull UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Create a new user.
     *
     * @throws IllegalStateException if username already exists
     */
    public void createUser(@NonNull String username, @NonNull String password, @NonNull Callback<Long> onComplete) {
        DbExecutor.get().execute(() -> {
            UserEntity existing = userDao.findByUsername(username);
            if (existing != null) {
                throw new IllegalStateException("Username already exists");
            }

            String hashed = PasswordHasher.hash(password.toCharArray());
            UserEntity user = new UserEntity();
            user.username = username;
            user.passwordHash = hashed;
            long userId = userDao.insert(user);
            onComplete.onResult(userId); // Callback for completion (e.g., UI update)
        });
    }

    /**
     * Validate login credentials.
     */
    public void authenticate(@NonNull String username, @NonNull String password, @NonNull Callback<UserEntity> callback) {
        DbExecutor.get().execute(() -> {
            UserEntity user = userDao.findByUsername(username);
            if (user == null) {
                callback.onResult(null);
                return;
            }
            boolean ok = PasswordHasher.verify(password.toCharArray(), user.passwordHash);
            callback.onResult(ok ? user : null);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
