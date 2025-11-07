package com.example.eventmaster.ui.entrant;

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
    private static final boolean USE_MOCK_FALLBACK = true;

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
        setContentView(R.layout.activity_entrant_notifications);

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
     */
    private void loadNotifications() {
        showLoading(true);
        Log.d(TAG, "Loading notifications for user: " + currentUserId);

        notificationService.getNotificationsForUser(
                currentUserId,
                this::handleNotificationsFetched,
                this::handleLoadError
        );
    }

    private void handleNotificationsFetched(List<Notification> loadedNotifications) {
        if (loadedNotifications == null || loadedNotifications.isEmpty()) {
            Log.d(TAG, "No notifications found in Firestore for user: " + currentUserId);
            if (USE_MOCK_FALLBACK) {
                loadMockNotificationsFallback();
            } else {
                showLoading(false);
                handleNotificationsLoaded(new ArrayList<>());
            }
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

    private void loadMockNotificationsFallback() {
        Log.d(TAG, "Loading mock notifications based on available events");
        eventRepository.getAllEvents(new EventRepository.OnEventListListener() {
            @Override
            public void onSuccess(List<Event> events) {
                showLoading(false);
                List<Notification> mockNotifications = createMockNotifications(events);
                handleNotificationsLoaded(mockNotifications);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load events for mock notifications", e);
                showLoading(false);
                List<Notification> mockNotifications = createMockNotifications(null);
                handleNotificationsLoaded(mockNotifications);
            }
        });
    }

    private List<Notification> createMockNotifications(@Nullable List<Event> events) {
        List<Notification> generated = new ArrayList<>();

        generated.add(buildMockNotification(events, 0,
                Notification.NotificationType.LOTTERY_WON,
                "Registration Confirmed",
                "You've been selected to %s.",
                getDateHoursAgo(2)));

        generated.add(buildMockNotification(events, 1,
                Notification.NotificationType.REMINDER,
                "Reminder: Register Before Deadline",
                "Reminder! Sign up for %s before the deadline.",
                getDateDaysAgo(2)));

        generated.add(buildMockNotification(events, 2,
                Notification.NotificationType.LOTTERY_LOST,
                "Not Selected This Time",
                "Unfortunately, you weren't selected for %s.",
                getDateWeeksAgo(1)));

        generated.add(buildMockNotification(events, 3,
                Notification.NotificationType.INVITATION,
                "New Spot Available!",
                "A new spot became available for %s!",
                getDateWeeksAgo(3)));

        generated.add(buildMockNotification(events, 4,
                Notification.NotificationType.CANCELLATION,
                "Event Cancelled",
                "%s has been cancelled.",
                getDateWeeksAgo(4)));

        generated.add(buildMockNotification(events, 5,
                Notification.NotificationType.REMINDER,
                "Reminder: Event Starts Tomorrow",
                "Your %s starts tomorrow at 10:00 AM.",
                getDateWeeksAgo(5)));

        generated.add(buildMockNotification(events, 6,
                Notification.NotificationType.GENERAL,
                "Thank you for Attending!",
                "We hope you enjoyed %s!",
                getDateWeeksAgo(8)));

        return generated;
    }

    private Notification buildMockNotification(@Nullable List<Event> events,
                                               int index,
                                               Notification.NotificationType type,
                                               String titleTemplate,
                                               String messageTemplate,
                                               Date sentAt) {
        Event event = (events != null && index < events.size()) ? events.get(index) : null;
        String eventName = firstNonEmpty(
                event != null ? event.getName() : null,
                event != null ? event.getTitle() : null,
                getDefaultEventName(index));
        String eventId = firstNonEmpty(
                event != null ? event.getId() : null,
                String.format(Locale.getDefault(), "event_%03d", index + 1));
        String organizerId = firstNonEmpty(
                event != null ? event.getOrganizerId() : null,
                String.format(Locale.getDefault(), "organizer_%03d", index + 1));

        String title = String.format(Locale.getDefault(), titleTemplate, eventName);
        String message = String.format(Locale.getDefault(), messageTemplate, eventName);

        Notification notification = new Notification(
                eventId,
                currentUserId,
                organizerId,
                type,
                title,
                message
        );
        notification.setSentAt(sentAt);
        return notification;
    }

    private String getDefaultEventName(int index) {
        switch (index) {
            case 0:
                return "Swimming Lesson for Kids";
            case 1:
                return "Piano Lessons";
            case 2:
                return "Pottery Workshop";
            case 3:
                return "Swim Lessons";
            case 4:
                return "Beginner Yoga Class";
            case 5:
                return "Pottery Class";
            default:
                return "Pickleball for Adults";
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
     * Helper method to create a date X hours ago.
     */
    private Date getDateHoursAgo(int hours) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -hours);
        return cal.getTime();
    }

    /**
     * Helper method to create a date X days ago.
     */
    private Date getDateDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        return cal.getTime();
    }

    /**
     * Helper method to create a date X weeks ago.
     */
    private Date getDateWeeksAgo(int weeks) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -weeks);
        return cal.getTime();
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

        if (USE_MOCK_FALLBACK) {
            loadMockNotificationsFallback();
            return;
        }

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
        
        // For lottery win notifications, could add Accept/Decline buttons (US 01.05.02/01.05.03)
        if (notification.getType() == Notification.NotificationType.LOTTERY_WON) {
            builder.setNeutralButton("View Event", (dialog, which) -> {
                Toast.makeText(this, "Navigate to event details - Coming soon!", Toast.LENGTH_SHORT).show();
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

