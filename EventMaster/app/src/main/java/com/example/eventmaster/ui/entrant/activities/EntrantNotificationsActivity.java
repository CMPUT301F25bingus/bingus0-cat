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
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.entrant.adapters.NotificationsAdapter;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    private ProfileRepositoryFs profileRepo;

    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_notifications);

        notificationService = new NotificationServiceFs();
        eventRepository = new EventRepositoryFs();
        profileRepo = new ProfileRepositoryFs();

        currentUserId = resolveCurrentUserId();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();

        loadNotifications();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        deleteAllButton = findViewById(R.id.delete_all_button);
        recyclerView = findViewById(R.id.notifications_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationsAdapter(notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnNotificationClickListener(notification -> handleNotificationClick(notification));
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        deleteAllButton.setOnClickListener(v -> handleDeleteAllClick());
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
            notifications.clear();
            adapter.updateNotifications(notifications);
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        });
        b.setNegativeButton("Cancel", (d, w) -> d.dismiss());
        b.show();
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
}
