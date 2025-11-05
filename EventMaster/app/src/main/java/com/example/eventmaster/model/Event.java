package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.Timestamp;
import java.util.Objects;

/**
 * Event
 *
 * Role:
 *  - Represents an event document stored in Firestore under /events/{eventId}.
 *  - Contains metadata such as title, description, location, registration window, and media URLs.
 *
 * Design Pattern:
 *  - Model class (POJO) used within the MVC architecture.
 *
 * Outstanding Issues:
 *  - None known.
 */
public class Event {

    // --- Fields ---
    private String id; // Firestore document ID (not stored as field)
    private String title;
    private String description;
    private String location;
    private Timestamp registrationOpen;
    private Timestamp registrationClose;
    private String posterUrl;
    private String qrUrl;
    private String status = "DRAFT";

    /** Default empty constructor required by Firestore for deserialization. */
    @SuppressWarnings("unused")
    public Event() {}

    /**
     * Constructs a new Event object with core fields.
     *
     * @param title event title (required)
     * @param description optional event description
     * @param location optional event location
     * @param registrationOpen registration start timestamp
     * @param registrationClose registration end timestamp
     */
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

    /**
     * Validates the event’s required fields.
     *
     * @return {@code null} if valid, otherwise an error message.
     */
    @Nullable
    public String validate() {
        if (title == null || title.trim().isEmpty()) return "Title is required.";
        if (registrationOpen == null) return "Registration open time is required.";
        if (registrationClose == null) return "Registration close time is required.";
        if (registrationOpen.compareTo(registrationClose) > 0)
            return "Registration open time must be before or equal to close time.";
        return null;
    }

    // --- Getters and Setters ---

    /** @return Firestore document ID. */
    @Nullable public String getId() { return id; }
    /** @param id sets the Firestore document ID. */
    public void setId(@Nullable String id) { this.id = id; }

    /** @return event title. */
    @NonNull public String getTitle() { return title; }
    /** @param title sets the event title. */
    public void setTitle(@NonNull String title) { this.title = title; }

    /** @return optional description. */
    @Nullable public String getDescription() { return description; }
    /** @param description sets event description. */
    public void setDescription(@Nullable String description) { this.description = description; }

    /** @return event location. */
    @Nullable public String getLocation() { return location; }
    /** @param location sets event location. */
    public void setLocation(@Nullable String location) { this.location = location; }

    /** @return registration open timestamp. */
    @NonNull public Timestamp getRegistrationOpen() { return registrationOpen; }
    /** @param registrationOpen sets registration start. */
    public void setRegistrationOpen(@NonNull Timestamp registrationOpen) { this.registrationOpen = registrationOpen; }

    /** @return registration close timestamp. */
    @NonNull public Timestamp getRegistrationClose() { return registrationClose; }
    /** @param registrationClose sets registration end. */
    public void setRegistrationClose(@NonNull Timestamp registrationClose) { this.registrationClose = registrationClose; }

    /** @return poster image URL. */
    @Nullable public String getPosterUrl() { return posterUrl; }
    /** @param posterUrl sets poster image URL. */
    public void setPosterUrl(@Nullable String posterUrl) { this.posterUrl = posterUrl; }

    /** @return QR code image URL. */
    @Nullable public String getQrUrl() { return qrUrl; }
    /** @param qrUrl sets QR code image URL. */
    public void setQrUrl(@Nullable String qrUrl) { this.qrUrl = qrUrl; }

    /** @return event status (e.g., “DRAFT”, “PUBLISHED”). */
    @NonNull public String getStatus() { return status; }
    /** @param status sets current event status. */
    public void setStatus(@NonNull String status) { this.status = status; }

    // --- Equality and Hashing ---

    /**
     * Compares events by field values (ignores {@code id}).
     *
     * @param o object to compare
     * @return true if both events have equal values
     */
    @Override
    public boolean equals(Object o) {
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

    /**
     * Generates a hash code for this Event.
     *
     * @return integer hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(title, description, location, registrationOpen, registrationClose, posterUrl, qrUrl, status);
    }
}
