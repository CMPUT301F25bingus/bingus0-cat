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
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.CancelledEntrantsAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;


/**
 * Activity that displays all entrants who are no longer participating in the event
 * due to cancelled registrations. A registration is considered cancelled when its
 * status is either CANCELLED_BY_ORGANIZER or CANCELLED_BY_ENTRANT.
 *
 * This screen loads the cancelled entrants, retrieves their profile information,
 * shows the total number of cancelled users, and allows the organizer to run
 * a replacement lottery. The replacement lottery randomly selects new entrants
 * from the not_selected list and issues new invitations to them.
 */

public class CancelledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "CancelledEntrants";

    private String eventId;

    private RecyclerView recyclerView;
    private CancelledEntrantsAdapter adapter;
    private TextView totalCountText;
    private TextView sendNotificationButton;
    private MaterialToolbar backButton;

    private final List<Profile> cancelledProfiles = new ArrayList<>();
    private final List<String> cancelledStatuses = new ArrayList<>();

    // Services
    private NotificationService notificationService;
    private ProfileRepositoryFs profileRepo;
    private TextView textDrawReplacement;


    /**
     * Initializes the screen, retrieves the eventId,
     * sets up the RecyclerView and toolbar, and loads cancelled entrants.
     */
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
        sendNotificationButton = findViewById(R.id.textSendNotification);

        // Initialize services
        notificationService = new NotificationServiceFs();
        profileRepo = new ProfileRepositoryFs();

        backButton.setOnClickListener(v -> finish());

        // Setup send notification button
        sendNotificationButton.setOnClickListener(v -> handleSendNotificationClick());

        findViewById(R.id.textDrawReplacement).setOnClickListener(v -> {
            runReplacementLottery();
        });


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

    /**
     * Fetches all entrants whose registration status indicates cancellation.
     * After fetching the registration documents, it loads each user's profile
     * so their name and details can be shown in the list.
     */
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
//                        loadProfile(userId);
                        loadProfileWithFallback(userId, entrantId, userIdField);

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

    /**
     * Updates the on-screen count of cancelled entrants.
     */
    private void updateCount() {
        totalCountText.setText("Total cancelled entrants: " + cancelledProfiles.size());
    }

    /**
     * Runs the replacement lottery. This randomly selects users from the
     * not_selected collection (who were not chosen originally), creates new
     * invitations for them, sends notifications, adds them to chosen_list,
     * and removes them from not_selected.
     *
     * This is used when spots open up due to cancellations.
     */
    private void runReplacementLottery() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        int numCancelled = cancelledProfiles.size();
        if (numCancelled == 0) {
            Toast.makeText(this, "No cancelled entrants to replace.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Fetch from not_selected list
        db.collection("events")
                .document(eventId)
                .collection("not_selected")
                .get()
                .addOnSuccessListener(notSelectedSnap -> {

                    if (notSelectedSnap.isEmpty()) {
                        Toast.makeText(this, "No remaining entrants in not_selected.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert to list
                    List<DocumentSnapshot> docs = new ArrayList<>(notSelectedSnap.getDocuments());

                    // Shuffle to randomize
                    java.util.Collections.shuffle(docs);

                    // Pick replacements = number cancelled
                    List<DocumentSnapshot> replacements = docs.subList(0, Math.min(numCancelled, docs.size()));

                    List<Task<Void>> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : replacements) {
                        String userId = doc.getId();
                        WaitingListEntry entry = doc.toObject(WaitingListEntry.class);

                        if (entry == null) continue;

                        entry.setStatus("selected");

                        // 2. Add to chosen_list
                        Task<Void> addChosen = db.collection("events")
                                .document(eventId)
                                .collection("chosen_list")
                                .document(userId)
                                .set(entry);

                        tasks.add(addChosen);

                        // 3. Create a new invitation
                        Map<String, Object> invitation = new HashMap<>();
                        invitation.put("eventId", eventId);
                        invitation.put("entrantId", userId);
                        invitation.put("status", "PENDING");
                        invitation.put("createdAtUtc", System.currentTimeMillis());

                        Task<Void> inviteTask = db.collection("events")
                                .document(eventId)
                                .collection("invitations")
                                .document(userId)
                                .set(invitation, SetOptions.merge());

                        tasks.add(inviteTask);

                        // 4. Send notification
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("eventId", eventId);
                        notification.put("recipientId", userId);
                        notification.put("type", "LOTTERY_REPLACEMENT_SELECTED");
                        notification.put("title", "You've been selected!");
                        notification.put("message", "A spot has opened up, and you’ve been selected as a replacement. Please accept or decline your invitation.");
                        notification.put("isRead", false);
                        notification.put("createdAt", com.google.firebase.Timestamp.now());

                        Task<Void> notifTask = db.collection("notifications")
                                .add(notification)
                                .continueWith(t -> null);

                        tasks.add(notifTask);
                        //5. send push notifccation:
                        profileRepo.get(userId)
                                .addOnSuccessListener(profile -> {
                                    if (profile != null) {

                                        // Ensure userId is correct
                                        String profileUserId = profile.getUserId();
                                        if (profileUserId == null || !profileUserId.equals(userId)) {
                                            profile.setUserId(userId);
                                        }

                                        String replacementTitle = "🎉 You've been selected!";
                                        String replacementMessage = "A spot opened up and you’ve been selected as a replacement. " +
                                                "Go to the event page to respond to your invitation.";

                                        notificationService.sendNotificationToSelectedEntrants(
                                                eventId,
                                                List.of(profile),
                                                replacementTitle,
                                                replacementMessage,
                                                () -> Log.d(TAG, "🔔 Push notification sent to replacement: " + userId),
                                                (error) -> Log.e(TAG, "❌ Failed to send push to replacement: " + userId)
                                        );
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch profile for push notif: " + userId, e));



                        // 6. Remove from not_selected
                        Task<Void> removeTask = db.collection("events")
                                .document(eventId)
                                .collection("not_selected")
                                .document(userId)
                                .delete();

                        tasks.add(removeTask);
                    }

                    // Wait for all tasks
                    com.google.android.gms.tasks.Tasks.whenAll(tasks)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Replacement lottery completed!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(err -> {
                                Toast.makeText(this, "Error during replacement lottery.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Replacement lottery failed", err);
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load not_selected list.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading not_selected", e);
                });
    }

    private void loadProfileWithFallback(String userId, String entrantId, String userIdField) {
        // 1. Try new system: docId = userId
        profileRepo.get(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        profile.setUserId(userId);
                        addCancelledProfile(profile);
                        return;
                    }

                    // 2. Try entrantId (legacy deviceId sometimes stored here)
                    if (entrantId != null && !entrantId.isEmpty()) {
                        profileRepo.get(entrantId)
                                .addOnSuccessListener(p2 -> {
                                    if (p2 != null) {
                                        p2.setUserId(entrantId);
                                        addCancelledProfile(p2);
                                        return;
                                    }

                                    // 3. Try userId field
                                    if (userIdField != null && !userIdField.isEmpty()) {
                                        profileRepo.get(userIdField)
                                                .addOnSuccessListener(p3 -> {
                                                    if (p3 != null) {
                                                        p3.setUserId(userIdField);
                                                        addCancelledProfile(p3);
                                                        return;
                                                    }

                                                    // 4. Last fallback: old deviceId lookup
                                                    loadProfileByDeviceId(userId);
                                                });
                                    } else {
                                        loadProfileByDeviceId(userId);
                                    }
                                });
                    } else {
                        loadProfileByDeviceId(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed primary lookup for " + userId, e);
                    loadProfileByDeviceId(userId);
                });
    }

    private void loadProfileByDeviceId(String deviceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Profile p = snap.getDocuments().get(0).toObject(Profile.class);
                        if (p != null) {
                            addCancelledProfile(p);
                            return;
                        }
                    }

                    Log.w(TAG, "No profile found using deviceId fallback: " + deviceId);
                    adapter.updateCancelledEntrants(
                            new ArrayList<>(cancelledProfiles),
                            new ArrayList<>(cancelledStatuses)
                    );
                    updateCount();
                })
                .addOnFailureListener(e -> Log.e(TAG, "DeviceId fallback failed for " + deviceId, e));
    }

    /**
     * Adds a cancelled entrant's profile safely (no duplicates)
     * and updates the adapter + count.
     */
    private void addCancelledProfile(Profile profile) {
        if (profile == null) return;

        String userId = profile.getUserId();

        // Avoid duplicates
        for (Profile p : cancelledProfiles) {
            if (p.getUserId() != null && p.getUserId().equals(userId)) {
                Log.d(TAG, "Skipping duplicate profile for " + userId);
                return;
            }
        }

        cancelledProfiles.add(profile);
        Log.d(TAG, "✓ Added cancelled profile for " + userId);

        // Update the UI lists
        adapter.updateCancelledEntrants(
                new ArrayList<>(cancelledProfiles),
                new ArrayList<>(cancelledStatuses)
        );

        updateCount();
    }




}
