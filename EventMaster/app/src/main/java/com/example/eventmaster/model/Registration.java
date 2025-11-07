package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

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
}