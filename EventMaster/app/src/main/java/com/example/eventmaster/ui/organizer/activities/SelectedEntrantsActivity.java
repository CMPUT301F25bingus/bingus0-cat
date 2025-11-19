package com.example.eventmaster.ui.organizer.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.*;

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
    private LinearLayout backButtonContainer;
    private RecyclerView recyclerView;
    private TextView totalSelectedCount;
    private TextView sendNotificationButton;
    private TextView exportCsvButton;
    private TextView viewEnrolledLink;
    private TextView cancelEntrantsLink;

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
        viewEnrolledLink = findViewById(R.id.view_enrolled_link);
        cancelEntrantsLink = findViewById(R.id.cancel_entrants_link);
    }

    /**
     * Loads event data and selected entrants.
     * Currently uses mock data for demonstration.
     * In production, data should come from Intent extras.
     */
    private void loadData() {
        Intent intent = getIntent();
        if (intent != null) {
            eventId = intent.getStringExtra(EXTRA_EVENT_ID);
        }

        if (eventId == null || eventId.isEmpty()) {
            eventId = "event_123";
        }

        selectedEntrants = createMockSelectedEntrants(eventId);

        Log.d(TAG, "Loaded " + selectedEntrants.size() + " selected entrants for event: " + eventId);
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
        // Back button
        backButtonContainer.setOnClickListener(v -> handleBackButtonClick());

        // Send Notification button (Main action for US 02.05.01)
        sendNotificationButton.setOnClickListener(v -> handleSendNotificationClick());

        // Export CSV button
        exportCsvButton.setOnClickListener(v -> handleExportCsvClick());

        // View enrolled link
        viewEnrolledLink.setOnClickListener(v -> handleViewEnrolledClick());

        // Cancel entrants link
        cancelEntrantsLink.setOnClickListener(v -> handleCancelEntrantsClick());
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
     * Implements US 02.06.05 (stubbed for now).
     */
    private void handleExportCsvClick() {
        Log.d(TAG, "Export CSV button clicked");
        Toast.makeText(this, "Export CSV functionality - Coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement CSV export using CSVExportService
    }

    /**
     * Handles View Enrolled link click.
     * Shows list of entrants who have confirmed attendance.
     */
    private void handleViewEnrolledClick() {
        Log.d(TAG, "View enrolled link clicked");
        Toast.makeText(this, "View enrolled entrants - Coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to enrolled entrants screen
    }

    /**
     * Handles Cancel Entrants link click.
     * Allows cancelling entrants who haven't signed up.
     */
    private void handleCancelEntrantsClick() {
        Log.d(TAG, "Cancel entrants link clicked");
        Toast.makeText(this, "Cancel entrants functionality - Coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement cancel entrants functionality
    }
}

