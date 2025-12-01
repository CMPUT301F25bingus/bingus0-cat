package com.example.eventmaster.common;

/**
 * Centralized Firestore collection and document paths.
 * 
 * Purpose:
 * - Single source of truth for all Firestore paths
 * - Reduces typos and inconsistencies
 * - Makes path changes easier to manage
 * 
 * Usage:
 * import static com.example.eventmaster.common.FirestorePaths.*;
 * db.collection(EVENTS).document(eventId).collection(WAITING_LIST)
 * 
 * Or use path builders:
 * String path = waitingListPath(eventId);
 */
public final class FirestorePaths {
    
    // Private constructor to prevent instantiation
    private FirestorePaths() {
        throw new AssertionError("Cannot instantiate FirestorePaths class");
    }
    
    // ==================== Top-Level Collections ====================
    
    /** Events collection */
    public static final String EVENTS = "events";
    
    /** User profiles collection */
    public static final String PROFILES = "profiles";
    
    /** Notifications collection */
    public static final String NOTIFICATIONS = "notifications";
    
    /** Organizer applications collection */
    public static final String ORGANIZER_APPLICATIONS = "organizerApplications";
    
    // ==================== Event Sub-Collections ====================
    
    /** Waiting list sub-collection under events */
    public static final String WAITING_LIST = "waiting_list";
    
    /** Chosen list sub-collection under events */
    public static final String CHOSEN_LIST = "chosen_list";
    
    /** Invitations sub-collection under events */
    public static final String INVITATIONS = "invitations";
    
    /** Registrations sub-collection under events */
    public static final String REGISTRATIONS = "registrations";
    
    // ==================== Path Builder Methods ====================
    
    /**
     * Builds path to a specific event document.
     * 
     * @param eventId the event document ID
     * @return path string: "events/{eventId}"
     */
    public static String eventPath(String eventId) {
        return EVENTS + "/" + eventId;
    }
    
    /**
     * Builds path to an event's waiting list collection.
     * 
     * @param eventId the event document ID
     * @return path string: "events/{eventId}/waiting_list"
     */
    public static String waitingListPath(String eventId) {
        return eventPath(eventId) + "/" + WAITING_LIST;
    }
    
    /**
     * Builds path to an event's chosen list collection.
     * 
     * @param eventId the event document ID
     * @return path string: "events/{eventId}/chosen_list"
     */
    public static String chosenListPath(String eventId) {
        return eventPath(eventId) + "/" + CHOSEN_LIST;
    }
    
    /**
     * Builds path to an event's invitations collection.
     * 
     * @param eventId the event document ID
     * @return path string: "events/{eventId}/invitations"
     */
    public static String invitationsPath(String eventId) {
        return eventPath(eventId) + "/" + INVITATIONS;
    }
    
    /**
     * Builds path to an event's registrations collection.
     * 
     * @param eventId the event document ID
     * @return path string: "events/{eventId}/registrations"
     */
    public static String registrationsPath(String eventId) {
        return eventPath(eventId) + "/" + REGISTRATIONS;
    }
    
    /**
     * Builds path to a specific user profile document.
     * 
     * @param userId the user document ID
     * @return path string: "profiles/{userId}"
     */
    public static String profilePath(String userId) {
        return PROFILES + "/" + userId;
    }
    
    /**
     * Builds path to a specific waiting list entry.
     * 
     * @param eventId the event document ID
     * @param userId the user document ID
     * @return path string: "events/{eventId}/waiting_list/{userId}"
     */
    public static String waitingListEntryPath(String eventId, String userId) {
        return waitingListPath(eventId) + "/" + userId;
    }
    
    /**
     * Builds path to a specific chosen list entry.
     * 
     * @param eventId the event document ID
     * @param userId the user document ID
     * @return path string: "events/{eventId}/chosen_list/{userId}"
     */
    public static String chosenListEntryPath(String eventId, String userId) {
        return chosenListPath(eventId) + "/" + userId;
    }
    
    /**
     * Builds path to a specific invitation document.
     * 
     * @param eventId the event document ID
     * @param userId the user document ID
     * @return path string: "events/{eventId}/invitations/{userId}"
     */
    public static String invitationPath(String eventId, String userId) {
        return invitationsPath(eventId) + "/" + userId;
    }
    
    /**
     * Builds path to a specific registration document.
     * 
     * @param eventId the event document ID
     * @param userId the user document ID
     * @return path string: "events/{eventId}/registrations/{userId}"
     */
    public static String registrationPath(String eventId, String userId) {
        return registrationsPath(eventId) + "/" + userId;
    }
}

