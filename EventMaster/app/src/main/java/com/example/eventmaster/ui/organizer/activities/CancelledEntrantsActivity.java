package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.adapters.CancelledEntrantsAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CancelledEntrantsActivity extends AppCompatActivity {

    private static final String TAG = "CancelledEntrants";

    private String eventId;

    private RecyclerView recyclerView;
    private CancelledEntrantsAdapter adapter;
    private TextView totalCountText;
    private MaterialToolbar backButton;

    private final List<Profile> cancelledProfiles = new ArrayList<>();
    private final List<String> cancelledStatuses = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_cancelled_entrants);

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this, "Missing event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.cancelled_entrants_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CancelledEntrantsAdapter();
        recyclerView.setAdapter(adapter);

        totalCountText = findViewById(R.id.total_selected_count);
        backButton = findViewById(R.id.back_button_container);

        backButton.setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.cancelledEntrantsTitle);
        if (title != null) title.setText("Cancelled Entrants");

        loadCancelledFromFirestore();
    }

    private void loadCancelledFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList(
                        "CANCELLED_BY_ORGANIZER",
                        "CANCELLED_BY_ENTRANT"
                ))
                .get()
                .addOnSuccessListener(snap -> {
                    Log.d(TAG, "Found " + snap.size() + " cancelled entrants");

                    if (snap.isEmpty()) {
//                        adapter.updateCancelledEntrants(cancelledProfiles, cancelledStatuses);
                        adapter.updateCancelledEntrants(
                                new ArrayList<>(cancelledProfiles),
                                new ArrayList<>(cancelledStatuses)
                        );

                        updateCount();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String deviceId = doc.getId();
                        String status = doc.getString("status");

                        cancelledStatuses.add(status);
                        loadProfile(deviceId);
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving cancelled entrants", e);
                    Toast.makeText(this, "Error loading cancelled entrants", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfile(String deviceId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("profiles")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {

                    if (!snap.isEmpty()) {
                        Profile p = snap.getDocuments().get(0).toObject(Profile.class);
                        cancelledProfiles.add(p);
                    }

                    adapter.updateCancelledEntrants(
                            new ArrayList<>(cancelledProfiles),
                            new ArrayList<>(cancelledStatuses)
                    );

                    updateCount();

                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed loading profile for " + deviceId, e)
                );
    }

    private void updateCount() {
        totalCountText.setText("Total cancelled entrants: " + cancelledProfiles.size());
    }
}
