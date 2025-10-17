package com.snhu.weightr.util;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utility helpers for text handling and input safety.
 */
public final class Utils {
    private Utils() {} // Prevent instantiation

    /**
     * Safely extracts trimmed text from an {@link Editable}.
     * Returns an empty string if the input is {@code null}.
     *
     * @param e Editable text, possibly null
     * @return Non-null, trimmed string
     */
    @NonNull
    public static String safeText(@Nullable Editable e) {
        return (e == null) ? "" : e.toString().trim();
    }
}
