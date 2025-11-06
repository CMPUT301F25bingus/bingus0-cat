package com.example.eventmaster.ui.organizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentManager;
import android.os.Bundle;
import android.view.View;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.EventAdapter;
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

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            // overlay visibility will auto-sync via listener
        } else {
            super.onBackPressed();
        }
    }
}
