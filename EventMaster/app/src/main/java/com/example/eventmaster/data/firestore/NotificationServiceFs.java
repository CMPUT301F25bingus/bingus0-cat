package com.example.eventmaster.data.firestore;

import android.util.Log;

import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.model.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Firebase Firestore implementation of NotificationService.
 * Handles sending notifications and storing notification history in Firestore.
 * 
 * Note: This implementation stores notification records in Firestore.
 * For actual push notifications, Firebase Cloud Messaging (FCM) would be used
 * in conjunction with a backend server (not implemented in this client-only version).
 * 
 * Outstanding issues:
 * - FCM push notifications require a backend server to send via FCM API
 * - Current implementation only stores notification records in Firestore
 * - In-app notification display is handled by entrant UI reading from Firestore
 */
public class NotificationServiceFs implements NotificationService {

    private static final String TAG = "NotificationServiceFs";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    
    private final FirebaseFirestore firestore;

    /**
     * Creates a new NotificationServiceFs instance.
     */
    public NotificationServiceFs() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Constructor for dependency injection (useful for testing).
     * 
     * @param firestore FirebaseFirestore instance
     */
    public NotificationServiceFs(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void sendNotificationToSelectedEntrants(
            String eventId,
            List<Profile> selectedProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Sending lottery win notifications to " + selectedProfiles.size() + " entrants");
        
        sendNotificationsToProfiles(
                eventId,
                selectedProfiles,
                Notification.NotificationType.LOTTERY_WON,
                title,
                message,
                onSuccess,
                onFailure
        );
    }

    @Override
    public void sendNotificationToNotSelectedEntrants(
            String eventId,
            List<Profile> notSelectedProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Sending lottery loss notifications to " + notSelectedProfiles.size() + " entrants");
        
        sendNotificationsToProfiles(
                eventId,
                notSelectedProfiles,
                Notification.NotificationType.LOTTERY_LOST,
                title,
                message,
                onSuccess,
                onFailure
        );
    }

    @Override
    public void sendNotificationToWaitingList(
            String eventId,
            List<Profile> waitingListProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Sending notification to waiting list: " + waitingListProfiles.size() + " entrants");
        
        sendNotificationsToProfiles(
                eventId,
                waitingListProfiles,
                Notification.NotificationType.GENERAL,
                title,
                message,
                onSuccess,
                onFailure
        );
    }

    @Override
    public void sendNotificationToCancelledEntrants(
            String eventId,
            List<Profile> cancelledProfiles,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Sending cancellation notifications to " + cancelledProfiles.size() + " entrants");
        
        sendNotificationsToProfiles(
                eventId,
                cancelledProfiles,
                Notification.NotificationType.CANCELLATION,
                title,
                message,
                onSuccess,
                onFailure
        );
    }

    /**
     * Helper method to send notifications to a list of profiles.
     * Creates notification records in Firestore for each recipient.
     * 
     * @param eventId Event ID
     * @param profiles List of profiles to notify
     * @param type Notification type
     * @param title Notification title
     * @param message Notification message
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    private void sendNotificationsToProfiles(
            String eventId,
            List<Profile> profiles,
            Notification.NotificationType type,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        if (profiles == null || profiles.isEmpty()) {
            Log.w(TAG, "No profiles to send notifications to");
            if (onSuccess != null) {
                onSuccess.onSuccess();
            }
            return;
        }

        // Filter out users who have opted out of notifications
        List<Profile> eligibleProfiles = new ArrayList<>();
        for (Profile profile : profiles) {
            if (profile.isNotificationsEnabled()) {
                eligibleProfiles.add(profile);
            } else {
                Log.d(TAG, "Skipping notification for user " + profile.getUserId() + " (opted out)");
            }
        }

        if (eligibleProfiles.isEmpty()) {
            Log.w(TAG, "No eligible profiles after filtering opt-outs");
            if (onSuccess != null) {
                onSuccess.onSuccess();
            }
            return;
        }

        // Track completion of all notification writes
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        int totalCount = eligibleProfiles.size();

        for (Profile profile : eligibleProfiles) {
            createNotificationRecord(eventId, profile.getUserId(), type, title, message,
                    () -> {
                        int completed = successCount.incrementAndGet() + failureCount.get();
                        Log.d(TAG, "Notification sent successfully to " + profile.getUserId() + 
                                " (" + completed + "/" + totalCount + ")");
                        
                        if (completed == totalCount) {
                            handleBatchCompletion(successCount.get(), failureCount.get(), 
                                    totalCount, onSuccess, onFailure);
                        }
                    },
                    error -> {
                        int completed = successCount.get() + failureCount.incrementAndGet();
                        Log.e(TAG, "Failed to send notification to " + profile.getUserId() + 
                                ": " + error + " (" + completed + "/" + totalCount + ")");
                        
                        if (completed == totalCount) {
                            handleBatchCompletion(successCount.get(), failureCount.get(), 
                                    totalCount, onSuccess, onFailure);
                        }
                    }
            );
        }
    }

    /**
     * Handles completion of batch notification sending.
     * 
     * @param successCount Number of successful notifications
     * @param failureCount Number of failed notifications
     * @param totalCount Total notifications attempted
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    private void handleBatchCompletion(
            int successCount,
            int failureCount,
            int totalCount,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        if (failureCount == 0) {
            Log.i(TAG, "All " + totalCount + " notifications sent successfully");
            if (onSuccess != null) {
                onSuccess.onSuccess();
            }
        } else if (successCount > 0) {
            String message = "Sent " + successCount + " of " + totalCount + 
                    " notifications (" + failureCount + " failed)";
            Log.w(TAG, message);
            if (onSuccess != null) {
                onSuccess.onSuccess(); // Partial success still counts
            }
        } else {
            String error = "Failed to send all " + totalCount + " notifications";
            Log.e(TAG, error);
            if (onFailure != null) {
                onFailure.onFailure(error);
            }
        }
    }

    /**
     * Creates a notification record in Firestore.
     * 
     * @param eventId Event ID
     * @param recipientUserId Recipient user ID
     * @param type Notification type
     * @param title Notification title
     * @param message Notification message
     * @param onSuccess Success callback
     * @param onFailure Failure callback
     */
    private void createNotificationRecord(
            String eventId,
            String recipientUserId,
            Notification.NotificationType type,
            String title,
            String message,
            OnSuccessListener onSuccess,
            OnFailureListener onFailure) {
        
        Notification notification = new Notification(
                eventId,
                recipientUserId,
                "system", // Sender ID (organizer ID should be passed in production)
                type,
                title,
                message
        );

        Map<String, Object> notificationData = createNotificationData(notification);

        firestore.collection(COLLECTION_NOTIFICATIONS)
                .add(notificationData)
                .addOnSuccessListener(documentReference -> {
                    String notificationId = documentReference.getId();
                    Log.d(TAG, "Notification created with ID: " + notificationId);
                    
                    // Update the document with its own ID
                    documentReference.update("notificationId", notificationId)
                            .addOnSuccessListener(aVoid -> {
                                if (onSuccess != null) {
                                    onSuccess.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update notification ID", e);
                                if (onFailure != null) {
                                    onFailure.onFailure(e.getMessage());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create notification", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Converts a Notification object to a Firestore-compatible map.
     * 
     * @param notification The notification to convert
     * @return Map of notification data
     */
    private Map<String, Object> createNotificationData(Notification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", notification.getEventId());
        data.put("recipientUserId", notification.getRecipientUserId());
        data.put("senderUserId", notification.getSenderUserId());
        data.put("type", notification.getType().name());
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("sentAt", notification.getSentAt());
        data.put("isRead", notification.isRead());
        return data;
    }

    @Override
    public void getNotificationHistoryForEvent(
            String eventId,
            OnNotificationHistoryListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Fetching notification history for event: " + eventId);
        
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("eventId", eventId)
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notifications.add(notification);
                    });
                    Log.d(TAG, "Retrieved " + notifications.size() + " notifications for event");
                    if (onSuccess != null) {
                        onSuccess.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch notification history", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }

    @Override
    public void getNotificationsForUser(
            String userId,
            OnNotificationHistoryListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Fetching notifications for user: " + userId);
        
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientUserId", userId)
                .orderBy("sentAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Notification> notifications = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> {
                        Notification notification = doc.toObject(Notification.class);
                        notifications.add(notification);
                    });
                    Log.d(TAG, "Retrieved " + notifications.size() + " notifications for user");
                    if (onSuccess != null) {
                        onSuccess.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch user notifications", e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }

    @Override
    public void markNotificationAsRead(String notificationId) {
        Log.d(TAG, "Marking notification as read: " + notificationId);
        
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> 
                        Log.d(TAG, "Notification marked as read successfully"))
                .addOnFailureListener(e -> 
                        Log.e(TAG, "Failed to mark notification as read", e));
    }
}

