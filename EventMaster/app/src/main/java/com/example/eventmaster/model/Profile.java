package com.example.eventmaster.model;

/**
 * Unified Profile model for EventMaster.
 * Firestore path: /profiles/{userId}
 */
public class Profile {
    private String userId;             // primary key (doc id)
    private String deviceId;           // optional: device-based identifier
    private String name;               // full name
    private String email;              // required
    private String phoneNumber;        // optional
    private String profileImageUrl;    // optional
    private String fcmToken;           // optional (for FCM notifications)
    private boolean notificationsEnabled; // defaults true
    private String role;               // "entrant" | "organizer" | "admin" (defaults "entrant")

    /** No-arg constructor required for Firestore. */
    public Profile() {
        this.notificationsEnabled = true;
        this.role = "entrant";
    }

    /** Minimal ctor (original 3-arg): deviceId defaults to userId. */
    public Profile(String userId, String name, String email) {
        this.userId = userId;
        this.deviceId = userId; // sensible default if not provided
        this.name = name;
        this.email = email;
        this.notificationsEnabled = true;
        this.role = "entrant";
    }

    /** Back-compat ctor (4-arg) used by tests and other branch. */
    public Profile(String userId, String deviceId, String name, String email) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.name = name;
        this.email = email;
        this.notificationsEnabled = true;
        this.role = "entrant";
    }

    /** "Full" ctor (from first version), sets deviceId = userId by default. */
    public Profile(String userId, String name, String email,
                   String phoneNumber, String profileImageUrl) {
        this.userId = userId;
        this.deviceId = userId; // default
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
        this.notificationsEnabled = true;
        this.role = "entrant";
    }

    // ----- Getters / Setters -----

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Optional aliases if any code uses id terminology
    public String getId() { return getUserId(); }
    public void setId(String id) { setUserId(id); }
}
