package com.example.eventmaster.model;

/**
 * Represents a user profile in the Event Lottery System.
 * Contains personal information and notification preferences.
 */
public class Profile {
    private String userId;
    private String deviceId;
    private String name;
    private String email;
    private String phoneNumber; // Optional
    private boolean notificationsEnabled;
    private String role; // "entrant", "organizer", "admin"

    // No-arg constructor required for Firestore
    public Profile() {
    }

    /**
     * Creates a new user profile.
     *
     * @param userId   Unique identifier for the user
     * @param deviceId Device identifier
     * @param name     User's full name
     * @param email    User's email address
     */
    public Profile(String userId, String deviceId, String name, String email) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.notificationsEnabled = true;
        this.role = "entrant";
    }

    // Getters and Setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
