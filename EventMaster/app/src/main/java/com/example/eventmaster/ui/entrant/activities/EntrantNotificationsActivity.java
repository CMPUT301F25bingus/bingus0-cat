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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.ui.entrant.activities.EntrantHistoryActivity;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.example.eventmaster.ui.entrant.activities.EventListActivity;
import com.example.eventmaster.model.Profile;
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
import java.util.concurrent.atomic.AtomicInteger;

public class EntrantNotificationsActivity extends AppCompatActivity {

    private static final String TAG = "EntrantNotifications";

    // UI Components
    private ImageView deleteAllButton;
    private android.widget.ImageButton notificationToggle;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar loadingIndicator;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;

    // Data
    private List<Notification> notifications;
    private NotificationsAdapter adapter;
    private Profile currentProfile;

    // Services
    private NotificationService notificationService;
    private EventRepository eventRepository;
    private ProfileRepositoryFs profileRepo;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_notifications);

        // Initialize service
        notificationService = new NotificationServiceFs();
        eventRepository = new EventRepositoryFs();
        profileRepo = new ProfileRepositoryFs();

        currentUserId = resolveCurrentUserId();

        // Initialize UI components
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup swipe-to-delete
        setupSwipeToDelete();

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup click listeners
        setupClickListeners();

        loadNotifications();
        loadUserProfile();
    }

    /**
     * Initializes all view components.
     */
    private void initializeViews() {
        deleteAllButton = findViewById(R.id.delete_all_button);
        notificationToggle = findViewById(R.id.notification_toggle);
        recyclerView = findViewById(R.id.notifications_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        loadingIndicator = findViewById(R.id.loading_indicator);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnNotificationClickListener(notification -> handleNotificationClick(notification));
    }

    /**
     * Sets up swipe-to-delete functionality for notifications.
     */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag and drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position >= 0 && position < notifications.size()) {
                    Notification notification = notifications.get(position);
                    deleteNotification(notification, position);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Deletes a single notification from Firestore and updates the UI.
     */
    private void deleteNotification(Notification notification, int position) {
        if (notification == null || notification.getNotificationId() == null) {
            Log.w(TAG, "Cannot delete notification: missing notification or ID");
            // Restore the item if deletion can't proceed
            adapter.notifyItemChanged(position);
            return;
        }

        notificationService.deleteNotification(
                notification.getNotificationId(),
                () -> {
                    Log.d(TAG, "Successfully deleted notification: " + notification.getNotificationId());
                    // Remove from local list
                    notifications.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, notifications.size());

                    // Show empty state if no notifications left
                    if (notifications.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    Log.e(TAG, "Failed to delete notification: " + notification.getNotificationId(), new Exception(error));
                    // Restore the item on failure
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                }
        );
    }

    /**
     * Sets up bottom navigation item selection.
     */
    private void setupBottomNavigation() {
        // Set Alerts as selected (current screen)
        bottomNavigationView.setSelectedItemId(R.id.nav_alerts);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                finish();
                startActivity(getIntent().setClass(this, EventListActivity.class));
                return true;
            } else if (itemId == R.id.nav_history) {
                finish();
                startActivity(getIntent().setClass(this, EntrantHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_alerts) {
                // Already on Alerts screen
                return true;
            } else if (itemId == R.id.nav_profile) {
                finish();
                startActivity(getIntent().setClass(this, com.example.eventmaster.ui.shared.activities.ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Sets up click listeners for interactive components.
     */
    private void setupClickListeners() {
        deleteAllButton.setOnClickListener(v -> handleDeleteAllClick());
        
        // Notification toggle button listener
        notificationToggle.setOnClickListener(v -> {
            if (currentProfile != null) {
                boolean newState = !currentProfile.isNotificationsEnabled();
                updateNotificationPreference(newState);
            }
        });
    }

    // -------------------------------------------------------------------------
    //                     ***  MAIN LOAD NOTIFICATIONS FIX  ***
    // -------------------------------------------------------------------------

    private void loadNotifications() {
        showLoading(true);

        String deviceId = DeviceUtils.getDeviceId(this);

        Log.d(TAG, "Resolving notifications for:");
        Log.d(TAG, "  currentUserId=" + currentUserId);
        Log.d(TAG, "  deviceId=" + deviceId);

        // 1. Collect all IDs we need to load notifications for
        List<String> queryIds = new ArrayList<>();
        queryIds.add(currentUserId);

        if (!deviceId.equals(currentUserId)) {
            queryIds.add(deviceId);
        }

        // 2. Retrieve profile using deviceId (Option B fix)
        profileRepo.getByDeviceId(deviceId).addOnSuccessListener(profile -> {

            if (profile != null && profile.getUserId() != null) {
                Log.d(TAG, "Found profile for deviceId → userId: " + profile.getUserId());
                queryIds.add(profile.getUserId());
            } else {
                Log.d(TAG, "No profile mapped to this deviceId");
            }

            // 3. Load notifications for ALL collected IDs
            loadNotificationsForIds(queryIds);
        });
    }

    private void loadNotificationsForIds(List<String> ids) {
        Log.d(TAG, "Loading notifications for IDs: " + ids);

        AtomicInteger remaining = new AtomicInteger(ids.size());
        Set<String> seen = new HashSet<>();
        List<Notification> combined = new ArrayList<>();

        for (String id : ids) {
            notificationService.getNotificationsForUser(
                    id,
                    notifs -> {
                        if (notifs != null) {
                            for (Notification n : notifs) {
                                if (n.getNotificationId() != null && seen.add(n.getNotificationId())) {
                                    combined.add(n);
                                }
                            }
                        }

                        if (remaining.decrementAndGet() == 0) {
                            finalizeLoadedNotifications(combined);
                        }
                    },
                    err -> {
                        if (remaining.decrementAndGet() == 0) {
                            finalizeLoadedNotifications(combined);
                        }
                    }
            );
        }
    }

    private void finalizeLoadedNotifications(List<Notification> all) {

        all.sort((a, b) -> {
            Date da = a.getSentAt();
            Date db = b.getSentAt();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });

        handleNotificationsLoaded(all);
        showLoading(false);
    }

    // -------------------------------------------------------------------------
    //                                UI UPDATE
    // -------------------------------------------------------------------------

    private void handleNotificationsLoaded(List<Notification> loadedNotifications) {
        Log.d(TAG, "Loaded " + loadedNotifications.size() + " notifications");
        this.notifications = loadedNotifications;
        adapter.updateNotifications(loadedNotifications);

        if (loadedNotifications.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void handleNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            notificationService.markNotificationAsRead(notification.getNotificationId());
            notification.setRead(true);
        }
        showNotificationDetails(notification);
    }

    private void showNotificationDetails(Notification notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(notification.getTitle());
        builder.setMessage(notification.getMessage());
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        if (notification.getType() == Notification.NotificationType.LOTTERY_WON
                && notification.getEventId() != null) {
            builder.setNeutralButton("View Event", (dialog, which) -> {
                Intent intent = new Intent(this, EventDetailsActivity.class);
                intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, notification.getEventId());
                startActivity(intent);
            });
        }

        builder.show();
    }

    private void handleDeleteAllClick() {
        if (notifications.isEmpty()) {
            Toast.makeText(this, "No notifications to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Delete All Notifications");
        b.setMessage("Are you sure you want to delete all notifications?");
        b.setPositiveButton("Delete", (d, w) -> {
            deleteAllNotificationsFromFirestore();
        });
        b.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        b.show();
    }

    /**
     * Deletes all notifications from Firestore for the current user (all IDs).
     */
    private void deleteAllNotificationsFromFirestore() {
        showLoading(true);
        
        String deviceId = DeviceUtils.getDeviceId(this);
        List<String> userIdsToDelete = new ArrayList<>();
        userIdsToDelete.add(currentUserId);
        
        if (!deviceId.equals(currentUserId)) {
            userIdsToDelete.add(deviceId);
        }

        // Get profile userId if available
        profileRepo.getByDeviceId(deviceId).addOnSuccessListener(profile -> {
            if (profile != null && profile.getUserId() != null && !userIdsToDelete.contains(profile.getUserId())) {
                userIdsToDelete.add(profile.getUserId());
            }
            
            // Delete notifications for all user IDs
            AtomicInteger completed = new AtomicInteger(0);
            int total = userIdsToDelete.size();
            
            if (total == 0) {
                finishDeletion();
                return;
            }
            
            for (String userId : userIdsToDelete) {
                notificationService.deleteAllNotificationsForUser(
                        userId,
                        () -> {
                            Log.d(TAG, "Successfully deleted notifications for userId: " + userId);
                            if (completed.incrementAndGet() == total) {
                                finishDeletion();
                            }
                        },
                        error -> {
                            Log.e(TAG, "Failed to delete notifications for userId: " + userId + ": " + error);
                            // Continue even if one fails - we still want to finish
                            if (completed.incrementAndGet() == total) {
                                finishDeletion();
                            }
                        }
                );
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get profile for deletion", e);
            // Still try to delete with what we have
            AtomicInteger completed = new AtomicInteger(0);
            int total = userIdsToDelete.size();
            
            if (total == 0) {
                finishDeletion();
                return;
            }
            
            for (String userId : userIdsToDelete) {
                notificationService.deleteAllNotificationsForUser(
                        userId,
                        () -> {
                            Log.d(TAG, "Successfully deleted notifications for userId: " + userId);
                            if (completed.incrementAndGet() == total) {
                                finishDeletion();
                            }
                        },
                        error -> {
                            Log.e(TAG, "Failed to delete notifications for userId: " + userId + ": " + error);
                            if (completed.incrementAndGet() == total) {
                                finishDeletion();
                            }
                        }
                );
            }
        });
    }

    /**
     * Finalizes the deletion process by refreshing the UI.
     */
    private void finishDeletion() {
        // Reload notifications to reflect deletions
        loadNotifications();
        Toast.makeText(this, "All notifications deleted", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean loading) {
        if (loadingIndicator == null) return;
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

    /**
     * Loads the user's profile and sets the notification icon state.
     */
    private void loadUserProfile() {
        String deviceId = DeviceUtils.getDeviceId(this);
        
        profileRepo.getByDeviceId(deviceId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        currentProfile = profile;
                        // Set icon state based on profile preference
                        updateNotificationIcon(profile.isNotificationsEnabled());
                    } else {
                        // If no profile, create one with notifications enabled by default
                        Profile newProfile = new Profile();
                        newProfile.setUserId(currentUserId);
                        newProfile.setDeviceId(deviceId);
                        newProfile.setNotificationsEnabled(true);
                        profileRepo.upsert(newProfile)
                                .addOnSuccessListener(p -> {
                                    currentProfile = newProfile;
                                    updateNotificationIcon(true);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load profile", e);
                    // Default to enabled if we can't load profile
                    updateNotificationIcon(true);
                });
    }

    /**
     * Updates the notification icon appearance based on state.
     */
    private void updateNotificationIcon(boolean enabled) {
        if (notificationToggle == null) return;
        
        if (enabled) {
            // Bell icon - notifications ON (teal color)
            notificationToggle.setImageResource(R.drawable.ic_bell_outline);
            notificationToggle.setColorFilter(getResources().getColor(R.color.teal_dark, null));
        } else {
            // Bell icon with slash - notifications OFF (gray color)
            notificationToggle.setImageResource(R.drawable.ic_bell_off);
            notificationToggle.setColorFilter(getResources().getColor(android.R.color.darker_gray, null));
        }
    }

    /**
     * Updates the notification preference in Firestore.
     */
    private void updateNotificationPreference(boolean enabled) {
        if (currentProfile == null) {
            Log.w(TAG, "Cannot update preference: no profile loaded");
            return;
        }
        
        String profileId = currentProfile.getUserId();
        if (profileId == null || profileId.isEmpty()) {
            profileId = currentUserId;
        }
        
        // Update icon immediately for better UX
        updateNotificationIcon(enabled);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationsEnabled", enabled);
        
        profileRepo.update(profileId, updates)
                .addOnSuccessListener(v -> {
                    currentProfile.setNotificationsEnabled(enabled);
                    Log.d(TAG, "Notification preference updated: " + enabled);
                })
                .addOnFailureListener(err -> {
                    // Revert icon on failure
                    updateNotificationIcon(!enabled);
                    Log.e(TAG, "Failed to update notification preference", err);
                });
    }
}
