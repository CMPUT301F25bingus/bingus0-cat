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
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.ChosenListAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity that displays all entrants who were chosen by the lottery.
 * This screen allows the organizer to view selected entrants and cancel all
 * pending invitations if needed. Cancelling pending invitations will move those
 * entrants into the cancelled entrants list and clear the chosen_list collection.
 */
public class ChosenListActivity extends AppCompatActivity {

    private static final String TAG = "ChosenListActivity";
    private RecyclerView recyclerView;
    private ChosenListAdapter adapter;
    private TextView totalChosenText;


    private TextView textSendNotification;
    private final WaitingListRepositoryFs repo = new WaitingListRepositoryFs();
    private final RegistrationServiceFs registrationService = new RegistrationServiceFs();

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
        textSendNotification = findViewById(R.id.textSendNotification);
        MaterialToolbar btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChosenListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Cancel entrants link
        TextView cancelEntrantsLink = findViewById(R.id.cancel_entrants_link);
        cancelEntrantsLink.setOnClickListener(v -> cancelAllPendingInvitations());

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

    /**
     * Loads all entrants currently in the chosen_list collection for the event.
     * Updates the UI list and total count.
     *
     * @param eventId The Firestore event ID.
     */
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
     * Cancels all invitations that are still pending. This changes the registration
     * status to CANCELLED_BY_ORGANIZER, updates the invitation document, and then
     * clears the chosen_list collection. Used when the organizer wants to revoke
     * all outstanding invites at once.
     */
    private void cancelAllPendingInvitations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("invitations")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(invSnap -> {

                    if (invSnap.isEmpty()) {
                        Toast.makeText(this, "No pending invitations to cancel", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : invSnap.getDocuments()) {

                        // Get entrantId from document field, not doc ID
                        String entrantId = doc.getString("entrantId");

                        if (entrantId == null || entrantId.isEmpty()) {
                            Log.e(TAG, "Invitation missing entrantId: " + doc.getId());
                            continue;
                        }

                        // 1) Cancel the registration using your service
                        registrationService.cancel(
                                eventId,
                                entrantId,
                                true,   // cancelled BY ORGANIZER
                                (v) -> Log.d(TAG, "Cancelled registration for: " + entrantId),
                                (err) -> Log.e(TAG, "Failed to cancel " + entrantId, err)
                        );

                        // 2) Update the invitation status
                        db.collection("events")
                                .document(eventId)
                                .collection("invitations")
                                .document(doc.getId())
                                .update("status", "CANCELLED_BY_ORGANIZER");
                    }

                    Toast.makeText(this, "Cancelled all pending invitations", Toast.LENGTH_SHORT).show();
                    deleteChosenListCollection(eventId);
//                    loadChosenList(eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading invitations", e);
                    Toast.makeText(this, "Error cancelling entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes all documents inside the chosen_list collection after pending
     * invitations are cancelled. Afterwards, the chosen list UI is refreshed.
     *
     * @param eventId The Firestore event ID.
     */
    private void deleteChosenListCollection(String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("chosen_list")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("ChosenList", "No chosen_list docs to delete.");
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(v -> Log.d("ChosenList", "Deleted " + doc.getId()))
                                .addOnFailureListener(e -> Log.e("ChosenList", "Error deleting doc", e));
                    }

                    Log.d("ChosenList", "chosen_list collection cleared.");
                    loadChosenList(eventId);

                })
                .addOnFailureListener(e -> {
                    Log.e("ChosenList", "Failed to load chosen_list", e);
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
