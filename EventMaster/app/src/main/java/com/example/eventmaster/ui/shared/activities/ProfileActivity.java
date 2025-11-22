package com.example.eventmaster.ui.shared.activities;

import android.content.Intent;
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
import com.example.eventmaster.data.api.RegistrationService;
import com.example.eventmaster.data.firestore.EventReadServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.ui.shared.adapters.HistoryAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProfileActivity
 * ----------------
 * Displays entrant profile info (name, email, phone, banned flag)
 * and shows event history based on registrations.
 *
 * Features:
 *  - Load user info from Firestore
 *  - Edit / delete profile
 *  - Show list of registered events (title + date)
 *  - Toolbar back arrow navigation to Event List
 */
public class ProfileActivity extends AppCompatActivity {

    // === Data repositories ===
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    private final RegistrationService regSvc =
            new RegistrationServiceFs(FirebaseFirestore.getInstance());
    private final EventReadService eventRead = new EventReadServiceFs();

    // Temporary fallback ID; replaced by FirebaseAuth UID if logged in
    private String currentId = "demoUser123";

    // === UI elements ===
    private TextView tvName, tvEmail, tvPhone, tvBanned;
    private Button btnEdit, btnDelete;
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shared_activity_profile);

        // 🔹 Setup Toolbar with back arrow
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Make it act as the app bar
            setSupportActionBar(toolbar);
            // Enable back arrow in the top-left corner
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            // When arrow clicked → return to previous screen
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        // 🔹 Use real UID if Firebase Auth user exists
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 🔹 Bind layout views
        tvName   = findViewById(R.id.tvName);
        tvEmail  = findViewById(R.id.tvEmail);
        tvPhone  = findViewById(R.id.tvPhone);
        tvBanned = findViewById(R.id.tvBanned);
        btnEdit  = findViewById(R.id.btnEdit);
        btnDelete= findViewById(R.id.btnDelete);
        rvHistory = findViewById(R.id.rvHistory);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 🔹 Edit button → open EditProfileActivity
        btnEdit.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)
                        .putExtra("profileId", currentId)));

        // 🔹 Delete button → confirm, then delete profile
        btnDelete.setOnClickListener(v -> confirmDelete());

        // 🔹 Setup RecyclerView for history
        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            historyAdapter = new HistoryAdapter();
            rvHistory.setAdapter(historyAdapter);
        }

        // Setup bottom navigation (for entrants)
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        // Show bottom nav for entrants (always show for now, can be made conditional later)
        bottomNavigationView.setVisibility(android.view.View.VISIBLE);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EventListActivity.class));
                return true;
            } else if (itemId == R.id.nav_history) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EntrantHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_alerts) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EntrantNotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on Profile screen
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile + history every time user returns
        loadProfile();
        loadHistory();
    }

    // === PROFILE SECTION ===

    /**
     * Load entrant profile from Firestore using ProfileRepositoryFs.
     * Updates name, email, phone, and banned chip.
     */
    private void loadProfile() {
        profileRepo.get(currentId, p -> {
            tvName.setText(ns(p.getName()));
            tvEmail.setText(ns(p.getEmail()));
            tvPhone.setText(ns(p.getPhone()));
            boolean banned = p.getBanned();
            tvBanned.setText(banned ? "BANNED" : "");
            tvBanned.setVisibility(banned ? TextView.VISIBLE : TextView.GONE);
        }, e -> tvName.setText("Error: " + e.getMessage()));
    }

    /**
     * Show confirmation dialog before permanently deleting profile.
     * If confirmed → deletes and closes activity.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete profile?")
                .setMessage("This removes your profile from the system.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) ->
                        profileRepo.delete(currentId, v -> finish(), err -> {}))
                .show();
    }

    // === HISTORY SECTION ===

    /**
     * Loads the list of events the entrant has registered for.
     * Fetches registration documents → maps each to an event title.
     */
    private void loadHistory() {
        if (rvHistory == null || historyAdapter == null) return;

        // Clear adapter while loading
        historyAdapter.replace(new ArrayList<>());

        regSvc.listByEntrant(currentId)
                .addOnSuccessListener(regs -> {
                    List<Registration> regsFinal =
                            (regs == null) ? new ArrayList<>() : new ArrayList<>(regs);

                    // Collect event IDs from registrations
                    Map<String, Event> cache = new HashMap<>();
                    List<String> eventIds = new ArrayList<>();
                    for (Registration r : regsFinal) {
                        String eid = r.getEventId();
                        if (eid != null && !cache.containsKey(eid)) {
                            cache.put(eid, null);
                            eventIds.add(eid);
                        }
                    }

                    // No event references → display basic rows
                    if (eventIds.isEmpty()) {
                        List<HistoryAdapter.Row> rows = new ArrayList<>();
                        for (Registration r : regsFinal) {
                            rows.add(new HistoryAdapter.Row(r, null));
                        }
                        historyAdapter.replace(rows);
                        return;
                    }

                    // Asynchronously fetch each event’s info
                    AtomicInteger done = new AtomicInteger(0);
                    int total = eventIds.size();

                    for (String eventId : eventIds) {
                        eventRead.get(eventId)
                                .addOnSuccessListener(e -> {
                                    cache.put(eventId, e);
                                    if (done.incrementAndGet() == total) {
                                        bindHistory(regsFinal, cache);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    if (done.incrementAndGet() == total) {
                                        bindHistory(regsFinal, cache);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        android.widget.Toast.makeText(this,
                                "History load failed: " + e.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show());
    }

    /**
     * Once all events fetched → build and bind rows to RecyclerView.
     */
    private void bindHistory(List<Registration> regs, Map<String, Event> cache) {
        List<HistoryAdapter.Row> rows = new ArrayList<>();
        for (Registration r : regs) {
            Event e = cache.get(r.getEventId());
            String title = (e == null) ? null : e.getTitle();
            rows.add(new HistoryAdapter.Row(r, title));
        }

        // Sort newest first
        rows.sort((a, b) -> Long.compare(b.reg.getCreatedAtUtc(), a.reg.getCreatedAtUtc()));
        historyAdapter.replace(rows);
    }

    // === Utility ===

    /**
     * Returns safe text (fallback if null or blank).
     */
    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
}
