package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.entrant.EntrantNotificationsActivity;
import com.example.eventmaster.ui.organizer.SelectedEntrantsActivity;
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
        // Organizer: Selected Entrants button
        Button organizerButton = findViewById(R.id.test_button_organizer);
        organizerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectedEntrantsActivity.class);
            startActivity(intent);
        });

        // Entrant: Notifications button
        Button entrantButton = findViewById(R.id.test_button_entrant);
        entrantButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EntrantNotificationsActivity.class);
            startActivity(intent);
        });

        // Existing Material Buttons
        MaterialButton viewEventListButton = findViewById(R.id.view_event_list_button);
        MaterialButton adminBrowseEventsButton = findViewById(R.id.admin_browse_events_button);
        MaterialButton scanQRButton = findViewById(R.id.scan_qr_button);

        viewEventListButton.setOnClickListener(v -> openEventList());
        adminBrowseEventsButton.setOnClickListener(v -> openAdminEventList());
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
