package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.*;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity displaying the waiting list for an event.
 * 
 * Allows organizers to:
 * - View all entrants in the waiting list
 * - Run the lottery to select entrants
 * - See total count of waiting entrants
 */
public class WaitingListActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListActivity";
    private RecyclerView recyclerView;
    private WaitingListAdapter adapter;
    private TextView totalCountText;
    private TextView drawReplacementText;
    private MaterialToolbar btnBack;
    private TextView textSendNotification;
    private TextView textSendNotificationNotSelected;

    private final WaitingListRepositoryFs waitingRepo = new WaitingListRepositoryFs();
    private final LotteryServiceFs lotteryService = new LotteryServiceFs();
    private final NotificationService notificationService = new NotificationServiceFs();
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

    private String eventId;
    private List<WaitingListEntry> currentWaitingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_waiting_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewWaitingList);
        totalCountText = findViewById(R.id.textTotalCount);
        drawReplacementText = findViewById(R.id.textDrawReplacement);
        btnBack = findViewById(R.id.btnBack);
        textSendNotification = findViewById(R.id.textSendNotification);
        textSendNotificationNotSelected = findViewById(R.id.textSendNotificationNotSelected);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitingListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Back button to return to View Entrants
        btnBack.setOnClickListener(v -> finish());

        // 📩 Send Notification to Waiting List Click (US 02.07.01)
        textSendNotification.setOnClickListener(v -> {
            if (currentWaitingList.isEmpty()) {
                Toast.makeText(this, "No entrants on waiting list to notify", Toast.LENGTH_SHORT).show();
                return;
            }
            showSendNotificationDialog();
        });

        // 📩 Send Notification to Not Selected Click
        textSendNotificationNotSelected.setOnClickListener(v -> {
            showSendNotificationToNotSelectedDialog();
        });

        // Load waiting list initially
        loadWaitingList(eventId);

        // 🎲 Run Lottery Click
        drawReplacementText.setOnClickListener(v -> {
            Toast.makeText(this, "Running Lottery...", Toast.LENGTH_SHORT).show();
            runLottery(eventId, 3); // pick 3 entrants for now
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning
        loadWaitingList(eventId);
    }

    private void loadWaitingList(String eventId) {
        waitingRepo.getWaitingList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                currentWaitingList = entries; // Store for notification sending
                adapter.updateList(entries);
                totalCountText.setText("Total waitlisted entrants: " + entries.size());
                Log.d(TAG, "Loaded " + entries.size() + " entrants");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching waiting list", e);
                Toast.makeText(WaitingListActivity.this,
                        "Failed to load waiting list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows dialog to send notification to waiting list entrants.
     */
    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to Waiting List");
        builder.setMessage("This will notify " + currentWaitingList.size() +
                " entrants on the waiting list.\n\n" +
                "Do you want to proceed?");

        builder.setPositiveButton("Send", (dialog, which) -> {
            sendNotificationToWaitingList();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * Core implementation of US 02.07.01.
     */
    private void sendNotificationToWaitingList() {
        if (currentWaitingList.isEmpty()) {
            Toast.makeText(this, "No entrants on waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        textSendNotification.setEnabled(false);
        Toast.makeText(this, "Loading profiles and sending notifications...", Toast.LENGTH_SHORT).show();

        // Get profiles for all waiting list entrants
        List<String> userIds = new ArrayList<>();
        for (WaitingListEntry entry : currentWaitingList) {
            userIds.add(entry.getUserId());
        }

        loadProfilesAndSendNotifications(userIds, true);
    }

    /**
     * Shows dialog to send notification to not selected entrants.
     */
    private void showSendNotificationToNotSelectedDialog() {
        // Query notifications to find not selected entrants
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("notifications")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", "LOTTERY_LOST")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (count == 0) {
                        Toast.makeText(this, "No not selected entrants found. Run the lottery first.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Send Notification to Not Selected Entrants");
                    builder.setMessage("This will notify " + count +
                            " entrants who were not selected in the lottery.\n\n" +
                            "Do you want to proceed?");

                    builder.setPositiveButton("Send", (dialog, which) -> {
                        sendNotificationToNotSelected(querySnapshot);
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });

                    builder.show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query not selected entrants", e);
                    Toast.makeText(this, "Failed to load not selected entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sends notification to not selected entrants.
     */
    private void sendNotificationToNotSelected(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        // Extract unique userIds from notifications
        Set<String> userIds = new HashSet<>();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            // Try recipientUserId first, then recipientId (legacy)
            String userId = doc.getString("recipientUserId");
            if (userId == null || userId.isEmpty()) {
                userId = doc.getString("recipientId");
            }
            if (userId != null && !userId.isEmpty()) {
                userIds.add(userId);
            }
        }

        if (userIds.isEmpty()) {
            Toast.makeText(this, "No valid user IDs found in notifications", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        textSendNotificationNotSelected.setEnabled(false);
        Toast.makeText(this, "Loading profiles and sending notifications...", Toast.LENGTH_SHORT).show();

        loadProfilesAndSendNotifications(new ArrayList<>(userIds), false);
    }

    /**
     * Loads profiles for user IDs and sends notifications.
     *
     * @param userIds List of user IDs
     * @param isWaitingList true if sending to waiting list, false if sending to not selected
     */
    private void loadProfilesAndSendNotifications(List<String> userIds, boolean isWaitingList) {
        List<Profile> profiles = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int total = userIds.size();

        if (total == 0) {
            Toast.makeText(this, "No users to notify", Toast.LENGTH_SHORT).show();
            if (isWaitingList) {
                textSendNotification.setEnabled(true);
            } else {
                textSendNotificationNotSelected.setEnabled(true);
            }
            return;
        }

        for (String userId : userIds) {
            profileRepo.get(userId)
                    .addOnSuccessListener(profile -> {
                        if (profile != null) {
                            // Ensure userId matches
                            String profileUserId = profile.getUserId();
                            if (profileUserId == null || profileUserId.isEmpty() || !profileUserId.equals(userId)) {
                                profile.setUserId(userId);
                            }
                            profiles.add(profile);
                        }
                        int done = completed.incrementAndGet();
                        if (done == total) {
                            // All profiles loaded, send notifications
                            sendNotificationsToProfiles(profiles, isWaitingList);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile for userId: " + userId, e);
                        int done = completed.incrementAndGet();
                        if (done == total) {
                            // Continue even if some profiles failed
                            if (!profiles.isEmpty()) {
                                sendNotificationsToProfiles(profiles, isWaitingList);
                            } else {
                                if (isWaitingList) {
                                    textSendNotification.setEnabled(true);
                                } else {
                                    textSendNotificationNotSelected.setEnabled(true);
                                }
                                Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Sends notifications to the provided profiles.
     *
     * @param profiles List of profiles to notify
     * @param isWaitingList true if sending to waiting list, false if sending to not selected
     */
    private void sendNotificationsToProfiles(List<Profile> profiles, boolean isWaitingList) {
        String title, message;

        if (isWaitingList) {
            title = "📢 Update from Event Organizer";
            message = "The organizer has sent you an update regarding the event. " +
                    "Please check your notifications for more details.";

            notificationService.sendNotificationToWaitingList(
                    eventId,
                    profiles,
                    title,
                    message,
                    () -> handleSendSuccess(profiles.size(), true),
                    error -> handleSendFailure(error, true)
            );
        } else {
            title = "📢 Event Update";
            message = "The organizer has sent you an update regarding the event. " +
                    "Please check your notifications for more details.";

            notificationService.sendNotificationToNotSelectedEntrants(
                    eventId,
                    profiles,
                    title,
                    message,
                    () -> handleSendSuccess(profiles.size(), false),
                    error -> handleSendFailure(error, false)
            );
        }
    }

    /**
     * Handles successful notification send.
     */
    private void handleSendSuccess(int count, boolean isWaitingList) {
        runOnUiThread(() -> {
            if (isWaitingList) {
                textSendNotification.setEnabled(true);
                Toast.makeText(this, "✅ Notifications sent successfully to " +
                        count + " waiting list entrants!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Successfully sent notifications to " + count + " waiting list entrants");
            } else {
                textSendNotificationNotSelected.setEnabled(true);
                Toast.makeText(this, "✅ Notifications sent successfully to " +
                        count + " not selected entrants!", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Successfully sent notifications to " + count + " not selected entrants");
            }
        });
    }

    /**
     * Handles notification send failure.
     */
    private void handleSendFailure(String error, boolean isWaitingList) {
        runOnUiThread(() -> {
            if (isWaitingList) {
                textSendNotification.setEnabled(true);
            } else {
                textSendNotificationNotSelected.setEnabled(true);
            }
            Toast.makeText(this, "❌ Failed to send notifications: " + error,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to send notifications: " + error);
        });
    }

    private void runLottery(String eventId, int count) {
        lotteryService.drawLottery(eventId, count)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Lottery Completed! ✓", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Lottery completed successfully");
                    } else {
                        // Even if some operations failed, the lottery might have partially worked
                        Toast.makeText(this, "Lottery completed with warnings", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Lottery completed with errors", task.getException());
                    }
                    
                    // Wait 1 second for Firebase to fully sync, then reload
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        loadWaitingList(eventId);
                    }, 1000);
                });
    }
}
