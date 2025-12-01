package com.example.eventmaster.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple helper to remember whether the last active session
 * on this device was as an entrant (deviceId-based).
 */
public final class EntrantSessionPrefs {

    private static final String PREFS_NAME = "entrant_session_prefs";
    private static final String KEY_LAST_ENTRANT_ACTIVE = "last_entrant_active";

    private EntrantSessionPrefs() {
        // Utility class
    }

    /**
     * Marks whether the last active session on this device was as an entrant.
     *
     * @param context  application or activity context
     * @param isActive true if last session was entrant, false otherwise
     */
    public static void setLastEntrantActive(Context context, boolean isActive) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_LAST_ENTRANT_ACTIVE, isActive)
                .apply();
    }

    /**
     * Checks whether the last recorded session on this device was as an entrant.
     *
     * @param context application or activity context
     * @return true if last session was entrant, false otherwise
     */
    public static boolean wasLastEntrantActive(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LAST_ENTRANT_ACTIVE, false);
    }

    /**
     * Clears any stored entrant session flags.
     *
     * @param context application or activity context
     */
    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}


