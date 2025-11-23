package com.example.eventmaster.ui.entrant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.ui.entrant.adapters.NotificationsAdapter;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Activity for displaying entrant notifications inbox.
 * Shows all notifications received by the entrant including lottery results.
 * 
 * Implements:
 * - US 01.04.01: Receive notification when selected (lottery won)
 * - US 01.04.02: Receive notification when not selected (lottery lost)
 * 
 * Outstanding issues:
 * - User ID is currently mocked, should come from authenticated user session
 * - Delete all functionality is stubbed
 * - Individual notification actions (accept/decline) will be added in future stories
 */
public class EntrantNotificationsActivity extends AppCompatActivity {

    private static final String TAG = "EntrantNotifications";

    // UI Components
    private ImageView backButton;
    private ImageView deleteAllButton;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar loadingIndicator;

    // Data
    private List<Notification> notifications;
    private NotificationsAdapter adapter;

    // Services
    private NotificationService notificationService;
    private EventRepository eventRepository;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_notifications);

        // Initialize service
        notificationService = new NotificationServiceFs();
        eventRepository = new EventRepositoryFs();
        currentUserId = resolveCurrentUserId();

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Load notifications
        loadNotifications();
    }

    /**
     * Initializes all view components.
     */
    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        deleteAllButton = findViewById(R.id.delete_all_button);
        recyclerView = findViewById(R.id.notifications_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Set item click listener
        adapter.setOnNotificationClickListener(notification -> {
            handleNotificationClick(notification);
        });
    }

    /**
     * Sets up click listeners for interactive components.
     */
    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Delete all button
        deleteAllButton.setOnClickListener(v -> handleDeleteAllClick());
    }

    /**
     * Loads notifications for the current user from Firestore.
     * Queries with both Firebase Auth UID and deviceId to ensure all notifications are found.
     */
    private void loadNotifications() {
        showLoading(true);
        String deviceId = DeviceUtils.getDeviceId(this);
        Log.d(TAG, "Loading notifications for user: " + currentUserId + " (deviceId: " + deviceId + ")");

        // Query with Firebase Auth UID first
        notificationService.getNotificationsForUser(
                currentUserId,
                notifications1 -> {
                    // Also query with deviceId to catch legacy notifications
                    if (deviceId != null && !deviceId.equals(currentUserId)) {
                        notificationService.getNotificationsForUser(
                                deviceId,
                                notifications2 -> {
                                    // Merge and deduplicate
                                    Set<String> seenIds = new HashSet<>();
                                    List<Notification> allNotifications = new ArrayList<>();
                                    
                                    if (notifications1 != null) {
                                        for (Notification n : notifications1) {
                                            if (n.getNotificationId() != null && !seenIds.contains(n.getNotificationId())) {
                                                seenIds.add(n.getNotificationId());
                                                allNotifications.add(n);
                                            }
                                        }
                                    }
                                    
                                    if (notifications2 != null) {
                                        for (Notification n : notifications2) {
                                            if (n.getNotificationId() != null && !seenIds.contains(n.getNotificationId())) {
                                                seenIds.add(n.getNotificationId());
                                                allNotifications.add(n);
                                            }
                                        }
                                    }
                                    
                                    // Sort by sentAt descending
                                    allNotifications.sort((a, b) -> {
                                        Date dateA = a.getSentAt();
                                        Date dateB = b.getSentAt();
                                        if (dateA == null && dateB == null) return 0;
                                        if (dateA == null) return 1;
                                        if (dateB == null) return -1;
                                        return dateB.compareTo(dateA);
                                    });
                                    
                                    handleNotificationsFetched(allNotifications);
                                },
                                error -> {
                                    Log.e(TAG, "Failed to load notifications with deviceId: " + error);
                                    // Still process notifications from Firebase Auth UID
                                    handleNotificationsFetched(notifications1 != null ? notifications1 : new ArrayList<>());
                                }
                        );
                    } else {
                        // deviceId same as currentUserId, just use one query
                        handleNotificationsFetched(notifications1 != null ? notifications1 : new ArrayList<>());
                    }
                },
                this::handleLoadError
        );
    }

    private void handleNotificationsFetched(List<Notification> loadedNotifications) {
        if (loadedNotifications == null || loadedNotifications.isEmpty()) {
            Log.d(TAG, "No notifications found for user: " + currentUserId);
            showLoading(false);
            handleNotificationsLoaded(new ArrayList<>());
            return;
        }

        hydrateNotificationsWithEvents(loadedNotifications);
    }

    private void hydrateNotificationsWithEvents(@NonNull List<Notification> loadedNotifications) {
        Set<String> eventIds = new HashSet<>();
        for (Notification notification : loadedNotifications) {
            if (notification.getEventId() != null && !notification.getEventId().trim().isEmpty()) {
                eventIds.add(notification.getEventId());
            }
        }

        if (eventIds.isEmpty()) {
            showLoading(false);
            handleNotificationsLoaded(loadedNotifications);
            return;
        }

        Map<String, Task<Event>> tasksByEventId = new HashMap<>();
        List<Task<Event>> tasks = new ArrayList<>();
        for (String eventId : eventIds) {
            Task<Event> task = eventRepository.getEventById(eventId);
            tasksByEventId.put(eventId, task);
            tasks.add(task);
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(result -> {
                    Map<String, String> eventNames = new HashMap<>();
                    for (Map.Entry<String, Task<Event>> entry : tasksByEventId.entrySet()) {
                        Task<Event> task = entry.getValue();
                        if (task.isSuccessful() && task.getResult() != null) {
                            Event event = task.getResult();
                            String name = firstNonEmpty(event.getName(), event.getTitle());
                            eventNames.put(entry.getKey(), name != null ? name : entry.getKey());
                        } else {
                            Log.w(TAG, "Failed to resolve event for id=" + entry.getKey(), task.getException());
                        }
                    }

                    decorateNotificationsWithEventNames(loadedNotifications, eventNames);
                    showLoading(false);
                    handleNotificationsLoaded(loadedNotifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event details for notifications", e);
                    showLoading(false);
                    handleNotificationsLoaded(loadedNotifications);
                });
    }

    private void decorateNotificationsWithEventNames(@NonNull List<Notification> loadedNotifications,
                                                     @NonNull Map<String, String> eventNames) {
        for (Notification notification : loadedNotifications) {
            String eventId = notification.getEventId();
            if (eventId == null) {
                continue;
            }
            String eventName = eventNames.get(eventId);
            if (eventName == null || eventName.trim().isEmpty()) {
                continue;
            }

            if (notification.getTitle() == null || notification.getTitle().trim().isEmpty()) {
                notification.setTitle(eventName + " update");
            }

            if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
                notification.setMessage("Latest update for " + eventName);
            } else if (!notification.getMessage().toLowerCase(Locale.getDefault())
                    .contains(eventName.toLowerCase(Locale.getDefault()))) {
                notification.setMessage(eventName + ": " + notification.getMessage());
            }
        }
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Handles successful loading of notifications.
     * 
     * @param loadedNotifications List of notifications from Firestore
     */
    private void handleNotificationsLoaded(List<Notification> loadedNotifications) {
        Log.d(TAG, "Loaded " + loadedNotifications.size() + " notifications");
        
        this.notifications = loadedNotifications;
        adapter.updateNotifications(loadedNotifications);

        // Show/hide empty state
        if (loadedNotifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    /**
     * Handles errors when loading notifications.
     * 
     * @param error Error message
     */
    private void handleLoadError(String error) {
        Log.e(TAG, "Failed to load notifications: " + error);
        showLoading(false);
        Toast.makeText(this, "Failed to load notifications: " + error,
                Toast.LENGTH_LONG).show();
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Handles click on a notification item.
     * 
     * @param notification The clicked notification
     */
    private void handleNotificationClick(Notification notification) {
        Log.d(TAG, "Notification clicked: " + notification.getTitle());
        
        // Mark as read
        if (!notification.isRead()) {
            notificationService.markNotificationAsRead(notification.getNotificationId());
            notification.setRead(true);
        }

        // Show notification details
        showNotificationDetails(notification);
    }

    /**
     * Shows detailed view of a notification.
     * 
     * @param notification The notification to show
     */
    private void showNotificationDetails(Notification notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(notification.getTitle());
        builder.setMessage(notification.getMessage());
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        
        // For lottery win notifications, add button to navigate to event details
        if (notification.getType() == Notification.NotificationType.LOTTERY_WON && notification.getEventId() != null) {
            builder.setNeutralButton("View Event", (dialog, which) -> {
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, notification.getEventId());
                startActivity(intent);
            });
        }
        
        builder.show();
    }

    /**
     * Handles delete all button click.
     */
    private void handleDeleteAllClick() {
        if (notifications.isEmpty()) {
            Toast.makeText(this, "No notifications to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete All Notifications");
        builder.setMessage("Are you sure you want to delete all notifications? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteAllNotifications();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * Deletes all notifications (stubbed for now).
     */
    private void deleteAllNotifications() {
        // TODO: Implement delete functionality in Firestore
        notifications.clear();
        adapter.updateNotifications(notifications);
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        Toast.makeText(this, "All notifications deleted", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "All notifications deleted");
    }

    private void showLoading(boolean loading) {
        if (loadingIndicator == null) {
            return;
        }
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private String resolveCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return DeviceUtils.getDeviceId(this);
    }
}

