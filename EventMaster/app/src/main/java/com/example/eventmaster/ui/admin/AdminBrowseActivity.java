package com.example.eventmaster.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.profiles.BrowseEntrantsActivity;
import com.example.eventmaster.ui.admin.profiles.BrowseOrganizersActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Admin "hub" screen that shows the four admin actions.
 * Only "Browse organizers" and "Browse profiles" are wired.
 * The other two show a placeholder toast.
 */
public class AdminBrowseActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse);

        MaterialButton btnOrganizers = findViewById(R.id.btnOrganizers);
        MaterialButton btnEvents = findViewById(R.id.btnEvents);
        MaterialButton btnProfiles = findViewById(R.id.btnProfiles);
        MaterialButton btnNotifications = findViewById(R.id.btnNotifications);

        btnOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseOrganizersActivity.class)));

        btnProfiles.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEntrantsActivity.class)));

        btnEvents.setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Browse Events not implemented", android.widget.Toast.LENGTH_SHORT).show());

        btnNotifications.setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Notifications not implemented", android.widget.Toast.LENGTH_SHORT).show());
    }
}
