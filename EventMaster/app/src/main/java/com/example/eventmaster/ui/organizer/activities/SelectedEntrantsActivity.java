package com.example.eventmaster.ui.organizer.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Activity for displaying selected entrants who won the lottery.
 * Allows organizers to send notifications to selected entrants inviting them to sign up.
 * 
 * Implements US 02.05.01: As an organizer I want to send a notification to chosen entrants.
 * CSV export functionality
 */
public class SelectedEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "SelectedEntrantsActivity";
    private static final String EXTRA_EVENT_ID = "event_id";
    private static final String EXTRA_SELECTED_ENTRANTS = "selected_entrants";

    /**
     * Creates an intent for launching the SelectedEntrantsActivity for a specific event.
     *
     * @param context The calling context.
     * @param eventId The ID of the event whose selected entrants should be displayed.
     * @return Configured {@link Intent}.
     */
    public static Intent createIntent(@NonNull Context context, @NonNull String eventId) {
        Intent intent = new Intent(context, SelectedEntrantsActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        return intent;
    }

    // UI Components
    private RecyclerView recyclerView;
    private TextView totalSelectedCount;
    private TextView sendNotificationButton;
    private TextView exportCsvButton;
    private TextView emptyStateText;

    // Data
    private String eventId;
    private List<Profile> selectedEntrants;
    private SelectedEntrantsAdapter adapter;

    // Services
    private NotificationService notificationService;
    private ProfileRepositoryFs profileRepo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_selected_entrants);

        // Initialize services
        notificationService = new NotificationServiceFs();
        profileRepo = new ProfileRepositoryFs();

        // Initialize UI components
        initializeViews();

        ImageView backArrow = findViewById(R.id.back_arrow);

        backArrow.setOnClickListener(v -> {
            Log.d(TAG, "Back arrow clicked");
            finish();
        });

        // Load data
        loadData();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Update UI with data
        updateUI();
    }

    /**
     * Initializes all view components.
     */
    private void initializeViews() {
        recyclerView = findViewById(R.id.selected_entrants_recycler_view);
        totalSelectedCount = findViewById(R.id.total_selected_count);
        sendNotificationButton = findViewById(R.id.send_notification_button);
        exportCsvButton = findViewById(R.id.export_csv_button);
        emptyStateText = findViewById(R.id.empty_state_text);
    }

    /**
     * Loads event data and selected entrants.
     */
    private void loadData() {
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getStringExtra("eventId");
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Invalid event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        selectedEntrants = new ArrayList<>();
        loadSelectedEntrantsFromFirestore();
        Log.d(TAG, "Loaded " + selectedEntrants.size() + " selected entrants for event: " + eventId);
    }

    private void loadSelectedEntrantsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Clear existing list before reloading to avoid duplicates
        selectedEntrants.clear();

        // 1) Load selected entrants (status = ACTIVE)
        db.collection("events")
                .document(eventId)
                .collection("registrations")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(regSnap -> {

                    if (regSnap.isEmpty()) {
                        Log.d(TAG, "No selected entrants found");
                        updateUI();
                        adapter.updateEntrants(selectedEntrants);
                        return;
                    }

                    Log.d(TAG, "Found " + regSnap.size() + " selected entrants");

                    // Track userIds we've already processed to avoid duplicates
                    Set<String> processedUserIds = new HashSet<>();
                    
                    for (DocumentSnapshot doc : regSnap.getDocuments()) {
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
                        
                        // Skip if we've already processed this userId (avoid duplicates)
                        if (processedUserIds.contains(userId)) {
                            Log.d(TAG, "Skipping duplicate registration for userId: " + userId + " (docId: " + doc.getId() + ")");
                            continue;
                        }
                        
                        processedUserIds.add(userId);
                        Log.d(TAG, "Found selected registration - docId: " + doc.getId() + ", userId: " + userId);

                        loadProfileForSelectedEntrant(userId);
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load selected entrants", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading selected entrants", e);
                });
    }

    private void loadProfileForSelectedEntrant(String userId) {

        // 1️⃣ Try document ID = userId (correct method for new accounts)
        profileRepo.get(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        finalizeSelectedProfile(userId, profile);
                        return;
                    }

                    Log.w(TAG, "(1) No profile under docId for UID: " + userId);
                    loadProfileByUserIdField(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "(1) Error loading docId profile for " + userId, e);
                    loadProfileByUserIdField(userId);
                });
    }

    private void loadProfileByUserIdField(String userId) {
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Profile profile = snap.getDocuments().get(0).toObject(Profile.class);
                        finalizeSelectedProfile(userId, profile);
                        return;
                    }

                    Log.w(TAG, "(2) No profile with userId field = " + userId);
                    loadProfileByDeviceId(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "(2) userId field lookup failed for " + userId, e);
                    loadProfileByDeviceId(userId);
                });
    }


    private void loadProfileByDeviceId(String deviceId) {
        FirebaseFirestore.getInstance()
                .collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Profile profile = snap.getDocuments().get(0).toObject(Profile.class);
                        finalizeSelectedProfile(deviceId, profile);
                        return;
                    }

                    Log.w(TAG, "(3) No profile found by deviceId fallback: " + deviceId);
                    adapter.updateEntrants(new ArrayList<>(selectedEntrants));
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "(3) deviceId lookup failed for: " + deviceId, e);
                    adapter.updateEntrants(new ArrayList<>(selectedEntrants));
                    updateUI();
                });
    }

    private void finalizeSelectedProfile(String userId, Profile profile) {
        if (profile == null) return;

        if (profile.getUserId() == null || !profile.getUserId().equals(userId)) {
            profile.setUserId(userId);
        }

        for (Profile p : selectedEntrants) {
            if (p.getUserId().equals(userId)) {
                Log.d(TAG, "Already added: " + userId);
                return;
            }
        }

        selectedEntrants.add(profile);
        Log.d(TAG, "✓ Added selected entrant: " + profile.getName());

        adapter.updateEntrants(new ArrayList<>(selectedEntrants));
        updateUI();
    }



    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        adapter = new SelectedEntrantsAdapter(selectedEntrants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Optional: Add item click listener
        adapter.setOnEntrantClickListener(profile -> {
            // Handle entrant item click (e.g., show details)
            Toast.makeText(this, "Clicked: " + profile.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Sets up click listeners for all interactive components.
     */
    private void setupClickListeners() {
        // Send Notification button (Main action for US 02.05.01)
        sendNotificationButton.setOnClickListener(v -> handleSendNotificationClick());

        // Export CSV button
        exportCsvButton.setOnClickListener(v -> handleExportCsvClick());
    }

    /**
     * Updates UI components with current data.
     */
    private void updateUI() {
        String countText = "Total selected entrants: " + selectedEntrants.size();
        totalSelectedCount.setText(countText);
        
        // Show/hide empty state
        if (selectedEntrants.isEmpty()) {
            if (emptyStateText != null) emptyStateText.setVisibility(android.view.View.VISIBLE);
            recyclerView.setVisibility(android.view.View.GONE);
        } else {
            if (emptyStateText != null) emptyStateText.setVisibility(android.view.View.GONE);
            recyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }

    /**
     * Handles back button click to return to previous screen.
     */
    private void handleBackButtonClick() {
        Log.d(TAG, "Back button clicked");
        finish(); // Close activity and return to previous screen
    }

    /**
     * Handles Send Notification button click.
     * Implements US 02.05.01 - Send notification to chosen entrants.
     */
    private void handleSendNotificationClick() {
        Log.d(TAG, "Send Notification button clicked");

        if (selectedEntrants.isEmpty()) {
            Toast.makeText(this, "No entrants to notify", Toast.LENGTH_SHORT).show();
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
        builder.setTitle("Send Notification to Selected Entrants");
        builder.setMessage("This will notify " + selectedEntrants.size() + 
                " entrants that they have been selected for the event.\n\n" +
                "Do you want to proceed?");

        builder.setPositiveButton("Send", (dialog, which) -> {
            sendNotificationToSelectedEntrants();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * Sends notification to all selected entrants.
     * Core implementation of US 02.05.01.
     */
    private void sendNotificationToSelectedEntrants() {
        // Show loading state
        sendNotificationButton.setEnabled(false);
        Toast.makeText(this, "Sending notifications...", Toast.LENGTH_SHORT).show();

        // Prepare notification content
        String title = "🎉 Congratulations! You've Been Enrolled!";
        String message = "Great news! You have been Enrolled in the lottery for this event.";

        // Send notifications
        notificationService.sendNotificationToSelectedEntrants(
                eventId,
                selectedEntrants,
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
                    selectedEntrants.size() + " entrants!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Successfully sent notifications to all selected entrants");
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
     * Handles Export CSV button click.
     * Implements US 02.06.05
     */

//    https://stackoverflow.com/questions/61279201/how-to-export-csv-file-android-studio
    private void handleExportCsvClick() {
        Log.d(TAG, "Export CSV button clicked");
        if (selectedEntrants == null || selectedEntrants.isEmpty()) {
            Toast.makeText(this, "No selected entrants to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String filename = "Event_" + eventId + "_SelectedEntrants.csv";

            // Build CSV content
            StringBuilder builder = new StringBuilder();
            builder.append("Name,Email,Phone,User ID\n");

            for (Profile p : selectedEntrants) {
                builder.append(safe(p.getName())).append(",");
                builder.append(safe(p.getEmail())).append(",");
                builder.append(safe(p.getPhoneNumber())).append(",");
                builder.append(safe(p.getUserId())).append("\n");
            }

            // Write file to Downloads folder
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloads, filename);

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(builder.toString().getBytes());
            fos.flush();
            fos.close();

            // Share the file
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Export CSV"));

            Toast.makeText(this, "CSV exported to Downloads", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to export CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }



    private String safe(String s) {
        return (s == null) ? "" : s.replace(",", " ");  // Avoid breaking CSV format
    }



}

