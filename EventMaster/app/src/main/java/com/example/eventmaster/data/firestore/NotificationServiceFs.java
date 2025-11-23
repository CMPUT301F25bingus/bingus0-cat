package com.example.eventmaster.data.firestore;

import android.util.Log;

import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.model.Profile;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.android.gms.tasks.Tasks;

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
            String recipientUserId = profile.getUserId();
            if (recipientUserId == null || recipientUserId.isEmpty()) {
                Log.w(TAG, "Profile " + profile.getName() + " has null/empty userId, skipping notification");
                int completed = successCount.get() + failureCount.incrementAndGet();
                if (completed == totalCount) {
                    handleBatchCompletion(successCount.get(), failureCount.get(), totalCount, onSuccess, onFailure);
                }
                continue;
            }
            Log.d(TAG, "Creating notification for profile: " + profile.getName() + 
                    ", userId: " + recipientUserId + ", type: " + type + ", eventId: " + eventId);
            createNotificationRecord(eventId, recipientUserId, type, title, message,
                    () -> {
                        int completed = successCount.incrementAndGet() + failureCount.get();
                        Log.d(TAG, "Notification sent successfully to " + recipientUserId + 
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
        
        Log.d(TAG, "createNotificationRecord: eventId=" + eventId + 
                ", recipientUserId=" + recipientUserId + ", type=" + type);
        
        Notification notification = new Notification(
                eventId,
                recipientUserId,
                "system", // Sender ID (organizer ID should be passed in production)
                type,
                title,
                message
        );
        
        Log.d(TAG, "Notification object created: recipientUserId=" + notification.getRecipientUserId());

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
        // Also add legacy recipientId field for backward compatibility
        if (notification.getRecipientUserId() != null) {
            data.put("recipientId", notification.getRecipientUserId());
        }
        data.put("senderUserId", notification.getSenderUserId());
        data.put("type", notification.getType().name());
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("sentAt", notification.getSentAt());
        data.put("isRead", notification.isRead());
        
        Log.d(TAG, "createNotificationData: recipientUserId=" + notification.getRecipientUserId() + 
                ", eventId=" + notification.getEventId() + ", type=" + notification.getType() +
                ", title=" + notification.getTitle());
        
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
                    List<Notification> notifications = parseNotifications(queryDocumentSnapshots);
                    Log.d(TAG, "Retrieved " + notifications.size() + " notifications for event");
                    if (onSuccess != null) {
                        onSuccess.onSuccess(notifications);
                    }
                })
                .addOnFailureListener(e -> {
                    // If orderBy fails, try without orderBy
                    Log.w(TAG, "Query with orderBy failed, trying without orderBy: " + e.getMessage());
                    firestore.collection(COLLECTION_NOTIFICATIONS)
                            .whereEqualTo("eventId", eventId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                List<Notification> notifications = parseNotifications(queryDocumentSnapshots);
                                // Sort manually by sentAt
                                notifications.sort((a, b) -> {
                                    Date dateA = a.getSentAt();
                                    Date dateB = b.getSentAt();
                                    if (dateA == null && dateB == null) return 0;
                                    if (dateA == null) return 1;
                                    if (dateB == null) return -1;
                                    return dateB.compareTo(dateA); // Descending order
                                });
                                Log.d(TAG, "Retrieved " + notifications.size() + " notifications for event (without orderBy)");
                                if (onSuccess != null) {
                                    onSuccess.onSuccess(notifications);
                                }
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Failed to fetch notification history", e2);
                                if (onFailure != null) {
                                    onFailure.onFailure(e2.getMessage());
                                }
                            });
                });
    }

    @Override
    public void getNotificationsForUser(
            String userId,
            OnNotificationHistoryListener onSuccess,
            OnFailureListener onFailure) {
        
        Log.d(TAG, "Fetching notifications for user: " + userId);
        
        // Query for notifications with recipientUserId OR legacy recipientId
        // Firestore doesn't support OR queries directly, so we need to query both and merge
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task1 = 
                firestore.collection(COLLECTION_NOTIFICATIONS)
                        .whereEqualTo("recipientUserId", userId)
                        .get();
        
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task2 = 
                firestore.collection(COLLECTION_NOTIFICATIONS)
                        .whereEqualTo("recipientId", userId) // Legacy field
                        .get();
        
        Tasks.whenAllComplete(task1, task2)
                .addOnSuccessListener(taskList -> {
                    List<Notification> allNotifications = new ArrayList<>();
                    Set<String> seenIds = new HashSet<>(); // Avoid duplicates
                    
                    // Process recipientUserId results
                    if (task1.isSuccessful() && task1.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task1.getResult().getDocuments()) {
                            if (!seenIds.contains(doc.getId())) {
                                seenIds.add(doc.getId());
                                Notification notif = parseNotification(doc);
                                if (notif != null) {
                                    allNotifications.add(notif);
                                }
                            }
                        }
                        Log.d(TAG, "Found " + task1.getResult().size() + " notifications with recipientUserId=" + userId);
                    }
                    
                    // Process legacy recipientId results
                    if (task2.isSuccessful() && task2.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task2.getResult().getDocuments()) {
                            if (!seenIds.contains(doc.getId())) {
                                seenIds.add(doc.getId());
                                Notification notif = parseNotification(doc);
                                if (notif != null) {
                                    allNotifications.add(notif);
                                }
                            }
                        }
                        Log.d(TAG, "Found " + task2.getResult().size() + " notifications with recipientId=" + userId);
                    }
                    
                    // Sort by sentAt descending
                    allNotifications.sort((a, b) -> {
                        Date dateA = a.getSentAt();
                        Date dateB = b.getSentAt();
                        if (dateA == null && dateB == null) return 0;
                        if (dateA == null) return 1;
                        if (dateB == null) return -1;
                        return dateB.compareTo(dateA); // Descending order
                    });
                    
                    Log.d(TAG, "Retrieved " + allNotifications.size() + " total notifications for user: " + userId);
                    if (onSuccess != null) {
                        onSuccess.onSuccess(allNotifications);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch notifications for userId: " + userId, e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }
    
    /**
     * Parses a single Firestore document into a Notification object.
     */
    private Notification parseNotification(com.google.firebase.firestore.DocumentSnapshot doc) {
        try {
            Notification notification = doc.toObject(Notification.class);
            if (notification == null) {
                return null;
            }
            
            // Ensure notificationId is set
            if (notification.getNotificationId() == null || notification.getNotificationId().isEmpty()) {
                notification.setNotificationId(doc.getId());
            }
            
            // Handle legacy field names (recipientId -> recipientUserId)
            if (notification.getRecipientUserId() == null || notification.getRecipientUserId().isEmpty()) {
                String legacyRecipientId = doc.getString("recipientId");
                if (legacyRecipientId != null && !legacyRecipientId.isEmpty()) {
                    Log.d(TAG, "Found legacy recipientId field: " + legacyRecipientId + " for notification: " + doc.getId());
                    notification.setRecipientUserId(legacyRecipientId);
                }
            }
            
            // Handle sentAt field - convert Timestamp to Date if needed
            if (notification.getSentAt() == null) {
                com.google.firebase.Timestamp timestamp = doc.getTimestamp("sentAt");
                if (timestamp != null) {
                    notification.setSentAt(timestamp.toDate());
                } else {
                    // Try createdAt as fallback
                    timestamp = doc.getTimestamp("createdAt");
                    if (timestamp != null) {
                        notification.setSentAt(timestamp.toDate());
                    }
                }
            }
            
            return notification;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
            return null;
        }
    }

    /**
     * Parses Firestore documents into Notification objects, handling field name variations.
     */
    private List<Notification> parseNotifications(com.google.firebase.firestore.QuerySnapshot queryDocumentSnapshots) {
        List<Notification> notifications = new ArrayList<>();
        queryDocumentSnapshots.forEach(doc -> {
            try {
                Notification notification = doc.toObject(Notification.class);
                
                // Ensure notificationId is set
                if (notification.getNotificationId() == null || notification.getNotificationId().isEmpty()) {
                    notification.setNotificationId(doc.getId());
                }
                
                // Handle legacy field names (recipientId -> recipientUserId)
                if (notification.getRecipientUserId() == null || notification.getRecipientUserId().isEmpty()) {
                    String legacyRecipientId = doc.getString("recipientId");
                    if (legacyRecipientId != null && !legacyRecipientId.isEmpty()) {
                        Log.d(TAG, "Found legacy recipientId field: " + legacyRecipientId + " for notification: " + doc.getId());
                        notification.setRecipientUserId(legacyRecipientId);
                    }
                }
                
                Log.d(TAG, "Parsed notification: id=" + notification.getNotificationId() + 
                        ", recipientUserId=" + notification.getRecipientUserId() + 
                        ", type=" + notification.getType() + 
                        ", title=" + notification.getTitle());
                
                // Handle sentAt field - convert Timestamp to Date if needed
                if (notification.getSentAt() == null) {
                    com.google.firebase.Timestamp timestamp = doc.getTimestamp("sentAt");
                    if (timestamp != null) {
                        notification.setSentAt(timestamp.toDate());
                    } else {
                        // Try createdAt as fallback
                        timestamp = doc.getTimestamp("createdAt");
                        if (timestamp != null) {
                            notification.setSentAt(timestamp.toDate());
                        } else {
                            notification.setSentAt(new java.util.Date());
                        }
                    }
                }
                
                notifications.add(notification);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse notification document: " + doc.getId(), e);
            }
        });
        return notifications;
    }

    @Override
    public void deleteNotification(String notificationId,
                                   OnSuccessListener onSuccess,
                                   OnFailureListener onFailure) {
        Log.d(TAG, "Deleting notification: " + notificationId);
        firestore.collection(COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted successfully: " + notificationId);
                    if (onSuccess != null) {
                        onSuccess.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete notification: " + notificationId, e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }

    @Override
    public void deleteAllNotificationsForUser(String userId,
                                                OnSuccessListener onSuccess,
                                                OnFailureListener onFailure) {
        Log.d(TAG, "Deleting all notifications for user: " + userId);
        
        // Query for all notifications for this user (both recipientUserId and legacy recipientId)
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task1 = 
                firestore.collection(COLLECTION_NOTIFICATIONS)
                        .whereEqualTo("recipientUserId", userId)
                        .get();
        
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task2 = 
                firestore.collection(COLLECTION_NOTIFICATIONS)
                        .whereEqualTo("recipientId", userId)
                        .get();
        
        Tasks.whenAllComplete(task1, task2)
                .addOnSuccessListener(taskList -> {
                    Set<String> notificationIds = new HashSet<>();
                    
                    // Collect all notification IDs from both queries
                    int count1 = 0;
                    if (task1.isSuccessful() && task1.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task1.getResult().getDocuments()) {
                            notificationIds.add(doc.getId());
                            count1++;
                        }
                        Log.d(TAG, "Found " + count1 + " notifications with recipientUserId=" + userId);
                    } else if (task1.getException() != null) {
                        Log.w(TAG, "Query 1 failed (recipientUserId): " + task1.getException().getMessage());
                    }
                    
                    int count2 = 0;
                    if (task2.isSuccessful() && task2.getResult() != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : task2.getResult().getDocuments()) {
                            notificationIds.add(doc.getId());
                            count2++;
                        }
                        Log.d(TAG, "Found " + count2 + " notifications with recipientId=" + userId);
                    } else if (task2.getException() != null) {
                        Log.w(TAG, "Query 2 failed (recipientId): " + task2.getException().getMessage());
                    }
                    
                    Log.d(TAG, "Total unique notifications to delete: " + notificationIds.size() + " for user: " + userId);
                    
                    if (notificationIds.isEmpty()) {
                        Log.d(TAG, "No notifications to delete for user: " + userId);
                        if (onSuccess != null) {
                            onSuccess.onSuccess();
                        }
                        return;
                    }
                    
                    // Firestore batch operations are limited to 500 operations
                    // Split into batches if needed
                    List<String> idList = new ArrayList<>(notificationIds);
                    int batchSize = 500;
                    int batchCount = (idList.size() + batchSize - 1) / batchSize;
                    
                    Log.d(TAG, "Deleting " + idList.size() + " notifications in " + batchCount + " batch(es)");
                    
                    deleteNotificationsInBatches(idList, 0, batchCount, batchSize, userId, onSuccess, onFailure);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query notifications for deletion: " + userId, e);
                    if (onFailure != null) {
                        onFailure.onFailure(e.getMessage());
                    }
                });
    }
    
    /**
     * Recursively deletes notifications in batches to handle Firestore's 500 operation limit.
     */
    private void deleteNotificationsInBatches(List<String> notificationIds, int currentBatch, int totalBatches,
                                              int batchSize, String userId,
                                              OnSuccessListener onSuccess, OnFailureListener onFailure) {
        int start = currentBatch * batchSize;
        int end = Math.min(start + batchSize, notificationIds.size());
        
        if (start >= notificationIds.size()) {
            // All batches complete
            Log.d(TAG, "Successfully deleted all " + notificationIds.size() + " notifications for user: " + userId);
            if (onSuccess != null) {
                onSuccess.onSuccess();
            }
            return;
        }
        
        List<String> batchIds = notificationIds.subList(start, end);
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        
        for (String notificationId : batchIds) {
            batch.delete(firestore.collection(COLLECTION_NOTIFICATIONS).document(notificationId));
        }
        
        Log.d(TAG, "Deleting batch " + (currentBatch + 1) + "/" + totalBatches + 
                " (" + batchIds.size() + " notifications)");
        
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch " + (currentBatch + 1) + "/" + totalBatches + " deleted successfully");
                    // Process next batch
                    deleteNotificationsInBatches(notificationIds, currentBatch + 1, totalBatches, 
                            batchSize, userId, onSuccess, onFailure);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete batch " + (currentBatch + 1) + "/" + totalBatches + 
                            " for user: " + userId, e);
                    if (onFailure != null) {
                        onFailure.onFailure("Failed to delete batch " + (currentBatch + 1) + ": " + e.getMessage());
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

