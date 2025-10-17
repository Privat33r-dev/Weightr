package com.snhu.weightr.data.session;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionStore {
    private static final String PREF = "session";
    private static volatile SessionStore INSTANCE;
    private final SharedPreferences prefs;

    private SessionStore(Context appCtx) {
        this.prefs = appCtx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static SessionStore get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (SessionStore.class) {
                if (INSTANCE == null) INSTANCE = new SessionStore(ctx.getApplicationContext());
            }
        }
        return INSTANCE;
    }

    public void save(long userId, String username) {
        prefs.edit().putLong("uid", userId).putString("uname", username).apply();
    }

    public void reset() {
        prefs.edit().putLong("uid", -1L).putString("uname", null).apply();
    }

    public long userId() { return prefs.getLong("uid", -1L); }
    public String username() { return prefs.getString("uname", null); }

    public void clear() { prefs.edit().clear().apply(); }
}
