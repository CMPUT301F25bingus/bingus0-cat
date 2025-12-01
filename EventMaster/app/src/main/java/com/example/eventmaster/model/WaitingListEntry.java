package com.example.eventmaster.model;

import com.google.firebase.Timestamp;
import java.util.Date;

/**
 * Represents an entrant's entry in an event's waiting or chosen list.
 */
public class WaitingListEntry {

    private String entryId;     // Firestore document ID
    private String eventId;     // Event reference
    private String userId;      // Entrant’s unique ID
    private String entrantName; // Optional display name
    private String email;
    private String phone;
    private Timestamp joinedDate;
    private Double lat;    // Optional geolocation
    private Double lng;   // Optional geolocation
    private String status;      // "waiting", "chosen", "accepted", "declined", "cancelled"
    private Profile profile;    // Profile information for the entrant

    // Empty constructor required by Firestore
    public WaitingListEntry() {}

    public WaitingListEntry(String entryId, String eventId, String userId, Date joinedDate) {
        this(entryId, eventId, userId, "Unknown", null, null, joinedDate, "waiting");
    }


    // Full constructor
    public WaitingListEntry(String entryId, String eventId, String userId, String entrantName,
                            String email, String phone, Date joinedDate, String status) {
        this.entryId = entryId;
        this.eventId = eventId;
        this.userId = userId;
        this.entrantName = entrantName;
        this.email = email;
        this.phone = phone;
        this.joinedDate = joinedDate != null ? new Timestamp(joinedDate) : Timestamp.now();
        this.status = status != null ? status : "waiting";
    }

    // Convenience constructor (defaults to "waiting" status)
    public WaitingListEntry(String entryId, String eventId, String userId, String entrantName,
                            String email, String phone) {
        this(entryId, eventId, userId, entrantName, email, phone, new Date(), "waiting");
    }

    // Getters and Setters
    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEntrantName() { return entrantName; }
    public void setEntrantName(String entrantName) { this.entrantName = entrantName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Timestamp getJoinedDate() { return joinedDate; }
    public void setJoinedDate(Timestamp joinedDate) { this.joinedDate = joinedDate; }

    public Double getlat() { return lat; }
    public void setlat(Double lat) { this.lat = lat; }

    public Double getlng() { return lng; }
    public void setlng(Double lng) { this.lng = lng; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }
}
