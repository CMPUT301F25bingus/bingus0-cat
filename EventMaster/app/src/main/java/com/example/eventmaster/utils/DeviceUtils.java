package com.example.eventmaster.utils;

import android.content.Context;
import android.provider.Settings;

/**
 * Utility class for device-related operations.
 * Provides device identification as per US 01.07.01 (device-based identification).
 */
public class DeviceUtils {

    /**
     * Gets the unique device ID for the current device.
     * Uses Android's Secure.ANDROID_ID which is unique per app installation.
     *
     * @param context Application context
     * @return Unique device identifier
     */
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}



