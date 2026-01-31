package com.snhu.weightr.data.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.snhu.weightr.data.session.SessionStore;

// Wrapper over SharedPreferences to store app settings.
public final class SettingsStore {
    private static final String PREF = "settings";
    private static final String PREF_SORT_DESCENDING = "sort_descending";

    private static volatile SettingsStore INSTANCE;
    private final SharedPreferences prefs;

    public boolean isSortDescending() {
        return prefs.getBoolean(PREF_SORT_DESCENDING, true);
    }

    public void setSortDescending(boolean descending) {
        prefs.edit().putBoolean(PREF_SORT_DESCENDING, descending).apply();
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