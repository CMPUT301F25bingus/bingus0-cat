package com.example.eventmaster.ui.organizer;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
 * Activity that displays a list of events created by the organizer.
 * Allows the organizer to view and manage each event and open
 * the Entrants Hub overlay to see registered participants.
 *
 * Firestore Collection: "events"
 * Fields: eventId, title, location, regStart, regEnd, posterUrl, createdAt
 *
 * User Stories Supported:
 *  - US 02.02.01: View entrants on waiting list
 *  - US 02.06.02: View cancelled entrants
 *  - US 02.06.03: View final enrolled entrants
 */
public class OrganizerManageEventsActivity extends AppCompatActivity {

    /** RecyclerView that lists all events. */
    private RecyclerView recycler;

    /** Adapter that binds event data to the list. */
    private EventAdapter adapter;

    /** List of events fetched from Firestore. */
    private final List<Map<String, Object>> events = new ArrayList<>();

    /** Overlay container that hosts the Entrants Hub fragment. */
    private View overlayContainer;

    /**
     * Initializes the screen, toolbar, and event list.
     * Sets up listeners for event selection and back navigation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_events);

        // Set up the toolbar and back navigation
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

        // Open Entrants Hub when an event is selected
        adapter.setOnEventClickListener(this::openEntrantsHub);

        // Sync overlay visibility with fragment back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);

        // Handle system back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
                } else {
                    handleBackToHomeOrPop();
                }
            }
        });

        // Load events from Firestore
        loadEventsFromFirestore();
    }

    /**
     * Loads events from Firestore and updates the RecyclerView.
     * Currently loads all events in the "events" collection.
     * Can later be filtered to only show those created by the current organizer.
     */
    private void loadEventsFromFirestore() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener(snap -> {
                    events.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Map<String, Object> m = new HashMap<>();
                        m.put("eventId", doc.getString("eventId"));
                        m.put("title", doc.getString("title"));
                        m.put("location", doc.getString("location"));
                        m.put("regStart", doc.get("regStart"));
                        m.put("regEnd", doc.get("regEnd"));
                        m.put("posterUrl", doc.get("posterUrl"));
                        events.add(m);
                    }
                    adapter.notifyDataSetChanged();

                    if (events.isEmpty()) {
                        Toast.makeText(this, "No events yet. Create one!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load events: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("OrganizerManageEvents", "loadEvents failed", e);
                });
    }

    /**
     * Opens the Entrants Hub overlay for the selected event.
     *
     * @param eventId The Firestore ID of the selected event.
     */
    private void openEntrantsHub(String eventId) {
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
     * Updates the overlay container visibility
     * based on the fragment back stack state.
     */
    private void syncOverlayVisibility() {
        boolean hasStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }

    /**
     * Handles back navigation. Pops a fragment if one is open;
     * otherwise returns to the previous screen.
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
