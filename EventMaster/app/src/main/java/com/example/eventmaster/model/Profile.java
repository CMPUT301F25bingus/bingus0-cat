package com.example.eventmaster.model;

/**
 * Represents a user profile in the EventMaster application.
 * Contains personal information for entrants including contact details.
 * 
 * Used for identifying users, sending notifications, and displaying user information.
 */
public class Profile {
    private String userId;           // Unique device-based identifier
    private String name;             // Full name of the user
    private String email;            // Email address (required)
    private String phoneNumber;      // Phone number (optional)
    private String profileImageUrl;  // URL to profile picture (optional)
    private String fcmToken;         // Firebase Cloud Messaging token for notifications
    private boolean notificationsEnabled; // Opt-in/out for notifications

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Profile() {
        this.notificationsEnabled = true; // Default to enabled
    }

    /**
     * Creates a new Profile with required fields.
     * 
     * @param userId Unique identifier for the user (typically device ID)
     * @param name Full name of the user
     * @param email Email address
     */
    public Profile(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.notificationsEnabled = true;
    }

    /**
     * Full constructor with all fields.
     */
    public Profile(String userId, String name, String email, String phoneNumber, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.notificationsEnabled = true;
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }
}
