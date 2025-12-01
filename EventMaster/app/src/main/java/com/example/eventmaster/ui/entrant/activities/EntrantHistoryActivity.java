package com.example.eventmaster.ui.entrant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
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
import com.example.eventmaster.model.RegistrationStatus;
import com.example.eventmaster.ui.shared.adapters.HistoryAdapter;
import com.example.eventmaster.utils.DeviceUtils;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    
    // Filter bar components
    private LinearLayout filterAll, filterWaiting, filterSelected, filterEnded;
    private TextView labelAll, labelWaiting, labelSelected, labelEnded;
    private TextView countAll, countWaiting, countSelected, countEnded;
    private View indicatorAll, indicatorWaiting, indicatorSelected, indicatorEnded;
    private String currentFilterType = "ALL";

    // Data
    private HistoryAdapter adapter;
    private String currentUserId;
    private final List<HistoryAdapter.HistoryItem> allItems = new ArrayList<>();

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

        // Setup filters
        setupFilters();

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
        
        // Filter bar views
        filterAll = findViewById(R.id.history_filter_all);
        filterWaiting = findViewById(R.id.history_filter_waiting);
        filterSelected = findViewById(R.id.history_filter_selected);
        filterEnded = findViewById(R.id.history_filter_ended);
        
        labelAll = findViewById(R.id.history_label_all);
        labelWaiting = findViewById(R.id.history_label_waiting);
        labelSelected = findViewById(R.id.history_label_selected);
        labelEnded = findViewById(R.id.history_label_ended);
        
        countAll = findViewById(R.id.history_count_all);
        countWaiting = findViewById(R.id.history_count_waiting);
        countSelected = findViewById(R.id.history_count_selected);
        countEnded = findViewById(R.id.history_count_ended);
        
        indicatorAll = findViewById(R.id.history_indicator_all);
        indicatorWaiting = findViewById(R.id.history_indicator_waiting);
        indicatorSelected = findViewById(R.id.history_indicator_selected);
        indicatorEnded = findViewById(R.id.history_indicator_ended);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(item -> {
            if (item.eventId != null) {
                Intent i = new Intent(this, EventDetailsActivity.class);
                i.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, item.eventId);
                startActivity(i);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        // Initialize count text views with default values
        if (countAll != null) {
            countAll.setText("0");
            countAll.setVisibility(View.VISIBLE);
        }
        if (countWaiting != null) {
            countWaiting.setText("0");
            countWaiting.setVisibility(View.VISIBLE);
        }
        if (countSelected != null) {
            countSelected.setText("0");
            countSelected.setVisibility(View.VISIBLE);
        }
        if (countEnded != null) {
            countEnded.setText("0");
            countEnded.setVisibility(View.VISIBLE);
        }
        
        // Set click listeners for filter options
        if (filterAll != null) {
            filterAll.setOnClickListener(v -> selectFilter("ALL"));
        }
        if (filterWaiting != null) {
            filterWaiting.setOnClickListener(v -> selectFilter("WAITING"));
        }
        if (filterSelected != null) {
            filterSelected.setOnClickListener(v -> selectFilter("SELECTED"));
        }
        if (filterEnded != null) {
            filterEnded.setOnClickListener(v -> selectFilter("ENDED"));
        }
        
        // Set initial filter to ALL
        selectFilter("ALL");
    }
    
    private void selectFilter(String filter) {
        currentFilterType = filter;
        updateFilterIndicators();
        applyFilter();
    }
    
    private void updateFilterIndicators() {
        int activeColor = ContextCompat.getColor(this, R.color.teal_dark);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_secondary_dark);
        
        // Update ALL
        boolean allActive = "ALL".equals(currentFilterType);
        if (labelAll != null) labelAll.setTextColor(allActive ? activeColor : inactiveColor);
        if (countAll != null) countAll.setTextColor(allActive ? activeColor : inactiveColor);
        if (indicatorAll != null) indicatorAll.setVisibility(allActive ? View.VISIBLE : View.INVISIBLE);
        
        // Update WAITING
        boolean waitingActive = "WAITING".equals(currentFilterType);
        if (labelWaiting != null) labelWaiting.setTextColor(waitingActive ? activeColor : inactiveColor);
        if (countWaiting != null) countWaiting.setTextColor(waitingActive ? activeColor : inactiveColor);
        if (indicatorWaiting != null) indicatorWaiting.setVisibility(waitingActive ? View.VISIBLE : View.INVISIBLE);
        
        // Update SELECTED
        boolean selectedActive = "SELECTED".equals(currentFilterType);
        if (labelSelected != null) labelSelected.setTextColor(selectedActive ? activeColor : inactiveColor);
        if (countSelected != null) countSelected.setTextColor(selectedActive ? activeColor : inactiveColor);
        if (indicatorSelected != null) indicatorSelected.setVisibility(selectedActive ? View.VISIBLE : View.INVISIBLE);
        
        // Update ENDED
        boolean endedActive = "ENDED".equals(currentFilterType);
        if (labelEnded != null) labelEnded.setTextColor(endedActive ? activeColor : inactiveColor);
        if (countEnded != null) countEnded.setTextColor(endedActive ? activeColor : inactiveColor);
        if (indicatorEnded != null) indicatorEnded.setVisibility(endedActive ? View.VISIBLE : View.INVISIBLE);
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

        // Query both the Firebase auth UID (if present) and the deviceId so we catch
        // registrations created under either identifier.
        List<String> idsToQuery = new ArrayList<>();
        String authId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        String deviceId = DeviceUtils.getDeviceId(this);
        if (authId != null && !authId.isEmpty()) idsToQuery.add(authId);
        if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(authId)) idsToQuery.add(deviceId);

        if (idsToQuery.isEmpty()) {
            showLoading(false);
            showEmptyState();
            return;
        }

        List<Task<?>> tasks = new ArrayList<>();
        List<Registration> mergedRegs = new ArrayList<>();
        Set<String> regSeen = new HashSet<>();

        for (String id : idsToQuery) {
            Task<List<Registration>> regTask = regSvc.listByEntrant(id);
            tasks.add(regTask.addOnSuccessListener(regs -> {
                if (regs != null) {
                    for (Registration r : regs) {
                        String key = r.getEventId() + "|" + r.getEntrantId();
                        if (!regSeen.contains(key)) {
                            regSeen.add(key);
                            mergedRegs.add(r);
                        }
                    }
                }
            }));
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            try {
                // Log any task failures for debugging
                for (Task<?> t : tasks) {
                    if (t.isComplete() && !t.isSuccessful() && t.getException() != null) {
                        Log.w(TAG, "History task failed", t.getException());
                    }
                }
                // After registrations, fetch waiting-list entries without requiring collection group index
                fetchWaitingListEntries(idsToQuery, docs -> {
                    try {
                        buildItems(mergedRegs, docs);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to build history items", e);
                        Toast.makeText(this, "Failed to load history", Toast.LENGTH_LONG).show();
                        showLoading(false);
                        showEmptyState();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to build history items", e);
                Toast.makeText(this, "Failed to load history", Toast.LENGTH_LONG).show();
                showLoading(false);
                showEmptyState();
            }
        });
    }

    /**
     * Fetch waiting list docs for the given IDs by scanning events and reading subcollection docs.
     * This avoids collectionGroup index requirements.
     */
    private void fetchWaitingListEntries(@NonNull List<String> idsToQuery,
                                         @NonNull java.util.function.Consumer<List<DocumentSnapshot>> onDone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").get()
                .addOnSuccessListener(eventsSnap -> {
                    List<DocumentSnapshot> waitingDocs = new ArrayList<>();
                    List<DocumentSnapshot> events = eventsSnap.getDocuments();
                    if (events.isEmpty() || idsToQuery.isEmpty()) {
                        onDone.accept(waitingDocs);
                        return;
                    }

                    // For each event and each id, fetch doc
                    AtomicInteger remaining = new AtomicInteger(events.size() * idsToQuery.size());
                    for (DocumentSnapshot ev : events) {
                        String eventId = ev.getId();
                        for (String id : idsToQuery) {
                            ev.getReference().collection("waiting_list").document(id).get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc != null && doc.exists()) {
                                            waitingDocs.add(doc);
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.w(TAG, "waitlist fetch failed for event " + eventId, e))
                                    .addOnCompleteListener(t -> {
                                        if (remaining.decrementAndGet() == 0) {
                                            onDone.accept(waitingDocs);
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to fetch events for waiting list scan", e);
                    onDone.accept(new ArrayList<>());
                });
    }

    /**
     * Build unified history items from registrations + waiting list, fetch event details, and bind.
     */
    private void buildItems(@NonNull List<Registration> regs,
                            @NonNull List<DocumentSnapshot> waitingDocs) {
        Map<String, HistoryAdapter.HistoryItem> map = new HashMap<>();

        // Registrations (takes precedence)
        for (Registration r : regs) {
            if (r.getEventId() == null) continue;
            HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem();
            item.eventId = r.getEventId();
            item.joinedDateMs = r.getCreatedAtUtc();
            item.status = mapStatus(r.getStatus());
            item.statusLabel = statusLabel(item.status);
            item.ended = false;
            map.put(r.getEventId(), item);
        }

        // Waiting list entries (only if no registration yet)
        for (DocumentSnapshot doc : waitingDocs) {
            String eventId = doc.getString("eventId");
            if (eventId == null && doc.getReference().getParent() != null
                    && doc.getReference().getParent().getParent() != null) {
                eventId = doc.getReference().getParent().getParent().getId();
            }
            if (eventId == null || map.containsKey(eventId)) continue;

            HistoryAdapter.HistoryItem item = new HistoryAdapter.HistoryItem();
            item.eventId = eventId;
            Object ts = doc.get("joinedDate");
            if (ts instanceof com.google.firebase.Timestamp) {
                item.joinedDateMs = ((com.google.firebase.Timestamp) ts).toDate().getTime();
            } else if (ts instanceof Number) {
                item.joinedDateMs = ((Number) ts).longValue();
            }
            String statusRaw = doc.getString("status");
            if (statusRaw == null) statusRaw = "waiting";
            item.status = mapWaitingStatus(statusRaw);
            item.statusLabel = statusLabel(item.status);
            map.put(eventId, item);
        }

        if (map.isEmpty()) {
            showLoading(false);
            showEmptyState();
            return;
        }

        // Fetch event details for titles/dates/posters
        List<String> eventIds = new ArrayList<>(map.keySet());
        if (eventIds.isEmpty()) {
            showLoading(false);
            showEmptyState();
            return;
        }
        AtomicInteger remaining = new AtomicInteger(eventIds.size());

        for (String eventId : eventIds) {
            eventRead.get(eventId)
                    .addOnSuccessListener(e -> {
                        HistoryAdapter.HistoryItem item = map.get(eventId);
                        if (item != null && e != null) {
                            item.title = e.getName();
                            if (e.getEventDate() != null) {
                                item.eventDateMs = e.getEventDate().getTime();
                                item.ended = e.getEventDate().before(new Date());
                                if (item.ended && !"SELECTED".equals(item.status)) {
                                    item.status = "ENDED";
                                    item.statusLabel = statusLabel(item.status);
                                }
                            }
                            if (e.getPosterUrl() != null) {
                                item.posterUrl = e.getPosterUrl();
                            }
                        }
                    })
                    .addOnCompleteListener(t -> {
                        if (remaining.decrementAndGet() == 0) {
                            bindHistory(new ArrayList<>(map.values()));
                        }
                    });
        }
    }

    /**
     * Once all events fetched → build and bind rows to RecyclerView.
     */
    private void bindHistory(@NonNull List<HistoryAdapter.HistoryItem> items) {
        showLoading(false);
        allItems.clear();
        allItems.addAll(items);
        updateFilterCounts();
        applyFilter();
    }

    private void applyFilter() {
        List<HistoryAdapter.HistoryItem> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (HistoryAdapter.HistoryItem item : allItems) {
            switch (currentFilterType) {
                case "WAITING":
                    if ("WAITING".equals(item.status)) filtered.add(item);
                    break;
                case "SELECTED":
                    if ("SELECTED".equals(item.status)) filtered.add(item);
                    break;
                case "ENDED":
                    if (item.eventDateMs > 0 && item.eventDateMs < now) filtered.add(item);
                    break;
                default:
                    filtered.add(item);
            }
        }

        // Sort newest join first
        filtered.sort((a, b) -> Long.compare(b.joinedDateMs, a.joinedDateMs));

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter.replace(filtered);
        }

        updateFilterCounts();
    }

    private void updateFilterCounts() {
        int allCount = allItems.size();
        int waiting = 0, selected = 0, ended = 0;
        long now = System.currentTimeMillis();
        for (HistoryAdapter.HistoryItem item : allItems) {
            if ("WAITING".equals(item.status)) waiting++;
            if ("SELECTED".equals(item.status)) selected++;
            if (item.eventDateMs > 0 && item.eventDateMs < now) ended++;
        }
        
        if (countAll != null) {
            countAll.setText(String.valueOf(allCount));
            countAll.setVisibility(View.VISIBLE);
        }
        if (countWaiting != null) {
            countWaiting.setText(String.valueOf(waiting));
            countWaiting.setVisibility(View.VISIBLE);
        }
        if (countSelected != null) {
            String selectedText = String.valueOf(selected);
            countSelected.setText(selectedText);
            countSelected.setVisibility(View.VISIBLE);
            countSelected.invalidate(); // Force redraw
            Log.d(TAG, "Selected count set to: " + selectedText);
        } else {
            Log.e(TAG, "countSelected TextView is null!");
        }
        if (countEnded != null) {
            countEnded.setText(String.valueOf(ended));
            countEnded.setVisibility(View.VISIBLE);
        }
    }

    private String mapStatus(RegistrationStatus st) {
        if (st == null) return "WAITING";
        switch (st) {
            case ACTIVE:
                return "SELECTED";
            case CANCELLED_BY_ENTRANT:
            case CANCELLED_BY_ORGANIZER:
                return "CANCELLED";
            case NOT_SELECTED:
                return "NOT_SELECTED";
            default:
                return "WAITING";
        }
    }

    private String mapWaitingStatus(String statusRaw) {
        String s = statusRaw.toLowerCase();
        if (s.contains("select")) return "SELECTED";
        if (s.contains("cancel")) return "CANCELLED";
        return "WAITING";
    }

    private String statusLabel(String status) {
        switch (status) {
            case "SELECTED": return "Selected";
            case "CANCELLED": return "Cancelled";
            case "NOT_SELECTED": return "Not selected";
            case "ENDED": return "Ended";
            default: return "On waitlist";
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
