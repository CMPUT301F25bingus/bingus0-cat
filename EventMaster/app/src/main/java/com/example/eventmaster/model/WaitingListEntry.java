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
    private Double latitude;    // Optional geolocation
    private Double longitude;   // Optional geolocation
    private String status;      // "waiting", "chosen", "accepted", "declined", "cancelled"

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

    public void setJoinedDate(Date joinedDate) {
        this.joinedDate = joinedDate != null ? new Timestamp(joinedDate) : null;
    }

    public Timestamp getJoinedDateTimestamp() {
        return joinedDate;
    }

    public void setJoinedDateTimestamp(Timestamp joinedDate) {
        this.joinedDate = joinedDate;
    }
    public String getEntrantName() { return entrantName; }
    public void setEntrantName(String entrantName) { this.entrantName = entrantName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Timestamp getJoinedDate() { return joinedDate; }
    public void setJoinedDate(Timestamp joinedDate) { this.joinedDate = joinedDate; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
