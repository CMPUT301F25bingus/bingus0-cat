package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.MainActivity;
import com.example.eventmaster.R;
import com.example.eventmaster.ui.entrant.activities.EntrantWelcomeActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Success screen shown after user submits organizer application.
 * Provides options to check status, go back to login, or continue as entrant.
 */
public class ApplicationSubmittedActivity extends AppCompatActivity {

    private String applicantEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_submitted);

        // Get email from intent (passed from ApplyOrganizerActivity)
        applicantEmail = getIntent().getStringExtra("applicantEmail");

        MaterialButton btnCheckStatus = findViewById(R.id.btnCheckStatus);
        MaterialButton btnBackToLogin = findViewById(R.id.btnBackToLogin);
        MaterialButton btnContinueEntrant = findViewById(R.id.btnContinueEntrant);

        // Check status button - opens status check dialog
        btnCheckStatus.setOnClickListener(v -> {
            // Pass email to SharedLoginActivity which will show status dialog
            Intent intent = new Intent(this, SharedLoginActivity.class);
            intent.putExtra("checkStatusEmail", applicantEmail);
            startActivity(intent);
            finish();
        });

        // Back to login button
        btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, SharedLoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Continue as entrant button
        btnContinueEntrant.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}

