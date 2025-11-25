package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.fragments.AdminEventDetailsFragment;

/**
 * Activity that hosts the AdminEventDetailsFragment.
 * Displays read-only event details for admin viewing.
 */
public class AdminEventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_event_details);

        // Get event ID from intent
        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null) {
            finish();
            return;
        }

        // Load fragment
        if (savedInstanceState == null) {
            AdminEventDetailsFragment fragment = AdminEventDetailsFragment.newInstance(eventId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}

