package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.EventAdapter;
import com.example.eventmaster.ui.shared.activities.EditProfileActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EventAdapter adapter;
    private ImageView imgProfileIcon;
    private TextView tvOrganizerName;
    private TextView tvOrganizerEmail;
    private TextView tvTotalEvents;
    private TextView tvActiveEvents;
    private android.widget.ImageButton btnSettings;
    private MaterialButton btnCreateEvents;

    // All events loaded from Firestore
    private final List<Map<String, Object>> allEvents = new ArrayList<>();
    // Filtered events shown in RecyclerView
    private final List<Map<String, Object>> filteredEvents = new ArrayList<>();

    // Filter tab views
    private LinearLayout filterAll, filterOngoing, filterEnded;
    private TextView labelAll, labelOngoing, labelEnded;
    private TextView countAll, countOngoing, countEnded;
    private View indicatorAll, indicatorOngoing, indicatorEnded;
    private String currentFilterType = "ALL";

    // Waiting list data
    private final Map<String, Integer> waitingListCounts = new HashMap<>();
    private final WaitingListRepository waitingListRepo = new WaitingListRepositoryFs();

    private View overlayContainer;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_manage_events);

        // Get current user ID safely
        com.google.firebase.auth.FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;

        // If no user is logged in, finish the activity
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        imgProfileIcon = findViewById(R.id.imgProfileIcon);
        tvOrganizerName = findViewById(R.id.tvOrganizerName);
        tvOrganizerEmail = findViewById(R.id.tvOrganizerEmail);
        tvTotalEvents = findViewById(R.id.tvTotalEvents);
        tvActiveEvents = findViewById(R.id.tvActiveEvents);
        btnSettings = findViewById(R.id.btnSettings);
        btnCreateEvents = findViewById(R.id.btnCreateEvents);

        overlayContainer = findViewById(R.id.fragment_container);

        recycler = findViewById(R.id.recyclerEvents);
        if (recycler != null) {
        recycler.setLayoutManager(new LinearLayoutManager(this));

            // Adapter uses filtered events list
            adapter = new EventAdapter(filteredEvents);
        recycler.setAdapter(adapter);

        adapter.setOnEventClickListener(this::openManageSpecificEvent);
        }

        // Initialize filter tabs
        initializeFilterTabs();
        setupFilters();

        // Setup Settings button
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                if (currentUserId != null) {
                    Intent intent = new Intent(this, EditProfileActivity.class);
                    intent.putExtra("profileId", currentUserId);
                    startActivity(intent);
                }
            });
        }

        // Setup Create Events button
        if (btnCreateEvents != null) {
            btnCreateEvents.setOnClickListener(v -> {
                startActivity(new Intent(this, OrganizerCreateEventActivity.class));
            });
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        FragmentManager fm = getSupportFragmentManager();
                        if (fm.getBackStackEntryCount() > 0) {
                            fm.popBackStack();
                        } else {
                            // On main screen, finish activity when back is pressed
                            finish();
                        }
                    }
                }
        );

        loadOrganizerProfile();
        loadEventsFromFirestore();
    }

    /**
     * Loads the organizer's profile information and displays it.
     */
    private void loadOrganizerProfile() {
        if (currentUserId == null) {
            return;
        }

        AuthHelper.getCurrentUserProfile(new AuthHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                if (profile != null) {
                    String name = profile.getName();
                    String email = profile.getEmail();
                    
                    if (tvOrganizerName != null) {
                        tvOrganizerName.setText(name != null && !name.isEmpty() ? name : "Organizer");
                    }
                    
                    if (tvOrganizerEmail != null) {
                        tvOrganizerEmail.setText(email != null && !email.isEmpty() ? email : "");
                    }
                    
                    // Load profile picture if available
                    if (imgProfileIcon != null) {
                        String imageUrl = profile.getProfileImageUrl();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Clear any tint before loading real image
                            imgProfileIcon.setColorFilter(null);
                            Glide.with(OrganizerManageEventsActivity.this)
                                    .load(imageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(imgProfileIcon);
                        } else {
                            // Use placeholder with tint
                            imgProfileIcon.setImageResource(R.drawable.baseline_person_24);
                            imgProfileIcon.setColorFilter(android.graphics.Color.parseColor("#15837C"));
                        }
                    }
                }
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(
                    OrganizerManageEventsActivity.this,
                    "Failed to load profile: " + (error != null ? error.getMessage() : "Unknown error"),
                    Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    /**
     * Loads only the organizer's events from Firestore.
     */
    private void loadEventsFromFirestore() {
        if (currentUserId == null) {
            return;
        }

        if (adapter == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerId", currentUserId)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snap -> {
                    if (allEvents == null || adapter == null) {
                        return;
                    }

                    allEvents.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        try {
                        String eventId = doc.getString("eventId");
                        if (eventId == null || eventId.isEmpty()) {
                            eventId = doc.getId();
                        }

                        Map<String, Object> m = new HashMap<>();
                        m.put("eventId", eventId);
                        m.put("title", doc.getString("title"));
                        m.put("location", doc.getString("location"));
                        m.put("regStart", doc.get("registrationOpen"));
                        m.put("regEnd", doc.get("registrationClose"));
                        m.put("posterUrl", doc.get("posterUrl"));
                        m.put("capacity", doc.getLong("capacity"));
                        m.put("geolocationRequired", doc.getBoolean("geolocationRequired"));
                            // Store event date for filtering
                            Object eventDateObj = doc.get("eventDate");
                            if (eventDateObj != null) {
                                m.put("eventDate", eventDateObj);
                            }

                            allEvents.add(m);
                        } catch (Exception e) {
                            // Skip malformed documents
                            android.util.Log.e("OrganizerManageEvents", "Error parsing event document", e);
                        }
                    }

                    // Load waiting list counts and then apply filter
                    loadWaitingListCounts(() -> {
                        updateStatistics();
                        updateFilterCounts();
                        applyFilter();
                    });

                })
                .addOnFailureListener(e -> {
                    // Check if error is about missing index
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("index")) {
                        Toast.makeText(
                                this,
                                "Please wait while the database is being set up.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                    Toast.makeText(
                            this,
                                "Failed to load events: " + (errorMsg != null ? errorMsg : "Unknown error"),
                            Toast.LENGTH_LONG
                    ).show();
                    }
                    android.util.Log.e("OrganizerManageEvents", "Error loading events", e);
                });
    }


    private void initializeFilterTabs() {
        filterAll = findViewById(R.id.events_filter_all);
        filterOngoing = findViewById(R.id.events_filter_ongoing);
        filterEnded = findViewById(R.id.events_filter_ended);

        labelAll = findViewById(R.id.events_label_all);
        labelOngoing = findViewById(R.id.events_label_ongoing);
        labelEnded = findViewById(R.id.events_label_ended);

        countAll = findViewById(R.id.events_count_all);
        countOngoing = findViewById(R.id.events_count_ongoing);
        countEnded = findViewById(R.id.events_count_ended);

        indicatorAll = findViewById(R.id.events_indicator_all);
        indicatorOngoing = findViewById(R.id.events_indicator_ongoing);
        indicatorEnded = findViewById(R.id.events_indicator_ended);
    }

    private void setupFilters() {
        if (filterAll != null) filterAll.setOnClickListener(v -> selectFilter("ALL"));
        if (filterOngoing != null) filterOngoing.setOnClickListener(v -> selectFilter("ONGOING"));
        if (filterEnded != null) filterEnded.setOnClickListener(v -> selectFilter("ENDED"));
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

        boolean allActive = "ALL".equals(currentFilterType);
        if (labelAll != null) labelAll.setTextColor(allActive ? activeColor : inactiveColor);
        if (countAll != null) countAll.setTextColor(allActive ? activeColor : inactiveColor);
        if (indicatorAll != null) indicatorAll.setVisibility(allActive ? View.VISIBLE : View.INVISIBLE);

        boolean ongoingActive = "ONGOING".equals(currentFilterType);
        if (labelOngoing != null) labelOngoing.setTextColor(ongoingActive ? activeColor : inactiveColor);
        if (countOngoing != null) countOngoing.setTextColor(ongoingActive ? activeColor : inactiveColor);
        if (indicatorOngoing != null) indicatorOngoing.setVisibility(ongoingActive ? View.VISIBLE : View.INVISIBLE);

        boolean endedActive = "ENDED".equals(currentFilterType);
        if (labelEnded != null) labelEnded.setTextColor(endedActive ? activeColor : inactiveColor);
        if (countEnded != null) countEnded.setTextColor(endedActive ? activeColor : inactiveColor);
        if (indicatorEnded != null) indicatorEnded.setVisibility(endedActive ? View.VISIBLE : View.INVISIBLE);
    }

    private void applyFilter() {
        filteredEvents.clear();
        long now = System.currentTimeMillis();

        for (Map<String, Object> event : allEvents) {
            boolean shouldInclude = false;

            switch (currentFilterType) {
                case "ALL":
                    shouldInclude = true;
                    break;
                case "ONGOING":
                    // Events that haven't ended yet
                    Object eventDateObj = event.get("eventDate");
                    boolean isEnded = false;
                    if (eventDateObj instanceof Timestamp) {
                        Timestamp eventDate = (Timestamp) eventDateObj;
                        isEnded = eventDate.toDate().getTime() < now;
                    } else {
                        // Fallback: check registration end date
                        Object regEndObj = event.get("regEnd");
                        if (regEndObj instanceof Timestamp) {
                            Timestamp regEnd = (Timestamp) regEndObj;
                            isEnded = regEnd.toDate().getTime() < now;
                        }
                    }
                    shouldInclude = !isEnded;
                    break;
                case "ENDED":
                    // Past events
                    Object eventDateObj2 = event.get("eventDate");
                    if (eventDateObj2 instanceof Timestamp) {
                        Timestamp eventDate = (Timestamp) eventDateObj2;
                        shouldInclude = eventDate.toDate().getTime() < now;
                    } else {
                        // Fallback: check registration end date
                        Object regEndObj = event.get("regEnd");
                        if (regEndObj instanceof Timestamp) {
                            Timestamp regEnd = (Timestamp) regEndObj;
                            shouldInclude = regEnd.toDate().getTime() < now;
                        }
                    }
                    break;
            }

            if (shouldInclude) {
                filteredEvents.add(event);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateStatistics() {
        if (tvTotalEvents != null) {
            tvTotalEvents.setText(String.valueOf(allEvents.size()));
        }

        // Count active/ongoing events
        int activeCount = 0;
        long now = System.currentTimeMillis();

        for (Map<String, Object> event : allEvents) {
            Object eventDateObj = event.get("eventDate");
            boolean isEnded = false;
            if (eventDateObj instanceof Timestamp) {
                isEnded = ((Timestamp) eventDateObj).toDate().getTime() < now;
            } else {
                Object regEndObj = event.get("regEnd");
                if (regEndObj instanceof Timestamp) {
                    isEnded = ((Timestamp) regEndObj).toDate().getTime() < now;
                }
            }
            if (!isEnded) {
                activeCount++;
            }
        }

        if (tvActiveEvents != null) {
            tvActiveEvents.setText(String.valueOf(activeCount));
        }
    }

    private void updateFilterCounts() {
        int allCount = allEvents.size();
        int ongoingCount = 0;
        int endedCount = 0;
        long now = System.currentTimeMillis();

        for (Map<String, Object> event : allEvents) {
            Object eventDateObj = event.get("eventDate");
            boolean isEnded = false;
            if (eventDateObj instanceof Timestamp) {
                isEnded = ((Timestamp) eventDateObj).toDate().getTime() < now;
            } else {
                Object regEndObj = event.get("regEnd");
                if (regEndObj instanceof Timestamp) {
                    isEnded = ((Timestamp) regEndObj).toDate().getTime() < now;
                }
            }
            
            if (isEnded) {
                endedCount++;
            } else {
                ongoingCount++;
            }
        }

        if (countAll != null) countAll.setText(String.valueOf(allCount));
        if (countOngoing != null) countOngoing.setText(String.valueOf(ongoingCount));
        if (countEnded != null) countEnded.setText(String.valueOf(endedCount));
    }

    private void loadWaitingListCounts(Runnable onComplete) {
        if (allEvents.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        waitingListCounts.clear();
        final int[] completedCount = {0};
        final int totalCount = allEvents.size();

        for (Map<String, Object> event : allEvents) {
            String eventId = extractEventId(event);
            if (eventId == null) {
                completedCount[0]++;
                if (completedCount[0] == totalCount && onComplete != null) {
                    onComplete.run();
                }
                continue;
            }

            // Load waiting list count
            waitingListRepo.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
                @Override
                public void onSuccess(int count) {
                    waitingListCounts.put(eventId, count);
                    completedCount[0]++;
                    if (adapter != null) {
                        adapter.setWaitingListCounts(waitingListCounts);
                    }
                    if (completedCount[0] == totalCount && onComplete != null) {
                        onComplete.run();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    waitingListCounts.put(eventId, 0);
                    completedCount[0]++;
                    if (adapter != null) {
                        adapter.setWaitingListCounts(waitingListCounts);
                    }
                    if (completedCount[0] == totalCount && onComplete != null) {
                        onComplete.run();
                    }
                }
            });
        }
    }

    private String extractEventId(Map<String, Object> e) {
        Object id = e.get("id");
        if (id == null) id = e.get("eventId");
        if (id == null) id = e.get("docId");
        return id == null ? null : String.valueOf(id);
    }

    private void openManageSpecificEvent(@NonNull String eventId) {

        String title = "Event";

        // Search in all events, not just filtered
        for (Map<String, Object> e : allEvents) {
            if (extractEventId(e).equals(eventId)) {
                Object t = e.get("title");
                if (t != null) title = t.toString();
                break;
            }
        }

        Intent intent = new Intent(this, OrganizerManageSpecificEventActivity.class);
        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_TITLE, title);
        startActivity(intent);
    }

    private void syncOverlayVisibility() {
        boolean hasStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (overlayContainer != null) {
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile in case it was updated
        loadOrganizerProfile();
        // Reload events in case new ones were created
        loadEventsFromFirestore();
        // Reload waiting list counts
        if (!allEvents.isEmpty()) {
            loadWaitingListCounts(() -> {
                updateFilterCounts();
                applyFilter();
            });
    }
}
}