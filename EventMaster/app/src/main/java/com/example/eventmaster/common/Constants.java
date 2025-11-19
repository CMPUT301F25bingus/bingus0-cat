package com.example.eventmaster.common;

/**
 * Application-wide constants for status values, roles, and configuration.
 * 
 * Purpose:
 * - Eliminates magic strings throughout the codebase
 * - Provides single source of truth for status values
 * - Makes refactoring and updates easier
 * 
 * Usage:
 * import static com.example.eventmaster.common.Constants.*;
 * if (status.equals(STATUS_ACTIVE)) { ... }
 */
public final class Constants {
    
    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
    
    // ==================== Event Status ====================
    
    /** Event is in draft state, not yet published */
    public static final String STATUS_DRAFT = "DRAFT";
    
    /** Event is published and visible to entrants */
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    
    /** Event is closed, no longer accepting registrations */
    public static final String STATUS_CLOSED = "CLOSED";
    
    // ==================== Registration Status ====================
    
    /** Registration is active and confirmed */
    public static final String STATUS_ACTIVE = "ACTIVE";
    
    /** Registration was cancelled by the entrant */
    public static final String STATUS_CANCELLED_BY_ENTRANT = "CANCELLED_BY_ENTRANT";
    
    /** Registration was cancelled by the organizer */
    public static final String STATUS_CANCELLED_BY_ORGANIZER = "CANCELLED_BY_ORGANIZER";
    
    // ==================== Invitation Status ====================
    
    /** Invitation is pending entrant response */
    public static final String STATUS_PENDING = "PENDING";
    
    /** Invitation was accepted by entrant */
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    
    /** Invitation was declined by entrant */
    public static final String STATUS_DECLINED = "DECLINED";
    
    // ==================== Waiting List Status ====================
    
    /** Entrant is in waiting list */
    public static final String STATUS_WAITING = "waiting";
    
    /** Entrant was chosen in lottery */
    public static final String STATUS_CHOSEN = "chosen";
    
    /** Entrant was selected for event */
    public static final String STATUS_SELECTED = "selected";
    
    /** Entrant cancelled their waiting list entry */
    public static final String STATUS_CANCELLED = "cancelled";
    
    // ==================== Notification Types ====================
    
    /** Notification for lottery winner */
    public static final String NOTIF_LOTTERY_WON = "LOTTERY_WON";
    
    /** Notification for lottery loser */
    public static final String NOTIF_LOTTERY_LOST = "LOTTERY_NOT_SELECTED";
    
    /** Notification for invitation */
    public static final String NOTIF_INVITATION = "INVITATION";
    
    /** Notification for reminder */
    public static final String NOTIF_REMINDER = "REMINDER";
    
    /** Notification for cancellation */
    public static final String NOTIF_CANCELLATION = "CANCELLATION";
    
    /** General notification */
    public static final String NOTIF_GENERAL = "GENERAL";
    
    // ==================== User Roles ====================
    
    /** Admin role with full system access */
    public static final String ROLE_ADMIN = "admin";
    
    /** Organizer role for creating and managing events */
    public static final String ROLE_ORGANIZER = "organizer";
    
    /** Entrant role for joining events */
    public static final String ROLE_ENTRANT = "entrant";
    
    // ==================== Default Values ====================
    
    /** Default QR code size in pixels */
    public static final int DEFAULT_QR_SIZE = 512;
    
    /** Default event capacity (0 = unlimited) */
    public static final int DEFAULT_CAPACITY = 0;
    
    /** Default notification enabled state */
    public static final boolean DEFAULT_NOTIFICATIONS_ENABLED = true;
    
    /** Default user active state */
    public static final boolean DEFAULT_USER_ACTIVE = true;
    
    /** Default user banned state */
    public static final boolean DEFAULT_USER_BANNED = false;
}

