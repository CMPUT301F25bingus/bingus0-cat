package com.example.eventmaster.model;

import com.google.firebase.Timestamp;

import java.util.Date;

/**
 * Represents an entrant's entry in an event's waiting list.
 * Tracks when they joined and optional geolocation data.
 */
public class WaitingListEntry {
    private String entryId;
    private String eventId;
    private String userId;
    private Timestamp joinedDate;
    private Double latitude;  // Optional geolocation
    private Double longitude; // Optional geolocation
    private String status; // "waiting", "selected", "accepted", "declined", "cancelled"

    // No-arg constructor required for Firestore
    public WaitingListEntry() {
    }

    /**
     * Creates a new waiting list entry.
     *
     * @param entryId    Unique identifier for this entry
     * @param eventId    ID of the event
     * @param userId     ID of the user joining the waiting list
     * @param joinedDate Date/time when user joined
     */
    public WaitingListEntry(String entryId, String eventId, String userId, Date joinedDate) {
        this.entryId = entryId;
        this.eventId = eventId;
        this.userId = userId;
        this.joinedDate = joinedDate != null ? new Timestamp(joinedDate) : null;
        this.status = "waiting";
    }

    // Getters and Setters

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getJoinedDate() {
        return joinedDate != null ? joinedDate.toDate() : null;
    }

    public void setJoinedDate(Date joinedDate) {
        this.joinedDate = joinedDate != null ? new Timestamp(joinedDate) : null;
    }
    
    public Timestamp getJoinedDateTimestamp() {
        return joinedDate;
    }
    
    public void setJoinedDateTimestamp(Timestamp joinedDate) {
        this.joinedDate = joinedDate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
