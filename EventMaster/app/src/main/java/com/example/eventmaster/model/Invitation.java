package com.example.eventmaster.model;

import java.util.Date;

/**
 * Invitation
 * Represents an invitation issued to an Entrant for a specific Event.
 *
 */

public class Invitation {
    private String id;        // not stored; set from doc id
    private String eventId;
    private String entrantId;
    private String status;    // "PENDING" | "ACCEPTED" | "DECLINED"

    private Date replyBy;


    public Invitation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEntrantId() { return entrantId; }
    public void setEntrantId(String entrantId) { this.entrantId = entrantId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getReplyBy() { return replyBy; }
    public void setReplyBy(Date replyBy) { this.replyBy = replyBy; }
}
