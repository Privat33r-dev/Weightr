package com.snhu.weightr.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.snhu.weightr.data.session.SessionStore;

// Wrapper over SharedPreferences to store app settings.
public final class SettingsStore {
    private static final String PREF = "settings";
    private static final String KEY_LAST_CONGRATS_DATE = "last_congrats_date";

    private static volatile SettingsStore INSTANCE;
    private final SharedPreferences prefs;

    public void setLastCongratsDate(@NonNull String iso) {
        prefs.edit().putString(KEY_LAST_CONGRATS_DATE, iso).apply();
    }


    private SettingsStore(Context appCtx) {
        this.prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static SettingsStore get(Context ctx) {
        if (INSTANCE != null) return INSTANCE;
        synchronized (SessionStore.class) {
            if (INSTANCE == null) INSTANCE = new SettingsStore(ctx.getApplicationContext());
        }
        return INSTANCE;
    }
}
