package com.example.eventmaster.ui.admin.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.android.material.button.MaterialButton;

/**
 * Admin "hub" screen that shows the admin actions.
 * "Browse events" and "Browse images" are now wired.
 */
public class AdminBrowseActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_browse);

        MaterialButton btnOrganizers = findViewById(R.id.btnOrganizers);
        MaterialButton btnEvents = findViewById(R.id.btnEvents);
        MaterialButton btnProfiles = findViewById(R.id.btnProfiles);
        MaterialButton btnImages = findViewById(R.id.btnImages);
        MaterialButton btnNotifications = findViewById(R.id.btnNotifications);
        MaterialButton btnReviewApplications = findViewById(R.id.btnReviewApplications);

        btnOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseOrganizersActivity.class)));

        btnProfiles.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEntrantsActivity.class)));

        btnEvents.setOnClickListener(v ->
                startActivity(new Intent(this, AdminEventListActivity.class)));

        btnImages.setOnClickListener(v ->
                startActivity(new Intent(this, AdminImageListActivity.class)));

        btnNotifications.setOnClickListener(v ->
                startActivity(new Intent(this, AdminNotificationLogActivity.class)));

        if (btnReviewApplications != null) {
            btnReviewApplications.setOnClickListener(v ->
                    startActivity(new Intent(this, AdminReviewApplicationsActivity.class)));
        }
    }
}

