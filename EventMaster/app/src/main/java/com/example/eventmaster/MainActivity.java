package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.ui.admin.activities.AdminWelcomeActivity;
import com.example.eventmaster.ui.entrant.activities.EventListActivity;
import com.example.eventmaster.ui.organizer.activities.OrganizerHomeActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Simple launcher screen for demoing all roles:
 * - Admin
 * - Organizer
 * - Entrant
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_activity_main_roles);

        MaterialButton btnAdmin = findViewById(R.id.btnAdmin);
        MaterialButton btnOrganizer = findViewById(R.id.btnOrganizer);
        MaterialButton btnEntrant = findViewById(R.id.btnEntrant);

        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminWelcomeActivity.class)));

        btnOrganizer.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerHomeActivity.class)));

        btnEntrant.setOnClickListener(v ->
                startActivity(new Intent(this, EventListActivity.class)));
    }
}
