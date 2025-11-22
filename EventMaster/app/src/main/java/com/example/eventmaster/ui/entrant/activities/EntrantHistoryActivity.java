package com.example.eventmaster.ui.entrant.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.data.api.RegistrationService;
import com.example.eventmaster.data.firestore.EventReadServiceFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.ui.shared.adapters.HistoryAdapter;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * History screen for entrants displaying all past event registrations.
 * Shows registration history with status (Selected, Not Selected, Cancelled).
 */
public class EntrantHistoryActivity extends AppCompatActivity {

    private static final String TAG = "EntrantHistory";

    // Services
    private final RegistrationService regSvc =
            new RegistrationServiceFs(FirebaseFirestore.getInstance());
    private final EventReadService eventRead = new EventReadServiceFs();

    // UI Components
    private RecyclerView recyclerView;
    private ProgressBar loadingIndicator;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigationView;

    // Data
    private HistoryAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrant_activity_history);

        // Get current user ID
        currentUserId = resolveCurrentUserId();

        // Initialize UI
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup bottom navigation
        setupBottomNavigation();

        // Load history
        loadHistory();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.history_recycler_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        emptyState = findViewById(R.id.empty_state);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        // Set History as selected (current screen)
        bottomNavigationView.setSelectedItemId(R.id.nav_history);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                finish();
                startActivity(getIntent().setClass(this, EventListActivity.class));
                return true;
            } else if (itemId == R.id.nav_history) {
                // Already on History screen
                return true;
            } else if (itemId == R.id.nav_alerts) {
                finish();
                startActivity(getIntent().setClass(this, EntrantNotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                finish();
                startActivity(getIntent().setClass(this, com.example.eventmaster.ui.shared.activities.ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Loads the list of events the entrant has registered for.
     * Fetches registration documents → maps each to an event title.
     */
    private void loadHistory() {
        showLoading(true);
        emptyState.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        regSvc.listByEntrant(currentUserId)
                .addOnSuccessListener(regs -> {
                    List<Registration> regsFinal =
                            (regs == null) ? new ArrayList<>() : new ArrayList<>(regs);

                    if (regsFinal.isEmpty()) {
                        showLoading(false);
                        showEmptyState();
                        return;
                    }

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
                        showLoading(false);
                        List<HistoryAdapter.Row> rows = new ArrayList<>();
                        for (Registration r : regsFinal) {
                            rows.add(new HistoryAdapter.Row(r, null));
                        }
                        adapter.replace(rows);
                        recyclerView.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Asynchronously fetch each event's info
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
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this,
                            "History load failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showEmptyState();
                });
    }

    /**
     * Once all events fetched → build and bind rows to RecyclerView.
     */
    private void bindHistory(@NonNull List<Registration> regs, @NonNull Map<String, Event> cache) {
        showLoading(false);

        List<HistoryAdapter.Row> rows = new ArrayList<>();
        for (Registration r : regs) {
            Event e = cache.get(r.getEventId());
            String title = (e == null) ? null : e.getTitle();
            rows.add(new HistoryAdapter.Row(r, title));
        }

        // Sort newest first
        rows.sort((a, b) -> Long.compare(b.reg.getCreatedAtUtc(), a.reg.getCreatedAtUtc()));

        if (rows.isEmpty()) {
            showEmptyState();
        } else {
            adapter.replace(rows);
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private String resolveCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return DeviceUtils.getDeviceId(this);
    }
}
