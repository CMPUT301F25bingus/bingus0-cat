package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
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
    private MaterialToolbar backButton;

    private final List<Profile> cancelledProfiles = new ArrayList<>();
    private final List<String> cancelledStatuses = new ArrayList<>();
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

        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.textDrawReplacement).setOnClickListener(v -> {
            runReplacementLottery();
        });


        TextView title = findViewById(R.id.cancelledEntrantsTitle);
        if (title != null) title.setText("Cancelled Entrants");

        loadCancelledFromFirestore();
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
                        String deviceId = doc.getId();
                        String status = doc.getString("status");

                        cancelledStatuses.add(status);
                        loadProfile(deviceId);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving cancelled entrants", e);
                    Toast.makeText(this, "Error loading cancelled entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads a single cancelled entrant’s profile using their deviceId.
     * When the profile is loaded successfully, the UI list is updated.
     *
     * @param deviceId The user’s deviceId and Firestore document ID.
     */
    private void loadProfile(String deviceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        Profile p = snap.getDocuments().get(0).toObject(Profile.class);
                        cancelledProfiles.add(p);
                    }

                    adapter.updateCancelledEntrants(
                            new ArrayList<>(cancelledProfiles),
                            new ArrayList<>(cancelledStatuses)
                    );

                    updateCount();

                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed loading profile for " + deviceId, e)
                );
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

                        // 5. Remove from not_selected
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

}
