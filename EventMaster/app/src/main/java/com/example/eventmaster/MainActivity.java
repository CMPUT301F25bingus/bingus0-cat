package com.example.eventmaster;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.model.WaitingListEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FirestoreWaitingList";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
    }

    // Join waiting list (adds entrant)
    public void joinWaitingList(WaitingListEntry entry) {
        db.collection("events")
                .document(entry.getEventId())
                .collection("waiting_list")
                .document(entry.getEntrantId())
                .set(entry)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Joined waiting list for " + entry.getEntrantName()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to join waiting list", e));
    }

    // Leave waiting list (removes entrant)
    public void leaveWaitingList(String eventId, String entrantId) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .document(entrantId)
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Left waiting list for event " + eventId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to leave waiting list", e));
    }

    // Get all entrants in waiting list (for organizers)
    public void getWaitingList(String eventId) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .addOnSuccessListener(query -> {
                    List<WaitingListEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        list.add(doc.toObject(WaitingListEntry.class));
                    }
                    Log.d(TAG, "Retrieved " + list.size() + " waiting entrants for " + eventId);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error fetching waiting list", e));
    }
}
