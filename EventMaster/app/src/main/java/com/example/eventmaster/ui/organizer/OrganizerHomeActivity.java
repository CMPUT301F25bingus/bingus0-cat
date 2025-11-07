package com.example.eventmaster.ui.organizer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/**
 * OrganizerHomeActivity
 *
 * Role:
 *  - Main home screen for organizers.
 *  - Provides navigation to create new events or manage existing ones.
 *
 * Design Pattern:
 *  - Controller/View in MVC architecture.
 *
 * Outstanding Issues:
 *  - None known.
 */
public class OrganizerHomeActivity extends AppCompatActivity {

    /**
     * Sets up the organizer home screen, initializes toolbar and navigation buttons.
     *
     * @param savedInstanceState previously saved state bundle, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        // --- Buttons ---
        MaterialButton btnCreate = findViewById(R.id.btnCreateEvents);
        MaterialButton btnManage = findViewById(R.id.btnManageEvents);

        // Navigate to event creation screen
        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerCreateEventActivity.class))
        );

        // Navigate to manage events screen
        btnManage.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerManageEventsActivity.class))
        );
    }
}
