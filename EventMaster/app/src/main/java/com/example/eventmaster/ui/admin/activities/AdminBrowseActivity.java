package com.example.eventmaster.ui.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.OrganizerApplicationRepositoryFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;

/**
 * Admin dashboard screen with 3 main category cards.
 * Shows counts for events, users, and pending applications.
 */
public class AdminBrowseActivity extends AppCompatActivity {
    
    private EventRepository eventRepo;
    private ProfileRepositoryFs profileRepo;
    private FirebaseFirestore db;
    
    private TextView tvEventsCount;
    private TextView tvUsersCount;
    private TextView tvApplicationsCount;
    
    private MaterialCardView cardManageEvents;
    private MaterialCardView cardUsersOrganizers;
    private MaterialCardView cardReviewApplications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_browse);

        eventRepo = new EventRepositoryFs();
        profileRepo = new ProfileRepositoryFs();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        cardManageEvents = findViewById(R.id.cardManageEvents);
        cardUsersOrganizers = findViewById(R.id.cardUsersOrganizers);
        cardReviewApplications = findViewById(R.id.cardReviewApplications);
        
        tvEventsCount = findViewById(R.id.tvEventsCount);
        tvUsersCount = findViewById(R.id.tvUsersCount);
        tvApplicationsCount = findViewById(R.id.tvApplicationsCount);

        // Set up click listeners
        setupClickListeners();
        
        // Load counts
        loadCounts();
    }

    private void setupClickListeners() {
        // Manage Events card
        cardManageEvents.setOnClickListener(v ->
                startActivity(new Intent(this, AdminEventListActivity.class)));

        // Users & Organizers card - Navigate to BrowseEntrantsActivity for now
        // TODO: Create unified BrowseUsersActivity with role filtering
        cardUsersOrganizers.setOnClickListener(v ->
                startActivity(new Intent(this, BrowseEntrantsActivity.class)));

        // Review Applications card
        cardReviewApplications.setOnClickListener(v ->
                startActivity(new Intent(this, AdminReviewApplicationsActivity.class)));
    }

    private void loadCounts() {
        loadEventsCount();
        loadUsersCount();
        loadApplicationsCount();
    }

    private void loadEventsCount() {
        eventRepo.getAllEvents()
                .addOnSuccessListener(events -> {
                    int count = events != null ? events.size() : 0;
                    tvEventsCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminBrowse", "Failed to load events count", e);
                    tvEventsCount.setText("0");
                });
    }

    private void loadUsersCount() {
        // Get total count of all active users (entrants + organizers, excluding admins)
        // We need to query separately for entrants and organizers since Firestore doesn't support OR queries
        db.collection("profiles")
                .whereEqualTo("role", "entrant")
                .get()
                .addOnSuccessListener(entrantSnapshot -> {
                    int entrantCount = entrantSnapshot != null ? entrantSnapshot.size() : 0;
                    // Now get organizers
                    db.collection("profiles")
                            .whereEqualTo("role", "organizer")
                            .get()
                            .addOnSuccessListener(organizerSnapshot -> {
                                int organizerCount = organizerSnapshot != null ? organizerSnapshot.size() : 0;
                                int totalCount = entrantCount + organizerCount;
                                // Format count: if >= 1000, show as "1.2K", otherwise show number
                                String formattedCount = formatCount(totalCount);
                                tvUsersCount.setText(formattedCount);
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("AdminBrowse", "Failed to load organizers count", e);
                                // Still show entrant count
                                String formattedCount = formatCount(entrantCount);
                                tvUsersCount.setText(formattedCount);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminBrowse", "Failed to load users count", e);
                    tvUsersCount.setText("0");
                });
    }

    private void loadApplicationsCount() {
        // Count pending organizer applications
        OrganizerApplicationRepositoryFs repo = new OrganizerApplicationRepositoryFs();
        repo.getPendingApplications()
                .addOnSuccessListener(apps -> {
                    int count = apps != null ? apps.size() : 0;
                    tvApplicationsCount.setText(String.valueOf(count));
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminBrowse", "Failed to load applications count", e);
                    tvApplicationsCount.setText("0");
                });
    }

    private String formatCount(int count) {
        if (count >= 1000) {
            double kCount = count / 1000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(kCount) + "K";
        }
        return String.valueOf(count);
    }
}
