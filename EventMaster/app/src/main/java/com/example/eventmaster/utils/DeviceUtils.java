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
     * @return Unique device identifier, never null
     */
    public static String getDeviceId(Context context) {
        try {
            String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (deviceId == null || deviceId.isEmpty()) {
                // Fallback for emulators or devices where ANDROID_ID is null
                return "DEVICE_" + System.currentTimeMillis();
            }
            return deviceId;
        } catch (Exception e) {
            // Fallback in case of any error
            return "DEVICE_FALLBACK_" + System.currentTimeMillis();
        }
    }
}



