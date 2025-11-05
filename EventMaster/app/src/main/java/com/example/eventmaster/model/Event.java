package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import java.util.Objects;

/**
 * Event domain model stored at /events/{eventId}
 *
 * Required: title, registrationOpen <= registrationClose
 * Optional: description, location, posterUrl, qrUrl
 */
public class Event {

    // Firestore doc id (not stored as field; set after create or when loaded)
    private String id;

    // Core
    private String title;
    private String description;
    private String location;

    // Registration window
    private Timestamp registrationOpen;
    private Timestamp registrationClose;

    // Media
    private String posterUrl;
    private String qrUrl;

    // DRAFT or PUBLISHED
    private String status = "DRAFT";

    /** Needed by Firestore */
    @SuppressWarnings("unused")
    public Event() {}

    public Event(@NonNull String title,
                 @Nullable String description,
                 @Nullable String location,
                 @NonNull Timestamp registrationOpen,
                 @NonNull Timestamp registrationClose) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
    }

    /** Basic validation; returns null if valid, else error message. */
    @Nullable
    public String validate() {
        if (title == null || title.trim().isEmpty()) return "Title is required.";
        if (registrationOpen == null) return "Registration open time is required.";
        if (registrationClose == null) return "Registration close time is required.";
        if (registrationOpen.compareTo(registrationClose) > 0) {
            return "Registration open time must be before or equal to close time.";
        }
        return null;
    }

    // --- Getters/Setters ---

    @Nullable public String getId() { return id; }
    public void setId(@Nullable String id) { this.id = id; }

    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }

    @Nullable public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    @Nullable public String getLocation() { return location; }
    public void setLocation(@Nullable String location) { this.location = location; }

    @NonNull public Timestamp getRegistrationOpen() { return registrationOpen; }
    public void setRegistrationOpen(@NonNull Timestamp registrationOpen) { this.registrationOpen = registrationOpen; }

    @NonNull public Timestamp getRegistrationClose() { return registrationClose; }
    public void setRegistrationClose(@NonNull Timestamp registrationClose) { this.registrationClose = registrationClose; }

    @Nullable public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(@Nullable String posterUrl) { this.posterUrl = posterUrl; }

    @Nullable public String getQrUrl() { return qrUrl; }
    public void setQrUrl(@Nullable String qrUrl) { this.qrUrl = qrUrl; }

    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    // value equality (ignores id)
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event e = (Event) o;
        return Objects.equals(title, e.title) &&
                Objects.equals(description, e.description) &&
                Objects.equals(location, e.location) &&
                Objects.equals(registrationOpen, e.registrationOpen) &&
                Objects.equals(registrationClose, e.registrationClose) &&
                Objects.equals(posterUrl, e.posterUrl) &&
                Objects.equals(qrUrl, e.qrUrl) &&
                Objects.equals(status, e.status);
    }

    @Override public int hashCode() {
        return Objects.hash(title, description, location, registrationOpen, registrationClose, posterUrl, qrUrl, status);
    }
}
