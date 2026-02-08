package com.snhu.weightr.util;

import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Utility helpers for text handling and input safety.
 */
public final class Utils {
    private Utils() {
    } // Prevent instantiation

    public static final double MAX_ALLOWED_WEIGHT = 2000;
    private final static double TOLERANCE = 1e-9;

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

    public static String isoUtc(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(d);
    }

    public static boolean approximatelyEqual(double a, double b) {
        return Math.abs(a - b) < TOLERANCE;
    }


}
