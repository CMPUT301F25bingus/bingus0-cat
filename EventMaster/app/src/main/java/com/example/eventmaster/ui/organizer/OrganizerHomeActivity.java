package com.example.eventmaster.ui.organizer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class OrganizerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_home);

        // --- Toolbar setup ---
        MaterialToolbar topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            topBar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // --- Buttons ---
        MaterialButton btnCreate = findViewById(R.id.btnCreateEvents);
        MaterialButton btnManage = findViewById(R.id.btnManageEvents);

        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerCreateEventActivity.class))
        );

        btnManage.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerManageEventsActivity.class))
        );
    }
}
