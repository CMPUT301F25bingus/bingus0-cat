package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.CancelledEntrantsAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CancelledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "CancelledEntrants";

    private String eventId;

    private RecyclerView recyclerView;
    private CancelledEntrantsAdapter adapter;
    private TextView totalCountText;
    private LinearLayout backButton;
    private TextView sendNotificationButton;

    private final List<Profile> cancelledProfiles = new ArrayList<>();
    private final List<String> cancelledStatuses = new ArrayList<>();
    
    // Services
    private NotificationService notificationService;
    private ProfileRepositoryFs profileRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_cancelled_entrants);

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.cancelled_entrants_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CancelledEntrantsAdapter();
        recyclerView.setAdapter(adapter);

        totalCountText = findViewById(R.id.total_selected_count);
        backButton = findViewById(R.id.back_button_container);
        sendNotificationButton = findViewById(R.id.send_notification_button);

        // Initialize services
        notificationService = new NotificationServiceFs();
        profileRepo = new ProfileRepositoryFs();

        backButton.setOnClickListener(v -> finish());
        
        // Setup send notification button
        sendNotificationButton.setOnClickListener(v -> handleSendNotificationClick());

        TextView title = findViewById(R.id.cancelledEntrantsTitle);
        if (title != null) title.setText("Cancelled Entrants");

        loadCancelledFromFirestore();
    }
    
    /**
     * Handles send notification button click.
     */
    private void handleSendNotificationClick() {
        if (cancelledProfiles.isEmpty()) {
            Toast.makeText(this, "No cancelled entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show confirmation dialog
        showSendNotificationDialog();
    }

    /**
     * Displays a dialog to compose and send the notification.
     */
    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to Cancelled Entrants");
        builder.setMessage("This will notify " + cancelledProfiles.size() + 
                " cancelled entrants about the event.\n\n" +
                "Do you want to proceed?");

        builder.setPositiveButton("Send", (dialog, which) -> {
            sendNotificationToCancelledEntrants();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * Sends notification to all cancelled entrants.
     */
    private void sendNotificationToCancelledEntrants() {
        // Show loading state
        sendNotificationButton.setEnabled(false);
        Toast.makeText(this, "Sending notifications...", Toast.LENGTH_SHORT).show();

        // Log userIds being used for debugging
        Log.d(TAG, "Sending notifications to " + cancelledProfiles.size() + " cancelled entrants:");
        for (Profile p : cancelledProfiles) {
            Log.d(TAG, "  - Profile: " + p.getName() + ", userId: " + p.getUserId());
        }

        // Prepare notification content
        String title = "📢 Event Update";
        String message = "The organizer has sent you an update regarding the event. " +
                "Please check your notifications for more details.";

        // Send notifications
        notificationService.sendNotificationToCancelledEntrants(
                eventId,
                cancelledProfiles,
                title,
                message,
                () -> handleSendSuccess(),
                error -> handleSendFailure(error)
        );
    }

    /**
     * Handles successful notification send.
     */
    private void handleSendSuccess() {
        runOnUiThread(() -> {
            sendNotificationButton.setEnabled(true);
            Toast.makeText(this, "✅ Notifications sent successfully to " + 
                    cancelledProfiles.size() + " cancelled entrants!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Successfully sent notifications to all cancelled entrants");
        });
    }

    /**
     * Handles notification send failure.
     * 
     * @param error Error message
     */
    private void handleSendFailure(String error) {
        runOnUiThread(() -> {
            sendNotificationButton.setEnabled(true);
            Toast.makeText(this, "❌ Failed to send notifications: " + error, 
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Failed to send notifications: " + error);
        });
    }

    private void loadCancelledFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList(
                        "CANCELLED_BY_ORGANIZER",
                        "CANCELLED_BY_ENTRANT"
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "Found " + snap.size() + " cancelled entrants");

                    if (snap.isEmpty()) {
//                        adapter.updateCancelledEntrants(cancelledProfiles, cancelledStatuses);
                        adapter.updateCancelledEntrants(
                                new ArrayList<>(cancelledProfiles),
                                new ArrayList<>(cancelledStatuses)
                        );

                        updateCount();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        // Registration document ID is the userId (Firebase Auth UID)
                        String userId = doc.getId(); // Document ID is the userId
                        
                        // Verify entrantId field matches (for debugging)
                        String entrantId = doc.getString("entrantId");
                        if (entrantId != null && !entrantId.isEmpty()) {
                            if (!entrantId.equals(userId)) {
                                Log.w(TAG, "Registration entrantId (" + entrantId + ") doesn't match document ID (" + userId + "), using entrantId");
                                userId = entrantId;
                            }
                        }
                        
                        // Also check userId field as fallback
                        String userIdField = doc.getString("userId");
                        if (userIdField != null && !userIdField.isEmpty()) {
                            userId = userIdField;
                        }
                        
                        String status = doc.getString("status");
                        Log.d(TAG, "Found cancelled registration - docId: " + doc.getId() + ", entrantId: " + entrantId + ", status: " + status);

                        cancelledStatuses.add(status);
                        loadProfile(userId);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving cancelled entrants", e);
                    Toast.makeText(this, "Error loading cancelled entrants", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfile(String userId) {
        // Load profile by userId (document ID = userId in profiles collection)
        profileRepo.get(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        // Always ensure userId matches (critical for notifications)
                        String profileUserId = profile.getUserId();
                        if (profileUserId == null || profileUserId.isEmpty() || !profileUserId.equals(userId)) {
                            Log.d(TAG, "Updating profile userId from '" + profileUserId + "' to '" + userId + "'");
                            profile.setUserId(userId);
                        }
                        
                        // Check if profile is already in the list (avoid duplicates)
                        boolean alreadyExists = false;
                        for (Profile p : cancelledProfiles) {
                            if (p.getUserId() != null && p.getUserId().equals(userId)) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        
                        if (!alreadyExists) {
                            cancelledProfiles.add(profile);
                            Log.d(TAG, "✓ Loaded profile for userId: " + userId + ", name: " + profile.getName());
                        } else {
                            Log.d(TAG, "Profile already in list for userId: " + userId);
                        }
                    } else {
                        Log.w(TAG, "Profile not found for userId: " + userId);
                    }
                    
                    adapter.updateCancelledEntrants(
                            new ArrayList<>(cancelledProfiles),
                            new ArrayList<>(cancelledStatuses)
                    );
                    updateCount();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed loading profile by document ID for userId: " + userId, e);
                    // Try fallback query by userId field
                    profileRepo.get(userId)
                            .addOnSuccessListener(profile -> {
                                if (profile != null) {
                                    // Always ensure userId matches (critical for notifications)
                                    String profileUserId = profile.getUserId();
                                    if (profileUserId == null || profileUserId.isEmpty() || !profileUserId.equals(userId)) {
                                        Log.d(TAG, "Updating profile userId from '" + profileUserId + "' to '" + userId + "' (fallback)");
                                        profile.setUserId(userId);
                                    }
                                    
                                    boolean alreadyExists = false;
                                    for (Profile p : cancelledProfiles) {
                                        if (p.getUserId() != null && p.getUserId().equals(userId)) {
                                            alreadyExists = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!alreadyExists) {
                                        cancelledProfiles.add(profile);
                                        Log.d(TAG, "✓ Loaded profile (fallback) for userId: " + userId);
                                    }
                                }
                                
                                adapter.updateCancelledEntrants(
                                        new ArrayList<>(cancelledProfiles),
                                        new ArrayList<>(cancelledStatuses)
                                );
                                updateCount();
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "✗ Failed loading profile (fallback) for userId: " + userId, e2);
                                adapter.updateCancelledEntrants(
                                        new ArrayList<>(cancelledProfiles),
                                        new ArrayList<>(cancelledStatuses)
                                );
                                updateCount();
                            });
                });
    }

    private void updateCount() {
        totalCountText.setText("Total cancelled entrants: " + cancelledProfiles.size());
    }
}
