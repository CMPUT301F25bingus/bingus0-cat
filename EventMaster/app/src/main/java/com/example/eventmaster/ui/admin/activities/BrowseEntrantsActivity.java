package com.example.eventmaster.ui.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.admin.adapters.EntrantAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * Admin screen to browse User profiles (Entrants and Organizers).
 * Includes a filter bar to switch between Users and Organizers.
 *
 * Responsibilities:
 *  - Subscribes to Firestore profiles stream and renders a list.
 *  - Allows admin to switch between viewing Users (entrants) and Organizers.
 *  - Allows admin to remove a profile (hard delete) with confirmation.
 *
 * Notes:
 *  - Relies on ProfileRepositoryFs.listenEntrants() and listenOrganizers().
 *  - Deleting will be reflected by the snapshot listener (no manual remove needed).
 */
public class BrowseEntrantsActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private ListenerRegistration reg;
    private EntrantAdapter adapter;
    
    private MaterialButton btnUsers;
    private MaterialButton btnOrganizers;
    private boolean showingUsers = true; // true = Users, false = Organizers

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_browse_entrants);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        // Initialize filter buttons
        btnUsers = findViewById(R.id.btnUsers);
        btnOrganizers = findViewById(R.id.btnOrganizers);
        
        // Set up filter button click listeners
        btnUsers.setOnClickListener(v -> switchToUsers());
        btnOrganizers.setOnClickListener(v -> switchToOrganizers());

        RecyclerView rv = findViewById(R.id.rvProfiles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EntrantAdapter(
                new ArrayList<>(),
                p -> {
                    // Open profile detail - for organizers, show full detail page; for users, just remove for now
                    if (!showingUsers) {
                        // Organizer clicked - open detail page
                        Intent intent = new Intent(this, AdminProfileDetailActivity.class);
                        intent.putExtra("profileId", p.getId());
                        startActivity(intent);
                    }
                },
                (pos, p) -> {
                    if (p.getId() == null) return;
                    
                    String roleType = showingUsers ? "user" : "organizer";
                    new MaterialAlertDialogBuilder(BrowseEntrantsActivity.this)
                            .setTitle("Remove " + roleType + "?")
                            .setMessage("Are you sure you want to permanently remove this " + roleType + "?")
                            .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                            .setPositiveButton("Remove", (d, w) -> {
                                repo.delete(
                                        p.getId(),
                                        v -> Snackbar.make(findViewById(android.R.id.content),
                                                capitalize(roleType) + " removed", Snackbar.LENGTH_SHORT).show(),
                                        e -> Snackbar.make(findViewById(android.R.id.content),
                                                "Failed to remove: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                                );
                            })
                            .show();
                }
        );

        rv.setAdapter(adapter);

        View vm = findViewById(R.id.tvViewMore);
        if (vm != null) vm.setVisibility(View.GONE);
    }

    private void switchToUsers() {
        if (showingUsers) return; // Already showing users
        
        showingUsers = true;
        updateButtonStyles();
        updateListener();
    }

    private void switchToOrganizers() {
        if (!showingUsers) return; // Already showing organizers
        
        showingUsers = false;
        updateButtonStyles();
        updateListener();
    }

    private void updateButtonStyles() {
        if (showingUsers) {
            // Users button is active
            btnUsers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.color_prof_primary, getTheme())));
            btnUsers.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
            btnUsers.setElevation(4f);
            
            // Organizers button is inactive
            btnOrganizers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.status_badge_bg, getTheme())));
            btnOrganizers.setTextColor(getResources().getColor(R.color.text_primary_dark, getTheme()));
            btnOrganizers.setElevation(2f);
        } else {
            // Organizers button is active
            btnOrganizers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.color_prof_primary, getTheme())));
            btnOrganizers.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
            btnOrganizers.setElevation(4f);
            
            // Users button is inactive
            btnUsers.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.status_badge_bg, getTheme())));
            btnUsers.setTextColor(getResources().getColor(R.color.text_primary_dark, getTheme()));
            btnUsers.setElevation(2f);
        }
    }

    private void updateListener() {
        // Remove existing listener
        if (reg != null) {
            reg.remove();
            reg = null;
        }
        
        // Start new listener based on current filter
        if (showingUsers) {
            reg = repo.listenEntrants(
                    list -> adapter.replace(list),
                    err -> {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Failed to load users.", Snackbar.LENGTH_LONG).show();
                    });
        } else {
            reg = repo.listenOrganizers(
                    list -> adapter.replace(list),
                    err -> {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Failed to load organizers.", Snackbar.LENGTH_LONG).show();
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateButtonStyles(); // Set initial button styles
        updateListener(); // Start the initial listener
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }
    
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
