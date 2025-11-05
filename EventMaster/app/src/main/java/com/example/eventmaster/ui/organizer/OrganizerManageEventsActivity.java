package com.example.eventmaster.ui.organizer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrganizerManageEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents;
    private EventAdapter adapter;
    private final List<Map<String, Object>> eventList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_manage_events);

        // ---------- TOP BAR ----------
        MaterialToolbar topBar = findViewById(R.id.topBar);
        setSupportActionBar(topBar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Events");
        }

        // handle back arrow click
        topBar.setNavigationOnClickListener(v -> onBackPressed());

        // ---------- UI COMPONENTS ----------
        recyclerEvents = findViewById(R.id.recyclerEvents);
        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(eventList);
        recyclerEvents.setAdapter(adapter);

        MaterialButton btnViewCancelled = findViewById(R.id.btnViewCancelled);
        btnViewCancelled.setOnClickListener(v ->
                Toast.makeText(this, "View cancelled events not yet implemented.", Toast.LENGTH_SHORT).show()
        );

        // ---------- FIREBASE ----------
        loadOrganizerEvents();
    }

    /**
     * Fetches events belonging to the currently signed-in organizer.
     */
    private void loadOrganizerEvents() {
        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (organizerId == null) {
            Toast.makeText(this, "No organizer signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(query -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        eventList.add(doc.getData());
                    }
                    adapter.notifyDataSetChanged();

                    if (eventList.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading events: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
