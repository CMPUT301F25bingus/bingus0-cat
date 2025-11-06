package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.utils.TestDataHelper;
import com.google.android.material.button.MaterialButton;

/**
 * Main launcher activity for testing.
 * - Seeds test events into Firestore
 * - Opens Organizer flow so you can test Entrants UI and event management
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Adjust padding for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupButtons();

        // ✅ optional: seed test data into Firestore once
//        TestDataHelper helper = new TestDataHelper();
//        helper.createSampleEvents();
//        helper.createSelectedEntrantForSwimmingEvent();
//
//        Toast.makeText(this, "Seeded sample events for testing", Toast.LENGTH_SHORT).show();

        // handle back gestures gracefully
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                finishAffinity(); // exit app cleanly
            }
        });
    }

    private void setupButtons() {
        MaterialButton viewEventListButton = findViewById(R.id.view_event_list_button);
        MaterialButton adminBrowseEventsButton = findViewById(R.id.admin_browse_events_button);
        MaterialButton scanQRButton = findViewById(R.id.scan_qr_button);

        // For testing, repurpose "View Event List" to go directly to OrganizerManageEventsActivity
        viewEventListButton.setText("Organizer Mode (Test)");
        viewEventListButton.setOnClickListener(v -> openOrganizerEvents());

        // keep the others as stubs
        adminBrowseEventsButton.setOnClickListener(v ->
                Toast.makeText(this, "Admin section not wired yet", Toast.LENGTH_SHORT).show());

        scanQRButton.setOnClickListener(v ->
                Toast.makeText(this, "QR scanner not wired yet", Toast.LENGTH_SHORT).show());
    }

    private void openOrganizerEvents() {
        Intent intent = new Intent(this, com.example.eventmaster.ui.organizer.OrganizerManageEventsActivity.class);
        startActivity(intent);
    }
}

//package com.example.eventmaster;
//
//import android.content.Intent;
//import android.os.Bundle;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.example.eventmaster.ui.entrant.EventDetailsActivity;
//import com.example.eventmaster.utils.TestDataHelper;
//import com.google.android.material.button.MaterialButton;
//
///**
// * Main launcher activity for testing.
// * Provides buttons to seed test data and navigate to event details.
// */
//public class MainActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_main);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        setupButtons();
//    }
//
//    private void setupButtons() {
//        MaterialButton viewEventListButton = findViewById(R.id.view_event_list_button);
//        MaterialButton adminBrowseEventsButton = findViewById(R.id.admin_browse_events_button);
//        MaterialButton scanQRButton = findViewById(R.id.scan_qr_button);
//
//        // View Event List (US #8)
//        viewEventListButton.setOnClickListener(v -> seedThenOpenEvents());
//
//        // Admin Browse Events (US #45)
//        adminBrowseEventsButton.setOnClickListener(v -> openAdminEventList());
//
//        // Scan QR Code (US #20)
//        scanQRButton.setOnClickListener(v -> openQRScanner());
//    }
//
//    private void openEventList() {
//        Intent intent = new Intent(this, com.example.eventmaster.ui.entrant.EventListActivity.class);
//        startActivity(intent);
//    }
//
//    private void seedThenOpenEvents() {
//        TestDataHelper helper = new TestDataHelper();
//
//        // 1) Seed the three events
//        helper.createSampleEvents();
//
//        // 2) Seed a selected entrant for THIS device so Accept/Decline shows
//        String entrantId = com.example.eventmaster.utils.DeviceUtils.getDeviceId(this);
//        helper.createSelectedEntrantForSwimmingEvent(entrantId);
//
//        // 3) Small delay is usually enough for dev (or chain success listeners if you want strict order)
//        //    To strictly wait, move `startActivity` into the addOnSuccessListener calls above.
//        //    For dev convenience:
//        getWindow().getDecorView().postDelayed(() -> {
//            // open the list so you see all three seeded events
//            openEventList();
//        }, 500); // 0.5s dev delay; adjust or chain strictly with listeners if needed
//    }
//
//
//    private void openAdminEventList() {
//        Intent intent = new Intent(this, com.example.eventmaster.ui.admin.AdminEventListActivity.class);
//        startActivity(intent);
//    }
//
//    private void openQRScanner() {
//        Intent intent = new Intent(this, com.example.eventmaster.ui.qr.QRScannerActivity.class);
//        startActivity(intent);
//    }
//}