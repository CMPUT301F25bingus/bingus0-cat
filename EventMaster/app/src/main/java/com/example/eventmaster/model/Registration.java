package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Represents an event registration in the EventMaster application.
 * 
 * A registration is created when an entrant accepts an invitation or is enrolled in an event.
 * 
 * Status values:
 * - ACTIVE: User is enrolled in the event
 * - CANCELLED_BY_ENTRANT: User declined or cancelled their registration
 * - CANCELLED_BY_ORGANIZER: Organizer cancelled the user's registration
 * 
 * Firestore Compatibility:
 * - Includes overloaded setters to handle both Long and Timestamp types
 * - This allows Firestore's serverTimestamp() to be deserialized correctly
 * 
 * Data Structure: events/{eventId}/registrations/{userId}
 */
@IgnoreExtraProperties

public class Registration {
    @Exclude
    private String id;
    private String eventId;
    private String entrantId;
    private RegistrationStatus status; // ACTIVE | CANCELLED_*
    private long createdAtUtc;
    private @Nullable Long cancelledAtUtc;

    public Registration(){
        this.status = RegistrationStatus.ACTIVE;
    }

    public Registration(@NonNull String eventId, @NonNull String entrantId, long createdAtUtc) {
        this.eventId = eventId;
        this.entrantId = entrantId;
        this.createdAtUtc = createdAtUtc;
        this.status = RegistrationStatus.ACTIVE;
    }

    public @Nullable String getId() {
        return id;
    }
    public void setId(@Nullable String id){
        this.id = id;
    }

    public @NonNull String getEventId() {
        return eventId;
    }
    public void setEventId(@NonNull String eventId){
        this.eventId = eventId;
    }

    public @NonNull String getEntrantId() {
        return entrantId;
    }
    public void setEntrantId(@NonNull String entrantId) {
        this.entrantId = entrantId;
    }

    public @NonNull RegistrationStatus getStatus() {
        return status == null ? RegistrationStatus.ACTIVE : status;
    }
    public void setStatus(@NonNull RegistrationStatus status) {
        this.status = status;
    }

    public long getCreatedAtUtc() {
        return createdAtUtc;
    }
    public void setCreatedAtUtc(long createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    public @Nullable Long getCancelledAtUtc() {
        return cancelledAtUtc;
    }
    public void setCancelledAtUtc(@Nullable Long cancelledAtUtc) {
        this.cancelledAtUtc = cancelledAtUtc;
    }
    
    // Firestore compatibility: handle Timestamp objects
    public void setCancelledAtUtc(@Nullable Timestamp timestamp) {
        this.cancelledAtUtc = (timestamp != null) ? timestamp.toDate().getTime() : null;
    }
    
    public void setCreatedAtUtc(@Nullable Timestamp timestamp) {
        this.createdAtUtc = (timestamp != null) ? timestamp.toDate().getTime() : 0L;
    }
}