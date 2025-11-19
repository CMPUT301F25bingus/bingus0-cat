package com.example.eventmaster.common;

/**
 * Firestore document field names.
 * 
 * Purpose:
 * - Centralized field name constants
 * - Prevents typos in field names
 * - Makes schema changes easier to track
 * 
 * Usage:
 * import static com.example.eventmaster.common.FirestoreFields.*;
 * map.put(EVENT_ID, eventId);
 * String title = doc.getString(TITLE);
 */
public final class FirestoreFields {
    
    // Private constructor to prevent instantiation
    private FirestoreFields() {
        throw new AssertionError("Cannot instantiate FirestoreFields class");
    }
    
    // ==================== Event Fields ====================
    
    /** Event document ID field */
    public static final String EVENT_ID = "eventId";
    
    /** Event name field (canonical) */
    public static final String NAME = "name";
    
    /** Event title field (backward compatibility) */
    public static final String TITLE = "title";
    
    /** Event description field */
    public static final String DESCRIPTION = "description";
    
    /** Event location field */
    public static final String LOCATION = "location";
    
    /** Event date field */
    public static final String EVENT_DATE = "eventDate";
    
    /** Event organizer ID field */
    public static final String ORGANIZER_ID = "organizerId";
    
    /** Event organizer name field */
    public static final String ORGANIZER_NAME = "organizerName";
    
    /** Event poster URL field */
    public static final String POSTER_URL = "posterUrl";
    
    /** Event QR code URL field */
    public static final String QR_URL = "qrUrl";
    
    /** Event capacity field */
    public static final String CAPACITY = "capacity";
    
    /** Event waiting list limit field */
    public static final String WAITING_LIST_LIMIT = "waitingListLimit";
    
    /** Event geolocation required field */
    public static final String GEOLOCATION_REQUIRED = "geolocationRequired";
    
    /** Event price field */
    public static final String PRICE = "price";
    
    /** Event status field */
    public static final String STATUS = "status";
    
    // ==================== Registration Window Fields ====================
    
    /** Registration open timestamp field */
    public static final String REGISTRATION_OPEN = "registrationOpen";
    
    /** Registration close timestamp field */
    public static final String REGISTRATION_CLOSE = "registrationClose";
    
    /** Legacy registration start date field (backward compatibility) */
    public static final String REG_START = "regStart";
    
    /** Legacy registration end date field (backward compatibility) */
    public static final String REG_END = "regEnd";
    
    // ==================== Timestamp Fields ====================
    
    /** Created at timestamp field */
    public static final String CREATED_AT = "createdAt";
    
    /** Updated at timestamp field */
    public static final String UPDATED_AT = "updatedAt";
    
    /** Published at timestamp field */
    public static final String PUBLISHED_AT = "publishedAt";
    
    /** Cancelled at timestamp field */
    public static final String CANCELLED_AT = "cancelledAtUtc";
    
    /** Created at UTC timestamp field */
    public static final String CREATED_AT_UTC = "createdAtUtc";
    
    /** Joined date field */
    public static final String JOINED_DATE = "joinedDate";
    
    /** Sent at timestamp field */
    public static final String SENT_AT = "sentAt";
    
    // ==================== User Fields ====================
    
    /** User ID field */
    public static final String USER_ID = "userId";
    
    /** Entrant ID field */
    public static final String ENTRANT_ID = "entrantId";
    
    /** Entrant name field */
    public static final String ENTRANT_NAME = "entrantName";
    
    /** Device ID field */
    public static final String DEVICE_ID = "deviceId";
    
    /** User email field */
    public static final String EMAIL = "email";
    
    /** User phone number field */
    public static final String PHONE = "phone";
    
    /** User phone number field (alternate) */
    public static final String PHONE_NUMBER = "phoneNumber";
    
    /** User profile image URL field */
    public static final String PROFILE_IMAGE_URL = "profileImageUrl";
    
    /** FCM token field */
    public static final String FCM_TOKEN = "fcmToken";
    
    /** Notifications enabled field */
    public static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";
    
    /** User role field */
    public static final String ROLE = "role";
    
    /** User banned field */
    public static final String BANNED = "banned";
    
    /** User active field */
    public static final String ACTIVE = "active";
    
    // ==================== Location Fields ====================
    
    /** Latitude field */
    public static final String LATITUDE = "latitude";
    
    /** Longitude field */
    public static final String LONGITUDE = "longitude";
    
    // ==================== Invitation Fields ====================
    
    /** Invitation status field */
    public static final String INVITATION_STATUS = "status";
    
    // ==================== Notification Fields ====================
    
    /** Notification recipient user ID field */
    public static final String RECIPIENT_ID = "recipientId";
    
    /** Notification recipient user ID field (alternate) */
    public static final String RECIPIENT_USER_ID = "recipientUserId";
    
    /** Notification sender user ID field */
    public static final String SENDER_USER_ID = "senderUserId";
    
    /** Notification type field */
    public static final String TYPE = "type";
    
    /** Notification title field */
    public static final String NOTIFICATION_TITLE = "title";
    
    /** Notification message field */
    public static final String MESSAGE = "message";
    
    /** Notification read status field */
    public static final String IS_READ = "isRead";
}

