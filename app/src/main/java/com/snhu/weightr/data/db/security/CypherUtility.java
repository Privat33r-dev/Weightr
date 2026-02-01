package com.snhu.weightr.data.db.security;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption utility for sensitive data.
 * <p>
 * Fixed app-wide salt for deterministic key (same password -> same key across devices).
 * </p>
 */
public final class CypherUtility {

    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";

    /*
    For IVs, it is recommended that implementations restrict support to the length of 96 bits, to
    promote interoperability, efficiency, and simplicity of design.

    Source: https://doi.org/10.6028/NIST.SP.800-38D

    GCM_IV_LENGTH is in bytes. 96 bits = 12 bytes
     */
    private static final int GCM_IV_LENGTH = 12;

    /*
    GCM Tag size in bits.
    As stated in NIST.SP.800-38D,
    "In general, t [Tag size] may be any one of the following five values: 128, 120, 112, 104, or 96".
     */
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH_BITS = 256;   // AES-256
    /*
     * Increasing iterations would make it slower to calculate each individual hash,
     * which slows down hash bruteforce attacks. On the other side, it also slows down password
     * checking, so it has to be a golden middle between performance and security.
     */
    private static final int PBKDF2_ITERATIONS = 200_000;

    // Fixed app-wide salt (deterministic, not secret; prevents from using auth hashes on the server
    // to decrypt user data)
    private static final byte[] FIXED_SALT = "weightr.fixed.salt".getBytes(StandardCharsets.UTF_8);

    private static final String KEY_DERIVATION_ALGO = "PBKDF2WithHmacSHA512";

    private final SecretKey encryptionKey;

    /**
     * Constructor from pre-derived key.
     * <p>
     * Use `deriveKey` static method to derive key.
     * </p>
     */
    public CypherUtility(@NonNull SecretKey key) {
        this.encryptionKey = key;
    }

    /**
     * Public static utility to derive key (used in login for storage). Requires Android SDK 26+.
     */
    @NonNull
    public static SecretKey deriveKey(@NonNull char[] password, @Nullable byte[] salt) throws IllegalStateException {
        if (salt == null) salt = FIXED_SALT;
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGO);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            // This should never happen on supported devices (minSdk 26+), but we still wrap defensively
            throw new IllegalStateException("Unexpected error during key derivation", e);
        } finally {
            spec.clearPassword(); // Clear password from spec (best practice)
        }
    }

    @NonNull
    public String encrypt(@NonNull String plaintext) throws Exception {
        byte[] iv = generateIv();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);

        return Base64.encodeToString(buffer.array(), Base64.DEFAULT);
    }

    @NonNull
    public String decrypt(@NonNull String encryptedBase64) throws Exception {
        byte[] data = Base64.decode(encryptedBase64, Base64.DEFAULT);

        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

        byte[] plaintextBytes = cipher.doFinal(ciphertext);
        return new String(plaintextBytes, StandardCharsets.UTF_8);
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}