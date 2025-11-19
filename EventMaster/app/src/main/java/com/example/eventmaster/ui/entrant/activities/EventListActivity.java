package com.example.eventmaster.ui.entrant.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.entrant.fragments.EventListFragment;

/**
 * Activity that hosts the EventListFragment.
 * Displays a list of all available events for entrants to browse and join.
 */
public class EventListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_event_list);

        // Load fragment
        if (savedInstanceState == null) {
            EventListFragment fragment = EventListFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}

