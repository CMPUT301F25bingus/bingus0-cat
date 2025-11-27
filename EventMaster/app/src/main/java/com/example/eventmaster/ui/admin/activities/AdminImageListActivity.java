package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.fragments.AdminImageListFragment;

/**
 * Activity that hosts the AdminImageListFragment.
 * Displays a list of all images uploaded to Firebase Storage for admin to browse and delete.
 */
public class AdminImageListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_image_list);

        // Load fragment
        if (savedInstanceState == null) {
            AdminImageListFragment fragment = AdminImageListFragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.commit();
        }
    }
}

