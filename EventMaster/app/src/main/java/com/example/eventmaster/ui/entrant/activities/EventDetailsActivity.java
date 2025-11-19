package com.example.eventmaster.ui.entrant.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.entrant.fragments.EventDetailsFragment;

/**
 * Activity that hosts the EventDetailsFragment.
 * Used to display event details and allow joining waiting list.
 */
public class EventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_event_details);

        // Get event ID from intent
        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);

        if (eventId == null) {
            // For testing, use a default event ID
            eventId = "test_event_1";
        }

        // Load fragment
        if (savedInstanceState == null) {
            EventDetailsFragment fragment = EventDetailsFragment.newInstance(eventId);
            // Forward TEST flags to fragment
            Bundle args = fragment.getArguments() != null ? fragment.getArguments() : new Bundle();
            args.putBoolean("TEST_MODE",
                    getIntent().getBooleanExtra("TEST_MODE", false));
            args.putBoolean("TEST_FORCE_INVITED",
                    getIntent().getBooleanExtra("TEST_FORCE_INVITED", false));
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();

//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            transaction.replace(R.id.fragment_container, fragment);
//            transaction.commit();
        }
    }
}

