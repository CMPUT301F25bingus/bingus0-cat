package com.example.eventmaster.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.Objects;

/**
 * Unified Event model.
 *
 * Firestore path: /events/{id}
 *
 * Canonical fields use consistent names. Alias getters/setters are provided to
 * preserve compatibility with older code (e.g., getTitle()/setTitle()).
 *
 * Doc ID isn't stored by Firestore; call setId(doc.getId()) after reads.
 */
public class Event {

    // ---- Canonical Fields ----
    private String id;                    // Firestore doc ID (not stored as field)
    private String name;                  // aka "title"
    private String description;
    private String location;

    // When the actual event occurs (optional)
    private Timestamp eventDate;

    // Registration window
    private Timestamp registrationOpen;   // aka registrationStartDate
    private Timestamp registrationClose;  // aka registrationEndDate

    // Media / links
    private String posterUrl;
    private String qrUrl;

    // Organizer
    private String organizerId;
    private String organizerName;

    // Constraints / options
    private int capacity;                       // 0 = unspecified
    private @Nullable Integer waitingListLimit; // null = unlimited
    private boolean geolocationRequired;
    private double price;                       // 0.0 = free

    // Lifecycle
    private String status = "DRAFT";            // DRAFT | PUBLISHED | CLOSED ...

    /** Empty constructor required by Firestore. */
    @SuppressWarnings("unused")
    public Event() {}

    /** Convenience constructor for common fields (Timestamp-based). */
    public Event(@NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @Nullable Timestamp eventDate,
                 @NonNull Timestamp registrationOpen,
                 @NonNull Timestamp registrationClose,
                 @Nullable String organizerId,
                 @Nullable String organizerName,
                 int capacity,
                 double price) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.capacity = capacity;
        this.price = price;
    }

    /** Overload: Dates instead of Timestamps. */
    public Event(@NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @Nullable Date eventDate,
                 @NonNull Date registrationStartDate,
                 @NonNull Date registrationEndDate,
                 @Nullable String organizerId,
                 @Nullable String organizerName,
                 int capacity,
                 double price) {
        this(
                name,
                description,
                location,
                eventDate != null ? new Timestamp(eventDate) : null,
                new Timestamp(registrationStartDate),
                new Timestamp(registrationEndDate),
                organizerId,
                organizerName,
                capacity,
                price
        );
    }

    /** Overload: includes eventId (Timestamp-based). */
    public Event(@Nullable String eventId,
                 @NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @Nullable Timestamp eventDate,
                 @NonNull Timestamp registrationOpen,
                 @NonNull Timestamp registrationClose,
                 @Nullable String organizerId,
                 @Nullable String organizerName,
                 int capacity,
                 double price) {
        this(name, description, location, eventDate, registrationOpen, registrationClose,
                organizerId, organizerName, capacity, price);
        this.id = eventId;
    }

    /** Overload: includes eventId (Date-based). */
    public Event(@Nullable String eventId,
                 @NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @Nullable Date eventDate,
                 @NonNull Date registrationStartDate,
                 @NonNull Date registrationEndDate,
                 @Nullable String organizerId,
                 @Nullable String organizerName,
                 int capacity,
                 double price) {
        this(
                eventId,
                name,
                description,
                location,
                eventDate != null ? new Timestamp(eventDate) : null,
                new Timestamp(registrationStartDate),
                new Timestamp(registrationEndDate),
                organizerId,
                organizerName,
                capacity,
                price
        );
    }

    /** Minimal ctor used by tests: name/desc/location + reg window only (Timestamp-based). */
    public Event(@NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @NonNull Timestamp registrationOpen,
                 @NonNull Timestamp registrationClose) {
        this(name, description, location,
                /*eventDate*/ null,
                registrationOpen, registrationClose,
                /*organizerId*/ null, /*organizerName*/ null,
                /*capacity*/ 0, /*price*/ 0.0);
    }

    /** Minimal ctor used by tests: name/desc/location + reg window only (Date-based). */
    public Event(@NonNull String name,
                 @Nullable String description,
                 @Nullable String location,
                 @NonNull Date registrationStartDate,
                 @NonNull Date registrationEndDate) {
        this(name, description, location,
                /*eventDate*/ (Timestamp) null,
                new Timestamp(registrationStartDate),
                new Timestamp(registrationEndDate),
                /*organizerId*/ null, /*organizerName*/ null,
                /*capacity*/ 0, /*price*/ 0.0);
    }

    // ---------- Validation ----------

    /** @return null if valid, otherwise an error message. */
    @Nullable
    public String validate() {
        if (name == null || name.trim().isEmpty()) {
            return "Title is required.";
        }
        if (registrationOpen == null) {
            return "Registration open time is required.";
        }
        if (registrationClose == null) {
            return "Registration close time is required.";
        }
        if (registrationOpen.compareTo(registrationClose) > 0) {
            return "Registration open time must be before or equal to close time.";
        }
        return null;
    }

    // ---------- Getters / Setters ----------

    @Nullable public String getId() { return id; }
    public void setId(@Nullable String id) { this.id = id; }

    @NonNull public String getName() { return name == null ? "" : name; }
    public void setName(@NonNull String name) { this.name = name; }

    @Nullable public String getDescription() { return description; }
    public void setDescription(@Nullable String description) { this.description = description; }

    @Nullable public String getLocation() { return location; }
    public void setLocation(@Nullable String location) { this.location = location; }

    @Nullable public Timestamp getEventDateTimestamp() { return eventDate; }
    public void setEventDateTimestamp(@Nullable Timestamp eventDate) { this.eventDate = eventDate; }

    @Nullable public Date getEventDate() { return eventDate != null ? eventDate.toDate() : null; }
    public void setEventDate(@Nullable Date eventDate) {
        this.eventDate = (eventDate != null) ? new Timestamp(eventDate) : null;
    }

    @NonNull public Timestamp getRegistrationOpen() { return registrationOpen; }
    public void setRegistrationOpen(@NonNull Timestamp registrationOpen) { this.registrationOpen = registrationOpen; }

    @NonNull public Timestamp getRegistrationClose() { return registrationClose; }
    public void setRegistrationClose(@NonNull Timestamp registrationClose) { this.registrationClose = registrationClose; }

    @Nullable public Date getRegistrationStartDate() {
        return registrationOpen != null ? registrationOpen.toDate() : null;
    }
    public void setRegistrationStartDate(@Nullable Date d) {
        this.registrationOpen = (d != null) ? new Timestamp(d) : null;
    }
    @Nullable public Timestamp getRegistrationStartDateTimestamp() { return registrationOpen; }
    public void setRegistrationStartDateTimestamp(@Nullable Timestamp ts) { this.registrationOpen = ts; }

    @Nullable public Date getRegistrationEndDate() {
        return registrationClose != null ? registrationClose.toDate() : null;
    }
    public void setRegistrationEndDate(@Nullable Date d) {
        this.registrationClose = (d != null) ? new Timestamp(d) : null;
    }
    @Nullable public Timestamp getRegistrationEndDateTimestamp() { return registrationClose; }
    public void setRegistrationEndDateTimestamp(@Nullable Timestamp ts) { this.registrationClose = ts; }

    @Nullable public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(@Nullable String posterUrl) { this.posterUrl = posterUrl; }

    @Nullable public String getQrUrl() { return qrUrl; }
    public void setQrUrl(@Nullable String qrUrl) { this.qrUrl = qrUrl; }

    @Nullable public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(@Nullable String organizerId) { this.organizerId = organizerId; }

    @Nullable public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(@Nullable String organizerName) { this.organizerName = organizerName; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Nullable public Integer getWaitingListLimit() { return waitingListLimit; }
    public void setWaitingListLimit(@Nullable Integer waitingListLimit) { this.waitingListLimit = waitingListLimit; }

    public boolean isGeolocationRequired() { return geolocationRequired; }
    public void setGeolocationRequired(boolean geolocationRequired) { this.geolocationRequired = geolocationRequired; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @NonNull public String getStatus() { return status == null ? "DRAFT" : status; }
    public void setStatus(@NonNull String status) { this.status = status; }

    // ---------- Alias API (back-compat) ----------

    // eventId <-> id
    @Nullable public String getEventId() { return getId(); }
    public void setEventId(@Nullable String eventId) { setId(eventId); }

    // title <-> name
    @NonNull public String getTitle() { return getName(); }
    public void setTitle(@NonNull String title) { setName(title); }

    // ---------- Equality / Hashing (ignores id) ----------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event e = (Event) o;
        return capacity == e.capacity &&
                geolocationRequired == e.geolocationRequired &&
                Double.compare(e.price, price) == 0 &&
                Objects.equals(name, e.name) &&
                Objects.equals(description, e.description) &&
                Objects.equals(location, e.location) &&
                Objects.equals(eventDate, e.eventDate) &&
                Objects.equals(registrationOpen, e.registrationOpen) &&
                Objects.equals(registrationClose, e.registrationClose) &&
                Objects.equals(posterUrl, e.posterUrl) &&
                Objects.equals(qrUrl, e.qrUrl) &&
                Objects.equals(organizerId, e.organizerId) &&
                Objects.equals(organizerName, e.organizerName) &&
                Objects.equals(waitingListLimit, e.waitingListLimit) &&
                Objects.equals(status, e.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, location, eventDate,
                registrationOpen, registrationClose, posterUrl, qrUrl,
                organizerId, organizerName, capacity, waitingListLimit,
                geolocationRequired, price, status);
    }
}
