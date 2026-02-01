package com.snhu.weightr.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Stores user session (user id and username) and derived encryption key via SharedPreferences.
 * NOTE: Encryption key is stored in plain text for now (quick implementation).
 * Security will be improved later with EncryptedSharedPreferences/Keystore.
 */
public final class SessionStore {
    private static final String PREF = "session";
    private static final String KEY_UID = "uid";
    private static final String KEY_UNAME = "uname";
    private static final String KEY_CRYPTO = "crypto_key"; // Base64-encoded AES secret key bytes
    private static final String SECRET_KEY_ALGO = "AES";

    private static volatile SessionStore INSTANCE;
    private final SharedPreferences prefs;

    private SessionStore(Context appCtx) {
        this.prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static SessionStore get(Context ctx) {
        if (INSTANCE != null) return INSTANCE;
        synchronized (SessionStore.class) {
            if (INSTANCE == null) INSTANCE = new SessionStore(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    public void save(long userId, String username, @NonNull SecretKey encryptionKey) {
        String encodedKey = Base64.encodeToString(encryptionKey.getEncoded(), Base64.NO_WRAP);
        prefs.edit()
                .putLong(KEY_UID, userId)
                .putString(KEY_UNAME, username)
                .putString(KEY_CRYPTO, encodedKey)
                .apply();
    }

    public void reset() {
        prefs.edit()
                .putLong(KEY_UID, -1L)
                .remove(KEY_UNAME)
                .remove(KEY_CRYPTO)
                .apply();
    }

    public long userId() {
        return prefs.getLong(KEY_UID, -1L);
    }

    public String username() {
        return prefs.getString(KEY_UNAME, null);
    }

    @Nullable
    public SecretKey encryptionKey() {
        String encoded = prefs.getString(KEY_CRYPTO, null);
        if (encoded == null) return null;
        byte[] keyBytes = Base64.decode(encoded, Base64.NO_WRAP);
        return new SecretKeySpec(keyBytes, SECRET_KEY_ALGO);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}