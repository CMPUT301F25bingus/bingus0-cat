package com.example.eventmaster.ui.admin.profiles;

import android.os.Bundle;
import android.widget.Button;
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

/**
 * Admin screen listing organizers (live Firestore stream).
 * Taps open AdminProfileDetailActivity.
 *
 * Responsibilities:
 * Loads organizer profile (name/email/phone/banned state)
 * Allows admin to ban/unban the organizer with confirmation.
 * Lists events owned by the organizer via {@link EventReadService}.
 */
public class AdminProfileDetailActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private final EventReadService eventRead = new EventReadServiceFs();

    private String profileId;

    // header fields
    private TextView tvName, tvEmail, tvPhone, tvState;
    private Button btnBan;

    // events section
    private RecyclerView rvEvents;
    private OrganizerEventsAdapter eventsAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile_detail);

        profileId = getIntent().getStringExtra("profileId");

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Header views
        tvName  = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvState = findViewById(R.id.tvState);
        btnBan  = findViewById(R.id.btnBan);

        btnBan.setOnClickListener(v -> toggleBan());

        // Events list
        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        eventsAdapter = new OrganizerEventsAdapter();
        rvEvents.setAdapter(eventsAdapter);
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
        tvState.setText(banned ? "BANNED" : "Active");
        btnBan.setText(banned ? "Unban organizer" : "Ban organizer");
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
                .addOnSuccessListener(events -> eventsAdapter.replace(events))
                .addOnFailureListener(e -> eventsAdapter.replace(new java.util.ArrayList<>()));
    }

    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
}
