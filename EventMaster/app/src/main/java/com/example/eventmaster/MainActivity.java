package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.waitinglist.WaitingListActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListDemo";
    private WaitingListRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new WaitingListRepositoryFs();

        // Create multiple test entries to match mockup
        WaitingListEntry entry1 = new WaitingListEntry("entrant001", "Bingus3", "Bingus3@example.com", "+1 (780) 533-4567", "event001", "waiting");
        WaitingListEntry entry2 = new WaitingListEntry("entrant002", "Bingus4", "Bingus4@example.com", "+1 (780) 533-4567", "event001", "waiting");
        WaitingListEntry entry3 = new WaitingListEntry("entrant003", "Bingus5", "Bingus5@example.com", "+1 (785) 534-1229", "event001", "waiting");
        WaitingListEntry entry4 = new WaitingListEntry("entrant004", "Bingus6", "Bingus6@example.com", "+1 (780) 533-4567", "event001", "waiting");

        // Add all entries
        repo.addEntrant("event001", entry1);
        repo.addEntrant("event001", entry2);
        repo.addEntrant("event001", entry3);
        repo.addEntrant("event001", entry4)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "All entrants added");
                    // Launch the waiting list activity to view it
                    Intent intent = new Intent(MainActivity.this, WaitingListActivity.class);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add entrants", e));
    }
}
