package com.snhu.weightr.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

    public static String isoUtc(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(d);
    }

    public static boolean hasAllSmsPermissions(@NonNull Context context) {
        ArrayList<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.SEND_SMS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            perms.add(Manifest.permission.READ_PHONE_NUMBERS);
        } else {
            perms.add(Manifest.permission.READ_PHONE_STATE);
        }

        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}
