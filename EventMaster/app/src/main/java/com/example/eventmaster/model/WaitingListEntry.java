package com.example.eventmaster.model;

public class WaitingListEntry {
    private String entrantId;
    private String entrantName;
    private String email;
    private String eventId;
    private long timestamp;
    private String status; // "waiting", "selected", "rejected"

    public WaitingListEntry() {} // Firestore requires empty constructor

    public WaitingListEntry(String entrantId, String entrantName, String email, String eventId, String status) {
        this.entrantId = entrantId;
        this.entrantName = entrantName;
        this.email = email;
        this.eventId = eventId;
        this.timestamp = System.currentTimeMillis();
        this.status = status;
    }

    // Default constructor for new entrants (waiting by default)
    public WaitingListEntry(String entrantId, String entrantName, String email, String eventId) {
        this(entrantId, entrantName, email, eventId, "waiting");
    }

    public String getEntrantId() { return entrantId; }
    public String getEntrantName() { return entrantName; }
    public String getEmail() { return email; }
    public String getEventId() { return eventId; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
