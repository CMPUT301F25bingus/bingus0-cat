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

    private void syncOverlayVisibility() {
        boolean hasStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        overlayContainer.setVisibility(hasStack ? View.VISIBLE : View.GONE);
    }

}
