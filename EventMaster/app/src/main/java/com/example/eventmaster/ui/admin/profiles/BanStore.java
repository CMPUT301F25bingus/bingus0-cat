package com.example.eventmaster.ui.admin.profiles;

import android.content.Context;
import android.content.SharedPreferences;

public final class BanStore {
    private static final String PREFS = "admin_bans";
    private final SharedPreferences sp;

    public BanStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean isBanned(String email) {
        if (email == null) return false;
        return sp.getBoolean(key(email), false);
    }

    public void setBanned(String email, boolean banned) {
        if (email == null) return;
        sp.edit().putBoolean(key(email), banned).apply();
    }

    public void clear(String email) {
        if (email == null) return;
        sp.edit().remove(key(email)).apply();
    }

    public void clearAll() {
        sp.edit().clear().apply();
    }

    private String key(String email) {
        return "ban_" + email.trim().toLowerCase();
    }
}

