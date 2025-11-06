package com.example.eventmaster.ui.organizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import android.view.View;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.EventAdapter;
import androidx.activity.OnBackPressedCallback;
import com.example.eventmaster.ui.organizer.enrollments.OrganizerEntrantsHubFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EventAdapter adapter;
    private List<Map<String, Object>> events = new ArrayList<>();

    private View overlayContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_events);

        overlayContainer = findViewById(R.id.fragment_container);
        recycler = findViewById(R.id.recyclerEvents);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(events);
        recycler.setAdapter(adapter);

        // 🔗 When an event is tapped, open the Entrants Hub
        adapter.setOnEventClickListener(this::openEntrantsHub);

        // Optional: keep the overlay visibility in sync with back stack
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncOverlayVisibility);


        // Use the dispatcher for back gestures / system back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    // overlay visibility will auto-sync via your listener
                } else {
                    // No fragments on stack → let the activity finish as normal
                    setEnabled(false); // hand control back to system for this press
                    OrganizerManageEventsActivity.super.onBackPressed();
                }
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
        adapter.notifyDataSetChanged();
        // TODO: load events -> update 'events' + adapter.notifyDataSetChanged()
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
