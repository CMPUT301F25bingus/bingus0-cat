package com.example.eventmaster.model;

/**
 * Unified Profile model for EventMaster.
 * Firestore path: /profiles/{userId}
 *
 * Back-compat:
 * - getId()/setId() alias userId
 * - getPhone()/setPhone() alias phoneNumber
 * - role defaults to "entrant"
 * - banned defaults to false, active defaults to true
 * - notificationsEnabled defaults to true
 */
public class Profile {

    // ---- Canonical / stored fields ----
    private String userId;                 // Firestore doc id (a.k.a. id)
    private String deviceId;               // optional
    private String name;                   // display name
    private String email;                  // required by most flows
    private String phoneNumber;            // optional
    private String profileImageUrl;        // optional
    private String fcmToken;               // optional (push)
    private Boolean notificationsEnabled;  // default true
    private String role;                   // "entrant" | "organizer" | "admin" (default "entrant")
    private Boolean banned;                // default false
    private Boolean active;                // default true (soft-delete flag)

    /** No-arg constructor required by Firestore. */
    public Profile() {
        this.notificationsEnabled = true;
        this.role = "entrant";
        this.banned = false;
        this.active = true;
    }

    /** Convenience ctor (matches older code): id/name/email/phone, defaults role/banned/active. */
    public Profile(String id, String name, String email, String phone) {
        this(); // set defaults
        this.userId = id;
        this.deviceId = id; // sensible default
        this.name = name;
        this.email = email;
        this.phoneNumber = phone;
    }

    /** Convenience ctor including role (older branch). */
    public Profile(String id, String name, String email, String phone, String role) {
        this(id, name, email, phone);
        this.role = (role == null ? "entrant" : role);
    }

    /** Newer-style ctor (no phone), deviceId defaults to userId. */
    public Profile(String userId, String name, String email) {
        this();
        this.userId = userId;
        this.deviceId = userId;
        this.name = name;
        this.email = email;
    }

    // ---- Getters/Setters ----

    // Canonical ID
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email == null ? "" : email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled == null ? true : notificationsEnabled;
    }
    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getRole() { return role == null ? "entrant" : role; }
    public void setRole(String role) { this.role = role; }

    public boolean getBanned() { return banned == null ? false : banned; }
    public void setBanned(Boolean banned) { this.banned = banned; }

    public boolean getActive() { return active == null ? true : active; }
    public void setActive(Boolean active) { this.active = active; }

    // ---- Aliases for back-compat ----
    /** Alias for userId to support older code. */
    public String getId() { return getUserId(); }
    public void setId(String id) { setUserId(id); }

    /** Alias for phoneNumber to support older code. */
    public String getPhone() { return getPhoneNumber(); }
    public void setPhone(String phone) { setPhoneNumber(phone); }
}
