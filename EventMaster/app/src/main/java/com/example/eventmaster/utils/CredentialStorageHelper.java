package com.example.eventmaster.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.util.Base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper class for securely storing and retrieving user credentials for "Remember Me" functionality.
 * Uses EncryptedSharedPreferences to store email and password in an encrypted format.
 */
public class CredentialStorageHelper {
    
    private static final String PREFS_NAME = "encrypted_credentials";
    private static final String KEY_EMAIL = "saved_email";
    private static final String KEY_PASSWORD = "saved_password";
    
    /**
     * Gets or creates the encrypted SharedPreferences instance.
     * 
     * @param context The application context
     * @return EncryptedSharedPreferences instance, or null if initialization fails
     */
    @Nullable
    private static SharedPreferences getEncryptedPrefs(@NonNull Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            android.util.Log.e("CredentialStorage", "Failed to create encrypted prefs", e);
            return null;
        }
    }
    
    /**
     * Saves encrypted email and password credentials.
     * 
     * @param context The application context
     * @param email The user's email address
     * @param password The user's password
     * @return true if credentials were saved successfully, false otherwise
     */
    public static boolean saveCredentials(@NonNull Context context, 
                                         @NonNull String email, 
                                         @NonNull String password) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return false;
        }
        
        try {
            prefs.edit()
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_PASSWORD, password)
                    .apply();
            return true;
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to save credentials", e);
            return false;
        }
    }
    
    /**
     * Retrieves the saved email address.
     * 
     * @param context The application context
     * @return The saved email, or null if not found or error occurred
     */
    @Nullable
    public static String getSavedEmail(@NonNull Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return null;
        }
        
        try {
            return prefs.getString(KEY_EMAIL, null);
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to get saved email", e);
            return null;
        }
    }
    
    /**
     * Retrieves the saved password (decrypted).
     * 
     * @param context The application context
     * @return The saved password, or null if not found or error occurred
     */
    @Nullable
    public static String getSavedPassword(@NonNull Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return null;
        }
        
        try {
            return prefs.getString(KEY_PASSWORD, null);
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to get saved password", e);
            return null;
        }
    }
    
    /**
     * Checks if saved credentials exist.
     * 
     * @param context The application context
     * @return true if both email and password are saved, false otherwise
     */
    public static boolean hasSavedCredentials(@NonNull Context context) {
        String email = getSavedEmail(context);
        String password = getSavedPassword(context);
        return email != null && !email.isEmpty() && password != null && !password.isEmpty();
    }
    
    /**
     * Clears all saved credentials.
     * 
     * @param context The application context
     */
    public static void clearCredentials(@NonNull Context context) {
        SharedPreferences prefs = getEncryptedPrefs(context);
        if (prefs == null) {
            return;
        }
        
        try {
            prefs.edit()
                    .remove(KEY_EMAIL)
                    .remove(KEY_PASSWORD)
                    .apply();
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to clear credentials", e);
        }
    }
    
    // ==================== Password Encryption for Firestore ====================
    // These methods encrypt/decrypt passwords for storage in Firestore
    // Uses AES encryption with a fixed key (same across all devices)
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    /**
     * Gets a fixed encryption key that's the same across all devices.
     * This allows passwords encrypted on one device to be decrypted on another.
     * Uses SHA-256 to derive a 256-bit key from a constant string.
     */
    @Nullable
    private static SecretKey getEncryptionKey(@NonNull Context context) {
        try {
            // Use a fixed key derived from a constant string
            // This ensures the same key is used across all devices
            String keyString = "EventMasterOrganizerApplicationKey2024!@#";
            
            // Derive a 256-bit key from the string using SHA-256
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(keyString.getBytes(StandardCharsets.UTF_8));
            
            // Use first 32 bytes (256 bits) for AES-256
            byte[] aesKey = new byte[32];
            System.arraycopy(keyBytes, 0, aesKey, 0, 32);
            
            return new SecretKeySpec(aesKey, ALGORITHM);
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to get encryption key", e);
            return null;
        }
    }
    
    /**
     * Encrypts a password for storage in Firestore.
     * 
     * @param context The application context
     * @param password The password to encrypt
     * @return Encrypted password as Base64 string, or null if encryption fails
     */
    @Nullable
    public static String encryptPassword(@NonNull Context context, @NonNull String password) {
        try {
            SecretKey key = getEncryptionKey(context);
            if (key == null) {
                return null;
            }
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to encrypt password", e);
            return null;
        }
    }
    
    /**
     * Decrypts a password from Firestore.
     * 
     * @param context The application context
     * @param encryptedPassword The encrypted password (Base64 string)
     * @return Decrypted password, or null if decryption fails
     */
    @Nullable
    public static String decryptPassword(@NonNull Context context, @NonNull String encryptedPassword) {
        try {
            SecretKey key = getEncryptionKey(context);
            if (key == null) {
                return null;
            }
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedPassword, Base64.DEFAULT));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            android.util.Log.e("CredentialStorage", "Failed to decrypt password", e);
            return null;
        }
    }
}

