package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.ChosenListAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays all entrants who were chosen by the lottery.
 * This screen allows the organizer to view selected entrants and cancel all
 * pending invitations if needed. Cancelling pending invitations will move those
 * entrants into the cancelled entrants list and clear the chosen_list collection.
 */
public class ChosenListActivity extends AppCompatActivity {

    private static final String TAG = "ChosenListActivity";

    private RecyclerView recyclerView;
    private ChosenListAdapter adapter;
    private TextView totalChosenText;

    private List<WaitingListEntry> chosenList = new ArrayList<>();

    private final WaitingListRepositoryFs repo = new WaitingListRepositoryFs();
    private final RegistrationServiceFs registrationService = new RegistrationServiceFs();

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_chosen_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "No eventId provided!");
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewChosenList);
        totalChosenText = findViewById(R.id.textTotalChosen);
        MaterialToolbar btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChosenListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Cancel entrants link
        TextView cancelEntrantsLink = findViewById(R.id.cancel_entrants_link);
        cancelEntrantsLink.setOnClickListener(v -> cancelAllPendingInvitations());

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        loadChosenList(eventId);
    }

    /**
     * Loads all entrants currently in the chosen_list collection for the event.
     * Updates the UI list and total count.
     *
     * @param eventId The Firestore event ID.
     */
    private void loadChosenList(String eventId) {
        repo.getChosenList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {

                chosenList = entries;  //Save list for cancellation

                adapter.updateList(entries);
                totalChosenText.setText("Total chosen entrants: " + entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                totalChosenText.setText("Failed to load chosen entrants");
                Log.e(TAG, "Error loading chosen list", e);
            }
        });
    }

    /**
     * Cancels all invitations that are still pending. This changes the registration
     * status to CANCELLED_BY_ORGANIZER, updates the invitation document, and then
     * clears the chosen_list collection. Used when the organizer wants to revoke
     * all outstanding invites at once.
     */
    private void cancelAllPendingInvitations() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("invitations")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(invSnap -> {

                    if (invSnap.isEmpty()) {
                        Toast.makeText(this, "No pending invitations to cancel", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : invSnap.getDocuments()) {

                        // Get entrantId from document field, not doc ID
                        String entrantId = doc.getString("entrantId");

                        if (entrantId == null || entrantId.isEmpty()) {
                            Log.e(TAG, "Invitation missing entrantId: " + doc.getId());
                            continue;
                        }

                        // 1) Cancel the registration using your service
                        registrationService.cancel(
                                eventId,
                                entrantId,
                                true,   // cancelled BY ORGANIZER
                                (v) -> Log.d(TAG, "Cancelled registration for: " + entrantId),
                                (err) -> Log.e(TAG, "Failed to cancel " + entrantId, err)
                        );

                        // 2) Update the invitation status
                        db.collection("events")
                                .document(eventId)
                                .collection("invitations")
                                .document(doc.getId())
                                .update("status", "CANCELLED_BY_ORGANIZER");
                    }

                    Toast.makeText(this, "Cancelled all pending invitations", Toast.LENGTH_SHORT).show();
                    deleteChosenListCollection(eventId);
//                    loadChosenList(eventId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading invitations", e);
                    Toast.makeText(this, "Error cancelling entrants", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes all documents inside the chosen_list collection after pending
     * invitations are cancelled. Afterwards, the chosen list UI is refreshed.
     *
     * @param eventId The Firestore event ID.
     */
    private void deleteChosenListCollection(String eventId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("chosen_list")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("ChosenList", "No chosen_list docs to delete.");
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        doc.getReference().delete()
                                .addOnSuccessListener(v -> Log.d("ChosenList", "Deleted " + doc.getId()))
                                .addOnFailureListener(e -> Log.e("ChosenList", "Error deleting doc", e));
                    }

                    Log.d("ChosenList", "chosen_list collection cleared.");
                    loadChosenList(eventId);

                })
                .addOnFailureListener(e -> {
                    Log.e("ChosenList", "Failed to load chosen_list", e);
                });
    }


}
