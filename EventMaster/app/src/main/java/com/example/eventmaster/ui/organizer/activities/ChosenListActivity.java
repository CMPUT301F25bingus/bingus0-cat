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
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
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
 * Activity for displaying all chosen entrants (lottery winners).
 * Entrants are loaded from "chosen_list", which in your database uses deviceId
 * as the document ID rather than the Firebase userId.
 *
 * This activity:
 * - Loads chosen entrants
 * - Resolves their profiles from profiles/
 * - Lets organizer send notifications to entrants
 * - Cancels pending invitations when necessary
 */
public class ChosenListActivity extends AppCompatActivity {

    private static final String TAG = "ChosenListActivity";

    private RecyclerView recyclerView;
    private ChosenListAdapter adapter;
    private TextView totalChosenText;
    private TextView textSendNotification;

    private String eventId;

    private final WaitingListRepositoryFs repo = new WaitingListRepositoryFs();
    private final RegistrationServiceFs registrationService = new RegistrationServiceFs();
    private final NotificationService notificationService = new NotificationServiceFs();
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

    private List<WaitingListEntry> currentChosenList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_chosen_list);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "Missing eventId");
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

        btnBack.setOnClickListener(v -> finish());

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
     * Loads entrants from events/{eventId}/chosen_list.
     * chosen_list uses deviceId as its document ID, so profile resolution must use deviceId matching.
     */
    private void loadChosenList(String eventId) {
        repo.getChosenList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                currentChosenList = entries;
                adapter.updateList(entries);
                totalChosenText.setText("Total chosen entrants: " + entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load chosen list", e);
                totalChosenText.setText("Failed to load chosen entrants");
            }
        });
    }

    /**
     * Confirmation dialog before sending notifications.
     */
    private void showSendNotificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Notification to Chosen Entrants");
        builder.setMessage(
                "This will notify " + currentChosenList.size() +
                        " entrants who have been chosen by the lottery.\n\n" +
                        "They will be reminded to visit the event page to respond to their invitation."
        );

        builder.setPositiveButton("Send", (dialog, which) -> sendNotificationToChosenEntrants());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

//    /**
//     * Loads profiles using the *embedded profile object* inside chosen_list,
//     * because chosen_list stores the REAL uid only inside profile.userId.
//     */
//    private void sendNotificationToChosenEntrants() {
//        if (currentChosenList.isEmpty()) {
//            Toast.makeText(this, "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        textSendNotification.setEnabled(false);
//        Toast.makeText(this, "Sending notifications...", Toast.LENGTH_SHORT).show();
//
//        List<Profile> profiles = new ArrayList<>();
//
//        for (WaitingListEntry entry : currentChosenList) {
//
//            // Extract REAL user profile (the embedded object)
//            Profile p = entry.getProfile();
//
//            if (p == null) {
//                Log.e(TAG, "Chosen entry missing embedded profile");
//                continue;
//            }
//
//            // Ensure correct userId
//            String uid = p.getUserId();
//            if (uid == null || uid.isEmpty()) {
//                uid = p.getId(); // fallback
//            }
//
//            p.setUserId(uid);
//            profiles.add(p);
//        }
//
//        if (profiles.isEmpty()) {
//            Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
//            textSendNotification.setEnabled(true);
//            return;
//        }
//
//        // Now send notifications using valid profiles
//        String title = "🎉 You've been chosen!";
//        String message = "Congratulations! You've been chosen in the lottery. "
//                + "Go to the event page to accept or decline your invite.";
//
//        notificationService.sendNotificationToSelectedEntrants(
//                eventId,
//                profiles,
//                title,
//                message,
//                () -> handleSendSuccess(profiles.size()),
//                err -> handleSendFailure(err)
//        );
//    }
    /**
     * Sends notifications to chosen entrants using the embedded profile
     * AND includes event name in the message.
     */
    private void sendNotificationToChosenEntrants() {

        if (currentChosenList.isEmpty()) {
            Toast.makeText(this, "No chosen entrants to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        textSendNotification.setEnabled(false);
        Toast.makeText(this, "Preparing notifications...", Toast.LENGTH_SHORT).show();

        // 1️⃣ Fetch event name first
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventDoc -> {

                    String eventName = "Event";
                    if (eventDoc.exists()) {
                        String name = eventDoc.getString("name");
                        if (name != null && !name.trim().isEmpty()) {
                            eventName = name;
                        }
                    }

                    // 2️⃣ Extract profiles from chosen_list entries
                    List<Profile> profiles = new ArrayList<>();

                    for (WaitingListEntry entry : currentChosenList) {
                        Profile p = entry.getProfile();

                        if (p == null) {
                            Log.e(TAG, "ChosenList entry missing embedded profile!");
                            continue;
                        }

                        String uid = p.getUserId();
                        if (uid == null || uid.isEmpty()) {
                            uid = p.getId();
                        }
                        p.setUserId(uid);

                        profiles.add(p);
                    }

                    if (profiles.isEmpty()) {
                        Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                        textSendNotification.setEnabled(true);
                        return;
                    }

                    // 3️⃣ Build event-aware message
                    String title = "🎉 " + eventName + " — You've Been Chosen!";
                    String message =
                            "Congratulations! You have been selected in the lottery for " + eventName +
                                    ". Please visit the event page to accept or decline your invitation.";

                    // 4️⃣ Send notifications
                    notificationService.sendNotificationToSelectedEntrants(
                            eventId,
                            profiles,
                            title,
                            message,
                            () -> handleSendSuccess(profiles.size()),
                            err -> handleSendFailure(err)
                    );

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event name", e);
                    textSendNotification.setEnabled(true);
                    Toast.makeText(this, "Failed to load event info", Toast.LENGTH_SHORT).show();
                });
    }



    /**
     * Loads profiles for all entries in chosen_list.
     * chosen_list uses deviceId as the docId, so we must resolve:
     * deviceId -> profiles.where(deviceId == entryId) -> real UID
     */
    private void loadProfilesAndSendNotifications(List<String> ids) {
        List<Profile> profiles = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger(0);
        int total = ids.size();

        if (total == 0) {
            textSendNotification.setEnabled(true);
            Toast.makeText(this, "No users to notify", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String id : ids) {
            resolveProfile(id, profile -> {
                if (profile != null) profiles.add(profile);

                if (completed.incrementAndGet() == total) {
                    if (profiles.isEmpty()) {
                        Toast.makeText(this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                        textSendNotification.setEnabled(true);
                    } else {
                        sendNotificationsToProfiles(profiles);
                    }
                }
            });
        }
    }

    /**
     * Resolves a profile from a chosen_list entry ID.
     * chosen_list uses deviceId as doc ID, so the correct resolution order is:
     * 1. Try as a real Firebase UID
     * 2. Try profiles where deviceId == entryId
     */
    private void resolveProfile(String entryId, ProfileCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        profileRepo.get(entryId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        profile.setUserId(entryId);
                        Log.d(TAG, "Resolved profile by UID: " + entryId);
                        callback.onProfileResolved(profile);
                    } else {
                        db.collection("profiles")
                                .whereEqualTo("deviceId", entryId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(snap -> {
                                    if (!snap.isEmpty()) {
                                        DocumentSnapshot doc = snap.getDocuments().get(0);
                                        Profile p = doc.toObject(Profile.class);

                                        if (p != null) {
                                            String realUid = doc.getString("userId");
                                            if (realUid == null || realUid.isEmpty())
                                                realUid = doc.getId();

                                            p.setUserId(realUid);
                                            Log.d(TAG, "Resolved profile by deviceId: " + entryId + " → " + realUid);
                                            callback.onProfileResolved(p);
                                            return;
                                        }
                                    }

                                    Log.e(TAG, "No profile resolved for " + entryId);
                                    callback.onProfileResolved(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "deviceId lookup failed for " + entryId, e);
                                    callback.onProfileResolved(null);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "UID lookup failed for " + entryId, e);
                    callback.onProfileResolved(null);
                });
    }

    /**
     * Sends notifications to chosen entrant profiles.
     */
    private void sendNotificationsToProfiles(List<Profile> profiles) {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String eventName = "Event";
                    if (doc.exists()) {
                        Event e = doc.toObject(Event.class);
                        if (e != null && e.getName() != null) {
                            eventName = e.getName();
                        }
                    }

                    String title = "🎉 You've been selected!";
                    String message = eventName +
                            ": You have been chosen in the lottery. Please visit the event page to accept or decline your invitation.";

                    notificationService.sendNotificationToSelectedEntrants(
                            eventId,
                            profiles,
                            title,
                            message,
                            () -> handleSendSuccess(profiles.size()),
                            error -> handleSendFailure(error)
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event name", e);

                    String title = "🎉 You've been selected!";
                    String message = "Please visit the event page to accept or decline your invitation.";

                    notificationService.sendNotificationToSelectedEntrants(
                            eventId,
                            profiles,
                            title,
                            message,
                            () -> handleSendSuccess(profiles.size()),
                            this::handleSendFailure
                    );
                });
    }

    private void handleSendSuccess(int count) {
        runOnUiThread(() -> {
            textSendNotification.setEnabled(true);
            Toast.makeText(this, "Notifications sent to " + count + " chosen entrants!", Toast.LENGTH_LONG).show();
        });
    }

    private void handleSendFailure(String error) {
        runOnUiThread(() -> {
            textSendNotification.setEnabled(true);
            Toast.makeText(this, "Failed to send notifications: " + error, Toast.LENGTH_LONG).show();
        });
    }

    private interface ProfileCallback {
        void onProfileResolved(Profile profile);
    }
}
