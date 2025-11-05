package com.example.eventmaster.model;

import java.util.Date;

/**
 * Represents a notification in the EventMaster application.
 * Used for tracking notification history and managing notification delivery.
 * 
 * Supports US 02.05.01, US 01.04.01, US 01.04.02, and US 03.08.01.
 */
public class Notification {
    
    /**
     * Types of notifications that can be sent in the system.
     */
    public enum NotificationType {
        LOTTERY_WON,        // Entrant was selected in lottery
        LOTTERY_LOST,       // Entrant was not selected in lottery
        INVITATION,         // General invitation to sign up
        REMINDER,           // Reminder about event
        CANCELLATION,       // Event or registration cancelled
        GENERAL             // General announcement from organizer
    }

    private String notificationId;     // Unique identifier
    private String eventId;            // Event this notification relates to
    private String recipientUserId;    // User who receives the notification
    private String senderUserId;       // Organizer who sent the notification
    private NotificationType type;     // Type of notification
    private String title;              // Notification title
    private String message;            // Notification message body
    private Date sentAt;               // Timestamp when sent
    private boolean isRead;            // Whether recipient has read it

    /**
     * Default constructor required for Firebase deserialization.
     */
    public Notification() {
        this.sentAt = new Date();
        this.isRead = false;
    }

    /**
     * Creates a new notification with required fields.
     * 
     * @param eventId The event this notification relates to
     * @param recipientUserId The user receiving the notification
     * @param senderUserId The organizer sending the notification
     * @param type The type of notification
     * @param title The notification title
     * @param message The notification message body
     */
    public Notification(String eventId, String recipientUserId, String senderUserId,
                       NotificationType type, String title, String message) {
        this.eventId = eventId;
        this.recipientUserId = recipientUserId;
        this.senderUserId = senderUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.sentAt = new Date();
        this.isRead = false;
    }

    // Getters and Setters

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}

