package com.snhu.weightr.util;

import android.text.Editable;

import androidx.annotation.Nullable;

public class utils {

    public static String safeText(@Nullable Editable e) {
        return e == null ? "" : e.toString().trim();
    }
}
