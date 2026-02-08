package com.snhu.weightr.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Securely persists the derived encryption key using Android Keystore-backed encryption.
 * - Keystore generates/protects a master AES key (hardware-backed where possible).
 * - Derived key is encrypted with it and stored in plain SharedPreferences.
 * <p>
 * This approach allows us to avoid storing key in plaintext in files,
 * protecting from file extraction attacks via ADB.
 * </p>
 */
public final class SessionStore {
    private static final String PREF = "session_secure";
    private static final String KEY_ENCRYPTED_CRYPTO = "encrypted_crypto_key";
    private static final String KEYSTORE_ALIAS = "weightr_master_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TAG = "SessionStore";

    private static volatile SessionStore INSTANCE;
    private final SharedPreferences prefs;
    private final KeyStore keyStore;

    private SessionStore(Context appCtx) {
        this.prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init Keystore", e);
        }
    }

    public static SessionStore get(Context ctx) {
        if (INSTANCE != null) return INSTANCE;
        synchronized (SessionStore.class) {
            if (INSTANCE == null) INSTANCE = new SessionStore(ctx.getApplicationContext());
        }
        return INSTANCE;
    }

    /** Save derived encryption key (encrypted with Keystore master key) */
    public void save(@NonNull SecretKey derivedKey) {
        try {
            SecretKey masterKey = getOrCreateMasterKey();
            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey);

            byte[] encrypted = cipher.doFinal(derivedKey.getEncoded());
            byte[] iv = cipher.getIV();

            // Store IV + encrypted data concatenated (Base64)
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String encoded = Base64.encodeToString(combined, Base64.NO_WRAP);
            prefs.edit().putString(KEY_ENCRYPTED_CRYPTO, encoded).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save encrypted key", e);
        }
    }

    public void reset() {
        prefs.edit().remove(KEY_ENCRYPTED_CRYPTO).apply();
    }

    @Nullable
    public SecretKey encryptionKey() {
        String encoded = prefs.getString(KEY_ENCRYPTED_CRYPTO, null);
        if (encoded == null) return null;

        try {
            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
            if (combined.length < 12 + 16) return null; // Min IV (12) + ciphertext

            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            SecretKey masterKey = getMasterKey(); // Must exist
            if (masterKey == null) return null;

            Cipher cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(128, iv));

            byte[] decoded = cipher.doFinal(encrypted);
            return new javax.crypto.spec.SecretKeySpec(decoded, "AES");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load/decrypt key", e);
            return null;
        }
    }

    private SecretKey getOrCreateMasterKey() throws Exception {
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null);
            return entry.getSecretKey();
        }

        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build();
        kg.init(spec);
        return kg.generateKey();
    }

    @Nullable
    private SecretKey getMasterKey() throws Exception {
        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) return null;
        KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null);
        return entry.getSecretKey();
    }
}