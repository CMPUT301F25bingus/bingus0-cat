package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity displaying the list of entrants chosen by the lottery.
 * 
 * Shows entrants who have been selected but haven't yet responded (accepted/declined).
 * Once they respond, they move to either Selected (accepted) or Cancelled (declined) lists.
 */
public class ChosenListActivity extends AppCompatActivity {

    private static final String TAG = "ChosenListActivity";
    private RecyclerView recyclerView;
    private ChosenListAdapter adapter;
    private TextView totalChosenText;
    private TextView textSendNotification;
    private final WaitingListRepositoryFs repo = new WaitingListRepositoryFs();
    private final NotificationService notificationService = new NotificationServiceFs();
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    private String eventId;
    private List<WaitingListEntry> currentChosenList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_chosen_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "No eventId provided!");
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewChosenList);
        totalChosenText = findViewById(R.id.textTotalChosen);
        TextView btnBack = findViewById(R.id.btnBack);
        textSendNotification = findViewById(R.id.textSendNotification);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChosenListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // Send Notification button click listener
        if (textSendNotification != null) {
            textSendNotification.setOnClickListener(v -> {
                if (currentChosenList.isEmpty()) {
                    Toast.makeText(this, "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSendNotificationDialog();
            });
        }

        loadChosenList(eventId);
    }

    private void loadChosenList(String eventId) {
        repo.getChosenList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                currentChosenList = entries; // Store for notification sending
                adapter.updateList(entries);
                totalChosenText.setText("Total chosen entrants: " + entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                totalChosenText.setText("Failed to load chosen entrants");
                Log.e("ChosenList", "Error loading chosen list", e);
            }
        });
    }

    /**
     * Shows dialog to send notification to chosen entrants.
     */
    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to Chosen Entrants");
        builder.setMessage("This will notify " + currentChosenList.size() + 
                " entrants who have been chosen in the lottery.\n\n" +
                "They will be notified to go to the event to accept or decline their invitation.\n\n" +
                "Do you want to proceed?");

        builder.setPositiveButton("Send", (dialog, which) -> {
            sendNotificationToChosenEntrants();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * Sends notification to all chosen entrants.
     */
    private void sendNotificationToChosenEntrants() {
        if (currentChosenList.isEmpty()) {
            Toast.makeText(this, "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        if (textSendNotification != null) {
            textSendNotification.setEnabled(false);
        }
        Toast.makeText(this, "Loading profiles and sending notifications...", Toast.LENGTH_SHORT).show();

        // Get profiles for all chosen entrants
        List<String> userIds = new ArrayList<>();
        for (WaitingListEntry entry : currentChosenList) {
            userIds.add(entry.getUserId());
        }

        loadProfilesAndSendNotifications(userIds);
    }

    /**
     * Loads profiles for user IDs and sends notifications.
     */
    private void loadProfilesAndSendNotifications(List<String> userIds) {
        List<Profile> profiles = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int total = userIds.size();

        if (total == 0) {
            Toast.makeText(this, "No users to notify", Toast.LENGTH_SHORT).show();
            if (textSendNotification != null) {
                textSendNotification.setEnabled(true);
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
                            sendNotificationsToProfiles(profiles);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load profile for userId: " + userId, e);
                        int done = completed.incrementAndGet();
                        if (done == total) {
                            // Continue even if some profiles failed
                            if (!profiles.isEmpty()) {
                                sendNotificationsToProfiles(profiles);
                            } else {
                                if (textSendNotification != null) {
                                    textSendNotification.setEnabled(true);
                                }
                                Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    /**
     * Sends notifications to the provided profiles.
     */
    private void sendNotificationsToProfiles(List<Profile> profiles) {
        // Prepare notification content
        String title = "🎉 You've been chosen!";
        String message = "Congratulations! You've been chosen in the lottery for this event. " +
                "Go to the event details to accept or decline your invitation.";

        // Send notifications
        notificationService.sendNotificationToSelectedEntrants(
                eventId,
                profiles,
                title,
                message,
                () -> handleSendSuccess(profiles.size()),
                error -> handleSendFailure(error)
        );
    }

    /**
     * Handles successful notification send.
     */
    private void handleSendSuccess(int count) {
        runOnUiThread(() -> {
            if (textSendNotification != null) {
                textSendNotification.setEnabled(true);
            }
            Toast.makeText(this, "✅ Notifications sent successfully to " + 
                    count + " chosen entrants!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Successfully sent notifications to " + count + " chosen entrants");
        });
    }

    /**
     * Handles notification send failure.
     */
    private void handleSendFailure(String error) {
        runOnUiThread(() -> {
            if (textSendNotification != null) {
                textSendNotification.setEnabled(true);
            }
            Toast.makeText(this, "❌ Failed to send notifications: " + error, 
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to send notifications: " + error);
        });
    }
}

