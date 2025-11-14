package com.example.eventmaster.ui.entrant;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.MainActivity;
import com.example.eventmaster.R;
import com.example.eventmaster.ui.qr.QRScannerActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * Simple home screen for entrants.
 * - Browse Events
 * - Scan QR Code to view/join a specific event
 */
public class EntrantHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_home);

        // If you want a toolbar, you can add one to the XML and wire it here.
        MaterialToolbar topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            setSupportActionBar(topBar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            topBar.setNavigationOnClickListener(v -> finish());
        }

        MaterialButton btnViewEvents = findViewById(R.id.btnViewEvents);
        MaterialButton btnScanQr     = findViewById(R.id.btnScanQr);
        MaterialButton btnLogout     = findViewById(R.id.btnLogout);

        btnViewEvents.setOnClickListener(v -> {
            Intent i = new Intent(this, EventListActivity.class);
            startActivity(i);
        });

        btnScanQr.setOnClickListener(v -> {
            Intent i = new Intent(this, QRScannerActivity.class);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            AuthHelper.signOut();
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
