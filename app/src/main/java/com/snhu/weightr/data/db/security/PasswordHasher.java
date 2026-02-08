package com.snhu.weightr.data.db.security;

import android.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using PBKDF2WithHmacSHA512.
 * <p>
 * Designed for minSdk 26+ (guarantees PBKDF2WithHmacSHA512 support).
 * Output format (PHC-style):
 * $pbkdf2-sha512$v=<iterations>$salt=<base64>$hash=<base64>
 * </p>
 */
public final class PasswordHasher {
    private static final String ALGO = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 100_000;       // ~100–250 ms on modern devices
    private static final int SALT_LEN_BYTES = 32;       // 256-bit salt
    private static final int KEY_LEN_BITS = 256;        // 256-bit derived key (standard for security)

    private PasswordHasher() {
        // Prevent instantiation
    }

    /**
     * Hash a password; returns standardized PHC-style string.
     *
     * @param password Raw password as char[] (to avoid String pooling)
     * @return PHC-formatted hash string
     */
    public static String hash(final char[] password) {
        final byte[] pepper = "Weightr.App.Password.Salt".getBytes();

        final byte[] dk = derive(password, pepper, ITERATIONS, KEY_LEN_BITS);

        return "$pbkdf2-sha512" + "$v=" + ITERATIONS + "$salt=" + Base64.encodeToString(pepper, Base64.NO_WRAP) + "$hash=" + Base64.encodeToString(dk, Base64.NO_WRAP);
    }

    // ---- internals ----

    private static byte[] derive(char[] password, byte[] salt, int iterations, int keyLenBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 derivation failed", e);
        }
    }

}