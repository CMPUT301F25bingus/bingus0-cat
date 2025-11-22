package com.example.eventmaster.ui.organizer.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
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
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for displaying selected entrants who won the lottery.
 * Allows organizers to send notifications to selected entrants inviting them to sign up.
 * 
 * Implements US 02.05.01: As an organizer I want to send a notification to chosen entrants.
 * 
 * Outstanding issues:
 * - Event ID and selected entrants are currently mocked for demonstration
 * - In production, these should be passed via Intent extras from the previous screen
 * - CSV export functionality is stubbed and needs implementation
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
    private MaterialToolbar backButtonContainer;
    private RecyclerView recyclerView;
    private TextView totalSelectedCount;
    private TextView sendNotificationButton;
    private TextView exportCsvButton;

    // Data
    private String eventId;
    private List<Profile> selectedEntrants;
    private SelectedEntrantsAdapter adapter;

    // Services
    private NotificationService notificationService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_selected_entrants);

        // Initialize service
        notificationService = new NotificationServiceFs();

        // Initialize UI components
        initializeViews();

        backButtonContainer.setNavigationOnClickListener(v -> finish());


        // Load data (from Intent or mock data for now)
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
        backButtonContainer = findViewById(R.id.back_button_container);
        recyclerView = findViewById(R.id.selected_entrants_recycler_view);
        totalSelectedCount = findViewById(R.id.total_selected_count);
        sendNotificationButton = findViewById(R.id.send_notification_button);
        exportCsvButton = findViewById(R.id.export_csv_button);
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

                    for (DocumentSnapshot doc : regSnap.getDocuments()) {

                        String deviceId = doc.getId(); // userId stored by entrant side

                        loadProfileForSelectedEntrant(deviceId);
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load selected entrants", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading selected entrants", e);
                });
    }

    private void loadProfileForSelectedEntrant(String deviceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 2) Find the matching profile by its deviceId field
        db.collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(profileSnap -> {

                    if (!profileSnap.isEmpty()) {
                        Profile profile = profileSnap.getDocuments().get(0).toObject(Profile.class);
                        selectedEntrants.add(profile);
                        Log.d(TAG, "Loaded profile for: " + profile.getName());
                    } else {
                        Log.w(TAG, "No profile found for deviceId: " + deviceId);
                    }

                    // Update the adapter each time a profile loads
                    adapter.updateEntrants(new ArrayList<>(selectedEntrants));
                    updateUI();

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed loading profile for " + deviceId, e);
                });
    }


    /**
     * Creates mock selected entrants data for demonstration purposes.
     * 
     * @return List of mock profiles
     */
    private List<Profile> createMockSelectedEntrants(@NonNull String eventId) {
        List<Profile> mockProfiles = new ArrayList<>();

        boolean shouldPopulate = Math.abs(eventId.hashCode()) % 2 == 1;
        if (!shouldPopulate) {
            return mockProfiles;
        }

        Profile profile = new Profile("user_001", "Bingus", "bingus@example.com");
        profile.setPhoneNumber("+1 (785) 534-1229");
        mockProfiles.add(profile);

        return mockProfiles;
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
        String title = "🎉 Congratulations! You've Been Selected!";
        String message = "Great news! You have been selected in the lottery for this event. " +
                "Please confirm your attendance within 48 hours to secure your spot.";

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

