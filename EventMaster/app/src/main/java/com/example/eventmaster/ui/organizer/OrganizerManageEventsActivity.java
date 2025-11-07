package com.example.eventmaster.ui.organizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.ui.organizer.EventAdapter;
import androidx.activity.OnBackPressedCallback;
import com.example.eventmaster.ui.organizer.enrollments.OrganizerEntrantsHubFragment;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Activity that allows an organizer to view and manage their created events.
 * Displays a list of events owned by the currently logged-in organizer. When
 * an event is selected, the app opens the {@link OrganizerEntrantsHubFragment}
 * as an overlay, where the organizer can view entrants grouped by their
 * registration status (e.g., active, cancelled).
 *
 *
 * This screen fulfills the following user stories:
 *  US 02.06.02: View cancelled entrants
 *  US 02.06.03: View final enrolled list
 *  US 02.02.01: View the list of entrants who joined my event waiting list --> todo
 *
 * Once authentication and event tracking are implemented, this class will
 * connect to Firestore via {@link EventRepositoryFs} to dynamically load
 * events owned by the logged-in organizer.
 */

public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EventAdapter adapter;
    private List<Map<String, Object>> events = new ArrayList<>();

    private View overlayContainer;
    private EventRepositoryFs eventRepo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_events);

        MaterialToolbar topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            setSupportActionBar(topBar);
            // (Optional) ensure an up arrow shows even if theme doesn’t provide it:
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

        // 🔗 When an event is tapped, open the Entrants Hub
        adapter.setOnEventClickListener(this::openEntrantsHub);

        //keep the overlay visibility in sync with back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);

        // Sync overlay visibility with back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);

        // Use the dispatcher for back gestures / system back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
//                handleBackToHomeOrPop();
//            }
            }
        });

        // TEMP: fake data so tapping works
        events.add(new java.util.HashMap<String, Object>() {{
            put("eventId", "test_event_1");
            put("title", "Swimming Lessons for Beginners");
            put("location", "Local Recreation Centre Pool");
            put("regStart", null);
            put("regEnd", null);
            put("posterUrl", null);
        }});
        events.add(new java.util.HashMap<String, Object>() {{
            put("eventId", "test_event_3");
            put("title", "Interpretive Dance - Safety Basics");
            put("location", "Downtown Dance Studio");
            put("regStart", null);
            put("regEnd", null);
            put("posterUrl", null);
        }});
        //to be used when auth/track phone is implemented.
//        loadEvents();
        adapter.notifyDataSetChanged();
    }

        //TODO: after auth is implemneted we can do this...
//    private void loadEvents() {
//        View progress = findViewById(R.id.progress);           // add to XML (small spinner)
//        View empty = findViewById(R.id.emptyView);             // add to XML (“No events yet”)
//        if (progress != null) progress.setVisibility(View.VISIBLE);
//        if (empty != null) empty.setVisibility(View.GONE);
//
//        eventRepo.listByOrganizer(result -> {
//            events.clear();
//            events.addAll(result);
//            adapter.notifyDataSetChanged();
//            if (progress != null) progress.setVisibility(View.GONE);
//            if (empty != null) empty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
//        }, err -> {
//            if (progress != null) progress.setVisibility(View.GONE);
//            if (empty != null) {
//                ((android.widget.TextView) empty).setText("Failed to load: " + err.getMessage());
//                empty.setVisibility(View.VISIBLE);
//            }
//        });
//    }


    /**
     * Handles back navigation:
     * If an entrant-related fragment is open, pops it from the back stack.
     * If no fragments are open, returns to {@link OrganizerHomeActivity}.
     */
    private void handleBackToHomeOrPop() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            // No fragments visible → go to OrganizerHomeActivity
            Intent intent = new Intent(this, OrganizerHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Opens the {@link OrganizerEntrantsHubFragment} overlay for the selected event.
     * This fragment allows organizers to view entrants categorized by their
     * registration status (e.g., active, cancelled).
     *
     * @param eventId the unique ID of the selected event
     */
    private void openEntrantsHub(String eventId) {
        overlayContainer.setVisibility(View.VISIBLE);

        OrganizerEntrantsHubFragment hub = OrganizerEntrantsHubFragment.newInstance(eventId);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.slide_in_left,  // enter
                        android.R.anim.fade_out,       // exit
                        android.R.anim.fade_in,        // popEnter
                        android.R.anim.slide_out_right // popExit
                )
                .replace(R.id.fragment_container, hub)
                .addToBackStack("hub")
                .commit();
    }


    /**
     * Updates the visibility of the overlay container based on whether
     * there are fragments on the back stack.
     *
     * If no fragments are visible, the overlay is hidden to reveal
     * the event list beneath.
     */
    private void syncOverlayVisibility() {
        boolean hasStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }

}
