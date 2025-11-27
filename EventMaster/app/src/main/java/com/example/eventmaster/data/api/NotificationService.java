package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Notification;
import com.example.eventmaster.model.Profile;

import java.util.List;

/**
 * Service interface for managing notifications in the EventMaster application.
 * Provides methods for sending notifications to entrants and tracking notification history.
 * 
 * Implementations should handle Firebase Cloud Messaging integration
 * and notification persistence.
 */
public interface NotificationService {

    /**
     * Sends a notification to selected entrants who won the lottery.
     * Used by organizers to invite winners to sign up for events.
     * 
     * @param eventId The event ID
     * @param selectedProfiles List of profiles who were selected
     * @param title Notification title
     * @param message Notification message body
     * @param onSuccess Callback for successful send
     * @param onFailure Callback for failure with error message
     */
    void sendNotificationToSelectedEntrants(
            String eventId,
            List<Profile> selectedProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Sends a notification to entrants who were not selected in the lottery.
     * 
     * @param eventId The event ID
     * @param notSelectedProfiles List of profiles who were not selected
     * @param title Notification title
     * @param message Notification message body
     * @param onSuccess Callback for successful send
     * @param onFailure Callback for failure with error message
     */
    void sendNotificationToNotSelectedEntrants(
            String eventId,
            List<Profile> notSelectedProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Sends a notification to all entrants on the waiting list.
     * 
     * @param eventId The event ID
     * @param waitingListProfiles List of all profiles on waiting list
     * @param title Notification title
     * @param message Notification message body
     * @param onSuccess Callback for successful send
     * @param onFailure Callback for failure with error message
     */
    void sendNotificationToWaitingList(
            String eventId,
            List<Profile> waitingListProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Sends a notification to cancelled entrants.
     * 
     * @param eventId The event ID
     * @param cancelledProfiles List of cancelled profiles
     * @param title Notification title
     * @param message Notification message body
     * @param onSuccess Callback for successful send
     * @param onFailure Callback for failure with error message
     */
    void sendNotificationToCancelledEntrants(
            String eventId,
            List<Profile> cancelledProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Retrieves notification history for a specific event (for admin).
     * 
     * @param eventId The event ID
     * @param onSuccess Callback with list of notifications
     * @param onFailure Callback for failure with error message
     */
    void getNotificationHistoryForEvent(
            String eventId,
            OnNotificationHistoryListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Retrieves notifications for a specific user.
     * 
     * @param userId The user ID
     * @param onSuccess Callback with list of notifications
     * @param onFailure Callback for failure with error message
     */
    void getNotificationsForUser(
            String userId,
            OnNotificationHistoryListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Marks a notification as read.
     * 
     * @param notificationId The notification ID
     */
    void markNotificationAsRead(String notificationId);

    /**
     * Deletes a notification from Firestore.
     * 
     * @param notificationId The notification ID
     * @param onSuccess Callback for successful deletion
     * @param onFailure Callback for failure with error message
     */
    void deleteNotification(
            String notificationId,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    /**
     * Deletes all notifications for a specific user.
     * 
     * @param userId The user ID
     * @param onSuccess Callback for successful deletion
     * @param onFailure Callback for failure with error message
     */
    void deleteAllNotificationsForUser(
            String userId,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure
    );

    // Callback interfaces

    interface OnSuccessListener {
        void onSuccess();
    }

    interface OnFailureListener {
        void onFailure(String error);
    }

    interface OnNotificationHistoryListener {
        void onSuccess(List<Notification> notifications);
    }
}

