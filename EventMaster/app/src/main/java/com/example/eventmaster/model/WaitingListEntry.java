package com.example.eventmaster.model;

public class WaitingListEntry {
    private String entrantId;
    private String entrantName;
    private String email;
    private String phone;
    private String eventId;
    private long timestamp;
    private String status; // "waiting", "selected", "rejected"

    public WaitingListEntry() {} // Firestore requires empty constructor

    public WaitingListEntry(String entrantId, String entrantName, String email, String phone, String eventId, String status) {
        this.entrantId = entrantId;
        this.entrantName = entrantName;
        this.email = email;
        this.phone = phone;
        this.eventId = eventId;
        this.timestamp = System.currentTimeMillis();
        this.status = status;
    }

    // Default constructor for new entrants (waiting by default)
    public WaitingListEntry(String entrantId, String entrantName, String email, String phone, String eventId) {
        this(entrantId, entrantName, email, phone, eventId, "waiting");
    }

    // Getters
    public String getEntrantId() { return entrantId; }
    public String getEntrantName() { return entrantName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getEventId() { return eventId; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // Setters (required for Firestore deserialization)
    public void setEntrantId(String entrantId) { this.entrantId = entrantId; }
    public void setEntrantName(String entrantName) { this.entrantName = entrantName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setStatus(String status) { this.status = status; }
}
