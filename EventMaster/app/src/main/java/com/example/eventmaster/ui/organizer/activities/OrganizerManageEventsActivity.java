package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.adapters.EventAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EventAdapter adapter;

    // Only THIS list is used
    private final List<Map<String, Object>> events = new ArrayList<>();

    private View overlayContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_manage_events);

        // Top bar
        MaterialToolbar topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            setSupportActionBar(topBar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            topBar.setNavigationOnClickListener(v -> handleBackToHomeOrPop());
        }

        overlayContainer = findViewById(R.id.fragment_container);

        recycler = findViewById(R.id.recyclerEvents);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        // Adapter now expects full event list with geolocation + capacity
        adapter = new EventAdapter(events);
        recycler.setAdapter(adapter);

        adapter.setOnEventClickListener(this::openManageSpecificEvent);

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
                            handleBackToHomeOrPop();
                        }
                    }
                }
        );

        loadEventsFromFirestore();
    }

    /**
     * Loads only the organizer’s events from Firestore.
     */
    private void loadEventsFromFirestore() {

        String organizerId = com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();

        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("organizerId", organizerId)
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snap -> {

                    events.clear();

                    for (QueryDocumentSnapshot doc : snap) {

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

                        events.add(m);
                    }

                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
                        Toast.makeText(
                                this,
                                "You haven't created any events yet.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            this,
                            "Failed to load events: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }


    private void openManageSpecificEvent(@NonNull String eventId) {

        String title = "Event";

        for (Map<String, Object> e : events) {
            if (e.get("eventId").equals(eventId)) {
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
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }

    private void handleBackToHomeOrPop() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            finish();
        }
    }
}
