package com.example.eventmaster.ui.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.data.firestore.EventReadServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.admin.adapters.OrganizerEventsAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * Admin screen showing organizer profile details and events.
 * 
 * Responsibilities:
 * Loads organizer profile (name/email/phone/banned state)
 * Displays events created by the organizer
 * Allows admin to ban/unban the organizer with confirmation.
 */
public class AdminProfileDetailActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private final EventReadService eventRead = new EventReadServiceFs();

    private String profileId;

    // Profile views
    private TextView tvName, tvEmail, tvPhone, tvStatusBadge;
    private MaterialButton btnBan, btnViewNotificationLogs;

    // Events section
    private RecyclerView rvEvents;
    private OrganizerEventsAdapter eventsAdapter;
    private TextView tvEventsCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_profile_detail);

        profileId = getIntent().getStringExtra("profileId");

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Profile views
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        btnBan = findViewById(R.id.btnBan);
        btnViewNotificationLogs = findViewById(R.id.btnViewNotificationLogs);

        btnBan.setOnClickListener(v -> toggleBan());
        btnViewNotificationLogs.setOnClickListener(v -> openNotificationLogs());

        // Events list
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventsAdapter = new OrganizerEventsAdapter();
        rvEvents.setAdapter(eventsAdapter);
        
        tvEventsCount = findViewById(R.id.tvEventsCount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
        loadEvents();
    }

    private void loadProfile() {
        repo.get(profileId, this::bindProfile, e -> tvName.setText("Error loading profile"));
    }

    private void bindProfile(Profile p) {
        tvName.setText(ns(p.getName()));
        tvEmail.setText(ns(p.getEmail()));
        tvPhone.setText(ns(p.getPhone()));
        
        boolean banned = p.getBanned();
        if (banned) {
            tvStatusBadge.setText("BANNED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_banned);
            btnBan.setText("Unban Organizer");
        } else {
            tvStatusBadge.setText("Active");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_active);
            btnBan.setText("Ban Organizer");
        }
    }

    private void toggleBan() {
        repo.get(profileId, p -> {
            boolean next = !p.getBanned();
            new AlertDialog.Builder(this)
                    .setTitle(next ? "Ban organizer?" : "Unban organizer?")
                    .setMessage((next ? "Ban " : "Unban ") + ns(p.getName()) + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(next ? "Ban" : "Unban",
                            (d, w) -> repo.setBanned(profileId, next, v -> loadProfile(), err -> {}))
                    .show();
        }, e -> {});
    }

    private void loadEvents() {
        eventRead.listByOrganizer(profileId)
                .addOnSuccessListener(events -> {
                    if (events != null) {
                        eventsAdapter.replace(events);
                        // Update events count
                        tvEventsCount.setText(String.valueOf(events.size()));
                    } else {
                        eventsAdapter.replace(new ArrayList<>());
                        tvEventsCount.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    eventsAdapter.replace(new ArrayList<>());
                    tvEventsCount.setText("0");
                });
    }

    private void openNotificationLogs() {
        // Navigate to notification log activity filtered by this organizer
        Intent intent = new Intent(this, AdminNotificationLogActivity.class);
        intent.putExtra("organizerId", profileId);
        startActivity(intent);
    }

    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
}
