package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.fragments.AdminEventListFragment;

/**
 * Activity that hosts the AdminEventListFragment.
 * Displays a list of all events for admin to browse and manage.
 */
public class AdminEventListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_event_list);

        // Load fragment
        if (savedInstanceState == null) {
            AdminEventListFragment fragment = AdminEventListFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}

