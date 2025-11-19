package com.example.eventmaster.ui.admin.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.android.material.button.MaterialButton;

/**
 * Simple welcome screen for Admin flow.
 * Shows a "Continue" button that navigates to the AdminBrowseActivity hub.
 */
public class AdminWelcomeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_welcome);

        MaterialButton btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBrowseActivity.class)));
    }
}

