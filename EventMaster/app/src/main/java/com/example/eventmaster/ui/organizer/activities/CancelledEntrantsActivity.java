package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.CancelledEntrantsAdapter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Screen showing all cancelled entrants for an event.
 * Cancelled entrants include:
 *  - CANCELLED_BY_ORGANIZER
 *  - CANCELLED_BY_ENTRANT
 *
 * Features:
 *  - Display cancelled entrants
 *  - Send mass notifications to cancelled entrants
 *  - Run replacement lottery
 *  - Resolve profile fallbacks correctly using:
 *      1. userId (trusted)
 *      2. entrantId
 *      3. deviceId (legacy)
 */
public class CancelledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "CancelledEntrants";

    private String eventId;

    private RecyclerView recyclerView;
    private CancelledEntrantsAdapter adapter;
    private TextView totalCountText;
    private TextView sendNotificationButton;
    private android.view.View backButton;
    private TextView emptyStateText;

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
        sendNotificationButton = findViewById(R.id.textSendNotification);
        emptyStateText = findViewById(R.id.empty_state_text);

        notificationService = new NotificationServiceFs();
        profileRepo = new ProfileRepositoryFs();

        backButton.setOnClickListener(v -> finish());
        sendNotificationButton.setOnClickListener(v -> handleSendNotificationClick());

        findViewById(R.id.textDrawReplacement).setOnClickListener(v -> runReplacementLottery());

        TextView title = findViewById(R.id.cancelledEntrantsTitle);
        if (title != null) title.setText("Cancelled Entrants");

        loadCancelledFromFirestore();
    }

    /**
     * Triggered when the user taps Send Notification.
     */
    private void handleSendNotificationClick() {
        if (cancelledProfiles.isEmpty()) {
            Toast.makeText(this, "No cancelled entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }
        showSendNotificationDialog();
    }

    /**
     * Confirmation dialog before sending notifications.
     */
    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to Cancelled Entrants");
        builder.setMessage(
                "This will send an update to " + cancelledProfiles.size() +
                        " cancelled entrants.\n\nDo you want to proceed?"
        );

        builder.setPositiveButton("Send", (dialog, which) -> fetchEventNameAndSend());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    /**
     * First fetch the event name so the message can include:
     *   Winter Gala: The organizer has sent you an update. Please go to the event page.
     */
    private void fetchEventNameAndSend() {
        sendNotificationButton.setEnabled(false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String eventName = "Event";
                    if (doc.exists()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null && e.getName() != null && !e.getName().trim().isEmpty()) {
                            eventName = e.getName();
                        }
                    }

                    String title = "📢 Event Update";
                    String message = eventName + ": You’ve been removed from the event due to a change in your registration status. "
                            + "Please visit the event page for more details.";

                    sendNotifications(title, message);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event name", e);
                    String title = "📢 Event Update";
                    String message = "You’ve been removed from the event due to a change in your registration status. Please visit the event page for more details.";
                    sendNotifications(title, message);
                });
    }

    /**
     * Perform notification send using NotificationServiceFs.
     */
    private void sendNotifications(String title, String message) {
        Log.d(TAG, "Sending notifications with title=" + title + " message=" + message);

        notificationService.sendNotificationToCancelledEntrants(
                eventId,
                cancelledProfiles,
                title,
                message,
                this::handleSendSuccess,
                this::handleSendFailure
        );
    }

    /**
     * Called when notifications were successfully delivered.
     */
    private void handleSendSuccess() {
        runOnUiThread(() -> {
            sendNotificationButton.setEnabled(true);
            Toast.makeText(
                    this,
                    "Notifications sent to " + cancelledProfiles.size() + " cancelled entrants!",
                    Toast.LENGTH_LONG
            ).show();
            Log.i(TAG, "Notifications sent successfully");
        });
    }

    /**
     * Called when any failure occurs during notification.
     */
    private void handleSendFailure(String error) {
        runOnUiThread(() -> {
            sendNotificationButton.setEnabled(true);
            Toast.makeText(this, "Failed to send notifications: " + error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Send failed: " + error);
        });
    }

    /**
     * Loads cancelled entrants from Firestore:
     *   events/{eventId}/registrations
     *   where status in [CANCELLED_BY_ORGANIZER, CANCELLED_BY_ENTRANT]
     */
    private void loadCancelledFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList("CANCELLED_BY_ORGANIZER", "CANCELLED_BY_ENTRANT"))
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "Found " + snap.size() + " cancelled entrants");

                    if (snap.isEmpty()) {
                        adapter.updateCancelledEntrants(new ArrayList<>(cancelledProfiles),
                                new ArrayList<>(cancelledStatuses));
                        updateCount();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        String userId;
                        if (doc.contains("userId"))      userId = doc.getString("userId");
                        else if (doc.contains("entrantId")) userId = doc.getString("entrantId");
                        else                                userId = doc.getId();

                        String entrantId = doc.getString("entrantId");
                        String userIdField = doc.getString("userId");

                        cancelledStatuses.add(doc.getString("status"));
                        loadProfileWithFallback(userId, entrantId, userIdField);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving cancelled entrants", e);
                    Toast.makeText(this, "Error loading cancelled entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Resolves profiles safely in this priority order:
     *   1. userId (trusted)
     *   2. entrantId
     *   3. deviceId (legacy)
     */
    private void loadProfileWithFallback(String userId, String entrantId, String userIdField) {

        profileRepo.get(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        profile.setUserId(userId);
                        addCancelledProfile(profile);
                        return;
                    }

                    if (entrantId != null && !entrantId.isEmpty()) {
                        profileRepo.get(entrantId)
                                .addOnSuccessListener(p2 -> {
                                    if (p2 != null) {
                                        p2.setUserId(entrantId);
                                        addCancelledProfile(p2);
                                        return;
                                    }

                                    if (userIdField != null && !userIdField.isEmpty()) {
                                        profileRepo.get(userIdField)
                                                .addOnSuccessListener(p3 -> {
                                                    if (p3 != null) {
                                                        p3.setUserId(userIdField);
                                                        addCancelledProfile(p3);
                                                        return;
                                                    }
                                                    loadProfileByDeviceId(userId);
                                                });
                                    } else {
                                        loadProfileByDeviceId(userId);
                                    }
                                });
                    } else loadProfileByDeviceId(userId);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Primary lookup failed for " + userId, e);
                    loadProfileByDeviceId(userId);
                });
    }

    /**
     * Legacy fallback for very old registrations using deviceId.
     */
    private void loadProfileByDeviceId(String deviceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot doc = snap.getDocuments().get(0);
                        Profile p = doc.toObject(Profile.class);

                        if (p != null) {
                            String realUid = doc.getString("userId");
                            if (realUid == null || realUid.isEmpty()) realUid = doc.getId();
                            p.setUserId(realUid);

                            addCancelledProfile(p);
                            return;
                        }
                    }

                    Log.w(TAG, "No profile found using deviceId fallback: " + deviceId);
                    adapter.updateCancelledEntrants(new ArrayList<>(cancelledProfiles),
                            new ArrayList<>(cancelledStatuses));
                    updateCount();
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "DeviceId fallback failed for " + deviceId, e));
    }

    /**
     * Adds a profile to the cancelled list and updates the adapter.
     */
    private void addCancelledProfile(Profile profile) {
        if (profile == null) return;

        String userId = profile.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            Log.e(TAG, "Skipping profile: missing userId");
            return;
        }

        for (Profile p : cancelledProfiles) {
            if (p.getUserId().equals(userId)) return;
        }

        cancelledProfiles.add(profile);
        Log.d(TAG, "Added cancelled profile: " + userId);

        adapter.updateCancelledEntrants(new ArrayList<>(cancelledProfiles),
                new ArrayList<>(cancelledStatuses));
        updateCount();
    }

    /**
     * Updates the UI text showing the number of cancelled entrants.
     */
    private void updateCount() {
        totalCountText.setText("Total cancelled entrants: " + cancelledProfiles.size());
        
        // Show/hide empty state
        if (cancelledProfiles.isEmpty()) {
            if (emptyStateText != null) emptyStateText.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            if (emptyStateText != null) emptyStateText.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }

    /**
     * Runs the replacement lottery:
     * 1. Fetches all users in not_selected
     * 2. Randomly selects new entrants equal to number of cancelled ones
     * 3. Adds them to chosen_list
     * 4. Creates Firestore invitation
     * 5. Sends event-aware replacement notification (Firestore + push)
     * 6. Removes them from not_selected
     */
    private void runReplacementLottery() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        int numCancelled = cancelledProfiles.size();
        if (numCancelled == 0) {
            Toast.makeText(this, "No cancelled entrants to replace.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch event name FIRST so all notifications use it
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {

                    String eventName = "Event";
                    if (eventDoc.exists()) {
                        Event e = eventDoc.toObject(Event.class);
                        if (e != null && e.getName() != null && !e.getName().trim().isEmpty()) {
                            eventName = e.getName();
                        }
                    }

                    final String finalEventName = eventName;

                    // Now fetch not_selected list
                    db.collection("events")
                            .document(eventId)
                            .collection("not_selected")
                            .get()
                            .addOnSuccessListener(notSelectedSnap -> {

                                if (notSelectedSnap.isEmpty()) {
                                    Toast.makeText(this, "No remaining entrants in not_selected.", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Convert to list and randomize
                                List<DocumentSnapshot> docs = new ArrayList<>(notSelectedSnap.getDocuments());
                                java.util.Collections.shuffle(docs);

                                // Pick replacements = number cancelled
                                List<DocumentSnapshot> replacements =
                                        docs.subList(0, Math.min(numCancelled, docs.size()));

                                List<Task<Void>> tasks = new ArrayList<>();

                                for (DocumentSnapshot doc : replacements) {

                                    String userId = doc.getId();
                                    WaitingListEntry entry = doc.toObject(WaitingListEntry.class);

                                    if (entry == null) continue;

                                    entry.setStatus("selected");

                                    // ---- 2. Add to chosen_list ----
                                    Task<Void> addChosen = db.collection("events")
                                            .document(eventId)
                                            .collection("chosen_list")
                                            .document(userId)
                                            .set(entry);
                                    tasks.add(addChosen);

                                    // ---- 3. Create invitation ----
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

                                    // ---- 4. Event-aware notification text ----
                                    String title = "🎉 You've been selected!";
                                    String message = finalEventName +
                                            ": A spot has opened up and you’ve been selected as a replacement. " +
                                            "Please go to the event page to respond to your invitation.";

                                    // ---- 5. Add Firestore notification record ----
                                    Map<String, Object> notif = new HashMap<>();
                                    notif.put("eventId", eventId);
                                    notif.put("recipientUserId", userId);
                                    notif.put("type", "LOTTERY_REPLACEMENT_SELECTED");
                                    notif.put("title", title);
                                    notif.put("message", message);
                                    notif.put("isRead", false);
                                    notif.put("sentAt", com.google.firebase.Timestamp.now());

                                    Task<Void> notifTask = db.collection("notifications")
                                            .add(notif)
                                            .continueWith(t -> null);

                                    tasks.add(notifTask);

                                    // ---- 6. Push notification via NotificationServiceFs ----
                                    profileRepo.get(userId)
                                            .addOnSuccessListener(profile -> {
                                                if (profile != null) {
                                                    profile.setUserId(userId);

                                                    notificationService.sendNotificationToSelectedEntrants(
                                                            eventId,
                                                            List.of(profile),
                                                            title,
                                                            message,
                                                            () -> Log.d(TAG, "Push sent to replacement " + userId),
                                                            (error) -> Log.e(TAG, "Push send failed for replacement " + userId + ": " + error)
                                                    );
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e(TAG,
                                                    "Failed profile fetch for replacement push: " + userId, e));

                                    // ---- 7. Remove from not_selected ----
                                    Task<Void> removeTask = db.collection("events")
                                            .document(eventId)
                                            .collection("not_selected")
                                            .document(userId)
                                            .delete();
                                    tasks.add(removeTask);
                                }

                                // When all tasks complete
                                com.google.android.gms.tasks.Tasks.whenAll(tasks)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this,
                                                    "Replacement lottery completed!",
                                                    Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(err -> {
                                            Toast.makeText(this,
                                                    "Error during replacement lottery.",
                                                    Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "Replacement lottery failed", err);
                                        });

                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load not_selected list.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error loading not_selected", e);
                            });

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event name.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch event name", e);
                });
    }

}
