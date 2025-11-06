package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.waitinglist.WaitingListActivity;

import com.example.eventmaster.ui.entrant.EventDetailsActivity;
import com.example.eventmaster.utils.TestDataHelper;
import com.google.android.material.button.MaterialButton;

/**
 * Main launcher activity for testing.
 * Provides buttons to seed test data and navigate to event details.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListDemo";
    private WaitingListRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();
    }

    private void setupButtons() {
        MaterialButton viewEventListButton = findViewById(R.id.view_event_list_button);
        MaterialButton adminBrowseEventsButton = findViewById(R.id.admin_browse_events_button);
        MaterialButton scanQRButton = findViewById(R.id.scan_qr_button);

        // View Event List (US #8)
        viewEventListButton.setOnClickListener(v -> openEventList());

        // Admin Browse Events (US #45)
        adminBrowseEventsButton.setOnClickListener(v -> openAdminEventList());

        // Scan QR Code (US #20)
        scanQRButton.setOnClickListener(v -> openQRScanner());
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
