package com.example.eventmaster.ui.entrant;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.model.Notification;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private static final String MOCK_USER_ID = "user_001"; // TODO: Replace with actual user ID

    // UI Components
    private ImageView backButton;
    private ImageView deleteAllButton;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    // Data
    private List<Notification> notifications;
    private NotificationsAdapter adapter;

    // Services
    private NotificationService notificationService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_notifications);

        // Initialize service
        notificationService = new NotificationServiceFs();

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
        Log.d(TAG, "Loading notifications for user: " + MOCK_USER_ID);

        // For demo purposes, use mock data if Firestore is empty
        // In production, only fetch from Firestore
        loadMockNotifications();
        
        // Uncomment below to load from Firestore:
        /*
        notificationService.getNotificationsForUser(
                MOCK_USER_ID,
                this::handleNotificationsLoaded,
                this::handleLoadError
        );
        */
    }

    /**
     * Creates mock notifications for demonstration purposes.
     * This simulates the notifications shown in the Figma design.
     */
    private void loadMockNotifications() {
        notifications = new ArrayList<>();

        // 1. Registration Confirmed (Win - US 01.04.01)
        Notification win1 = new Notification(
                "event_001",
                MOCK_USER_ID,
                "organizer_001",
                Notification.NotificationType.LOTTERY_WON,
                "Registration Confirmed",
                "You've been selected to Swimming Lesson for Kids."
        );
        win1.setSentAt(getDateHoursAgo(2));
        notifications.add(win1);

        // 2. Reminder
        Notification reminder1 = new Notification(
                "event_002",
                MOCK_USER_ID,
                "organizer_002",
                Notification.NotificationType.REMINDER,
                "Reminder: Register Before Deadline",
                "Reminder! Sign up for Piano Lessons before Dec 15"
        );
        reminder1.setSentAt(getDateDaysAgo(2));
        notifications.add(reminder1);

        // 3. Not Selected (Loss - US 01.04.02)
        Notification loss1 = new Notification(
                "event_003",
                MOCK_USER_ID,
                "organizer_003",
                Notification.NotificationType.LOTTERY_LOST,
                "Not Selected This Time",
                "Unfortunately, you weren't selected for Pottery.."
        );
        loss1.setSentAt(getDateWeeksAgo(1));
        notifications.add(loss1);

        // 4. New Spot Available (Win variant)
        Notification win2 = new Notification(
                "event_001",
                MOCK_USER_ID,
                "organizer_001",
                Notification.NotificationType.INVITATION,
                "New Spot Available!",
                "A new spot became available for Swim Lessons!"
        );
        win2.setSentAt(getDateWeeksAgo(3));
        notifications.add(win2);

        // 5. Event Cancelled
        Notification cancel1 = new Notification(
                "event_004",
                MOCK_USER_ID,
                "organizer_004",
                Notification.NotificationType.CANCELLATION,
                "Event Cancelled",
                "Beginner Yoga Class has been cancelled."
        );
        cancel1.setSentAt(getDateWeeksAgo(4));
        notifications.add(cancel1);

        // 6. Reminder 2
        Notification reminder2 = new Notification(
                "event_003",
                MOCK_USER_ID,
                "organizer_003",
                Notification.NotificationType.REMINDER,
                "Reminder: Event Starts Tomorrow",
                "Your Pottery Class starts tomorrow at 10:00 AM"
        );
        reminder2.setSentAt(getDateWeeksAgo(5));
        notifications.add(reminder2);

        // 7. Thank you message
        Notification thanks = new Notification(
                "event_005",
                MOCK_USER_ID,
                "organizer_005",
                Notification.NotificationType.GENERAL,
                "Thank you for Attending!",
                "We hope you enjoyed Pickle ball for Adults!"
        );
        thanks.setSentAt(getDateWeeksAgo(8));
        notifications.add(thanks);

        // Update adapter
        handleNotificationsLoaded(notifications);
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
        Toast.makeText(this, "Failed to load notifications: " + error, Toast.LENGTH_LONG).show();
        
        // Show empty state
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
}

