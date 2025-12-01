package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.fragments.AdminNotificationLogFragment;

/**
 * Activity that hosts the AdminNotificationLogFragment.
 * Displays a log of all notifications sent in the system for admin viewing.
 */
public class AdminNotificationLogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_notification_log);

        // Get organizerId and eventId from intent if provided
        String organizerId = getIntent().getStringExtra("organizerId");
        String eventId = getIntent().getStringExtra("eventId");

        // Load fragment
        if (savedInstanceState == null) {
            AdminNotificationLogFragment fragment = AdminNotificationLogFragment.newInstance(organizerId, eventId);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}

