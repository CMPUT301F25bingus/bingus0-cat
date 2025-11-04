package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.entrant.EventDetailsActivity;
import com.example.eventmaster.utils.TestDataHelper;
import com.google.android.material.button.MaterialButton;

/**
 * Main launcher activity for testing.
 * Provides buttons to seed test data and navigate to event details.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
    }

    private void setupButtons() {
        MaterialButton seedDataButton = findViewById(R.id.seed_data_button);
        MaterialButton event1Button = findViewById(R.id.test_event1_button);
        MaterialButton event2Button = findViewById(R.id.test_event2_button);
        MaterialButton event3Button = findViewById(R.id.test_event3_button);
        MaterialButton viewEventListButton = findViewById(R.id.view_event_list_button);
        MaterialButton adminBrowseEventsButton = findViewById(R.id.admin_browse_events_button);
        MaterialButton scanQRButton = findViewById(R.id.scan_qr_button);

        // Seed test data
        seedDataButton.setOnClickListener(v -> {
            TestDataHelper testDataHelper = new TestDataHelper();
            testDataHelper.createSampleEvents();
            Toast.makeText(this, "Test data seeded! Check Firestore.", Toast.LENGTH_LONG).show();
        });

        // Test event 1 - Swimming Lessons (registration open)
        event1Button.setOnClickListener(v -> openEventDetails("test_event_1"));

        // Test event 2 - Piano Lessons (registration not yet open)
        event2Button.setOnClickListener(v -> openEventDetails("test_event_2"));

        // Test event 3 - Dance Class (registration open)
        event3Button.setOnClickListener(v -> openEventDetails("test_event_3"));

        // View Event List (US #8)
        viewEventListButton.setOnClickListener(v -> openEventList());

        // Admin Browse Events (US #45)
        adminBrowseEventsButton.setOnClickListener(v -> openAdminEventList());

        // Scan QR Code (US #20)
        scanQRButton.setOnClickListener(v -> openQRScanner());
    }

    private void openEventDetails(String eventId) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
    }

    private void openEventList() {
        Intent intent = new Intent(this, com.example.eventmaster.ui.entrant.EventListActivity.class);
        startActivity(intent);
    }

    private void openAdminEventList() {
        Intent intent = new Intent(this, com.example.eventmaster.ui.admin.AdminEventListActivity.class);
        startActivity(intent);
    }

    private void openQRScanner() {
        Intent intent = new Intent(this, com.example.eventmaster.ui.qr.QRScannerActivity.class);
        startActivity(intent);
    }
}