package com.example.eventmaster.ui.organizer;

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
import com.example.eventmaster.ui.organizer.enrollments.OrganizerEntrantsHubFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrganizerManageEventsActivity
 *
 * Role:
 * - Displays a list of events from Firestore for the organizer to manage.
 * - When an event card is tapped, opens OrganizerEntrantsHubFragment as an overlay
 *   so the organizer can view final (ACTIVE) and cancelled entrants.
 *
 * User Stories:
 * - US 02.06.02: View cancelled entrants
 * - US 02.06.03: View final enrolled list
 */
public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EventAdapter adapter;
    private final List<Map<String, Object>> events = new ArrayList<>();

    private View overlayContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_events);

        // Top bar with back navigation
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

        adapter = new EventAdapter(events);
        recycler.setAdapter(adapter);

        // When an event is tapped, open Entrants Hub overlay
        adapter.setOnEventClickListener(this::openEntrantsHub);

        // Sync overlay visibility with fragment back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);

        // Handle system back (gestures + button)
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

        // Load events from Firestore
        loadEventsFromFirestore();
    }

    /**
     * Loads events from Firestore and populates the RecyclerView.
     * For demo, loads all events. To scope per organizer, filter by organizerId.
     */
    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snap -> {
                    events.clear();

                    for (QueryDocumentSnapshot doc : snap) {
                        // Prefer explicit eventId; fallback to document id for older docs.
                        String eventId = doc.getString("eventId");
                        if (eventId == null || eventId.isEmpty()) {
                            eventId = doc.getId();
                        }

                        Map<String, Object> m = new HashMap<>();
                        m.put("eventId", eventId);
                        m.put("id", eventId); // extra safety for adapters using "id"
                        m.put("title", doc.getString("title"));
                        m.put("location", doc.getString("location"));
                        m.put("regStart", doc.get("regStart"));     // may be Timestamp
                        m.put("regEnd", doc.get("regEnd"));         // may be Timestamp
                        m.put("posterUrl", doc.get("posterUrl"));

                        events.add(m);
                    }

                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
                        Toast.makeText(this,
                                "No events yet. Create one as organizer.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to load events: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    android.util.Log.e("OrganizerManageEvents", "loadEvents failed", e);
                });
    }

    /**
     * Opens the Entrants Hub overlay for a selected event.
     *
     * @param eventId the ID of the selected event (from Firestore)
     */
    private void openEntrantsHub(@NonNull String eventId) {
        overlayContainer.setVisibility(View.VISIBLE);

        OrganizerEntrantsHubFragment hub = OrganizerEntrantsHubFragment.newInstance(eventId);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, hub)
                .addToBackStack("hub")
                .commit();
    }

    /**
     * Shows or hides the overlay container depending on whether
     * there are fragments currently in the back stack.
     */
    private void syncOverlayVisibility() {
        boolean hasStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }

    /**
     * Back behavior:
     * - If a fragment (hub or list) is open, pop it.
     * - Otherwise, finish and return to the previous activity (e.g. OrganizerHomeActivity).
     */
    private void handleBackToHomeOrPop() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            finish();
        }
    }
}
