package com.snhu.weightr.data.db.security;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using PBKDF2.
 * <p>
 * Priority:
 * 1) PBKDF2WithHmacSHA3-512 (JDK 9+/some Android providers)
 * 2) PBKDF2WithHmacSHA512
 * </p>
 * Output format (PHC-style):
 * $pbkdf2-<alg>$v=<iterations>$salt=<base64>$hash=<base64>
 */
public final class PasswordHasher {

    private static final int ITERATIONS = 100_000;   // target ~100–250 ms
    private static final int SALT_LEN_BYTES = 32;
    private static final int KEY_LEN_BITS = 512;

    private static final String[] CANDIDATE_ALGORITHMS = {
            "PBKDF2WithHmacSHA3-512",
            "PBKDF2WithHmacSHA512"
    };

    private static final String RESOLVED_ALGO = resolveAlgorithm();

    /**
     * Hash a password; returns standardized PHC-style string.
     */
    public static String hash(final char[] password) {
        final byte[] salt = new byte[SALT_LEN_BYTES];
        new SecureRandom().nextBytes(salt);
        final byte[] dk = derive(password, salt, ITERATIONS, KEY_LEN_BITS, RESOLVED_ALGO);
        final String algTag = algTag(RESOLVED_ALGO);
        return "$pbkdf2-" + algTag +
                "$v=" + ITERATIONS +
                "$salt=" + Base64.encodeToString(salt, Base64.NO_WRAP) +
                "$hash=" + Base64.encodeToString(dk, Base64.NO_WRAP);
    }

    /**
     * Verify password against stored PBKDF2 hash.
     */
    public static boolean verify(final char[] password, final String stored) {
        if (stored == null || !stored.startsWith("$pbkdf2-")) return false;
        try {
            final String[] parts = stored.split("\\$");
            // parts[0] = "", parts[1] = pbkdf2-ALG, parts[2] = v=ITER, parts[3] = salt=..., parts[4] = hash=...
            final String alg = resolveAlgFromTag(parts[1]);
            final int iterations = safeParseInt(parts[2].substring(2), -1);
            final byte[] salt = Base64.decode(parts[3].substring(5), Base64.NO_WRAP);
            final byte[] expected = Base64.decode(parts[4].substring(5), Base64.NO_WRAP);

            final byte[] actual = derive(password, salt, iterations, expected.length * 8, alg);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- internals ----

    private static byte[] derive(char[] pw, byte[] salt, int iter, int bits, String alg) {
        try {
            final PBEKeySpec spec = new PBEKeySpec(pw, salt, iter, bits);
            final SecretKeyFactory skf = SecretKeyFactory.getInstance(alg);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2 derive failed for alg=" + alg, e);
        }
    }

    private static String resolveAlgorithm() {
        for (String alg : CANDIDATE_ALGORITHMS) {
            try {
                SecretKeyFactory.getInstance(alg);
                return alg;
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException(
                "No supported PBKDF2 algorithm available (tried SHA3-512, SHA-512).");
    }

    private static String algTag(String alg) {
        if (alg.endsWith("SHA3-512")) return "sha3-512";
        if (alg.endsWith("SHA512")) return "sha512";
        return alg.toLowerCase();
    }

    private static String resolveAlgFromTag(String tag) {
        if (tag.equalsIgnoreCase("pbkdf2-sha3-512")) return "PBKDF2WithHmacSHA3-512";
        if (tag.equalsIgnoreCase("pbkdf2-sha512")) return "PBKDF2WithHmacSHA512";
        throw new IllegalArgumentException("Unsupported algorithm tag: " + tag);
    }

    private static int safeParseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private PasswordHasher() {
    }
}