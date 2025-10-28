package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Invitation
 * Represents an invitation issued to an Entrant for a specific Event.
 *
 */
@IgnoreExtraProperties
public class Invitation {

    @Exclude
    private String id;

    private String eventId;
    private String entrantId;
    private InvitationStatus status;
    private long issuedAtUtc;
    private long expiresAtUtc;

    public Invitation() {}

    public Invitation(@NonNull String eventId, @NonNull String entrantId, long issuedAtUtc, long expiresAtUtc) {
        this.eventId = eventId;
        this.entrantId = entrantId;
        this.issuedAtUtc = issuedAtUtc;
        this.expiresAtUtc = expiresAtUtc;
        this.status = InvitationStatus.PENDING;
    }
    @Exclude
    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    @Exclude
    public  boolean isExpired(long now){
        return now > expiresAtUtc;
    }

    @Exclude
    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    @Exclude
    public boolean isDeclined() {
        return status == InvitationStatus.DECLINED;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    @NonNull
    public String getEventId() {
        return eventId;
    }

    public void setEventId(@NonNull String eventId) {
        this.eventId = eventId;
    }

    @NonNull
    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(@NonNull String entrantId) {
        this.entrantId = entrantId;
    }

    @NonNull
    public InvitationStatus getStatus() {
        return status == null ? InvitationStatus.PENDING : status;
    }

    public void setStatus(@NonNull InvitationStatus status) {
        this.status = status;
    }

    public long getIssuedAtUtc() {
        return issuedAtUtc;
    }

    public void setIssuedAtUtc(long issuedAtUtc) {
        this.issuedAtUtc = issuedAtUtc;
    }

    public long getExpiresAtUtc() {
        return expiresAtUtc;
    }

    public void setExpiresAtUtc(long expiresAtUtc) {
        this.expiresAtUtc = expiresAtUtc;
    }
}
