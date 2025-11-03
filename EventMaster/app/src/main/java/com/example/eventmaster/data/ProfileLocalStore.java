package com.example.eventmaster.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class ProfileLocalStore {
    private static final String PREFS = "profile_local_store";
    private static final String K_NAME = "name";
    private static final String K_EMAIL = "email";
    private static final String K_PHONE = "phone";

    private final SharedPreferences sp;

    public ProfileLocalStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(String name, String email, String phone) {
        sp.edit()
                .putString(K_NAME, name)
                .putString(K_EMAIL, email)
                .putString(K_PHONE, phone)
                .apply();
    }

    public String name()  { return sp.getString(K_NAME,  null); }
    public String email() { return sp.getString(K_EMAIL, null); }
    public String phone() { return sp.getString(K_PHONE, null); }

    /** A profile “exists” if we have both a name and email saved */
    public boolean hasProfile() {
        return name() != null && email() != null && !name().isEmpty() && !email().isEmpty();
    }

    public void clear() { sp.edit().clear().apply(); }
}


