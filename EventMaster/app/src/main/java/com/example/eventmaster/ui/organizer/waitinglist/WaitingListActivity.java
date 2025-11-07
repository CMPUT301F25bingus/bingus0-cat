package com.example.eventmaster.ui.organizer.waitinglist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.model.WaitingListEntry;

import java.util.ArrayList;
import java.util.List;

public class WaitingListActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListActivity";
    private RecyclerView recyclerView;
    private WaitingListAdapter adapter;
    private TextView totalCountText;
    private TextView drawReplacementText;
    private TextView btnBack;

    private final WaitingListRepositoryFs waitingRepo = new WaitingListRepositoryFs();
    private final LotteryServiceFs lotteryService = new LotteryServiceFs();

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_waiting_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewWaitingList);
        totalCountText = findViewById(R.id.textTotalCount);
        drawReplacementText = findViewById(R.id.textDrawReplacement);
        btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitingListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Back button to return to View Entrants
        btnBack.setOnClickListener(v -> finish());

        // Load waiting list initially
        loadWaitingList(eventId);

        // 🎲 Run Lottery Click
        drawReplacementText.setOnClickListener(v -> {
            Toast.makeText(this, "Running Lottery...", Toast.LENGTH_SHORT).show();
            runLottery(eventId, 3); // pick 3 entrants for now
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data when returning
        loadWaitingList(eventId);
    }

    private void loadWaitingList(String eventId) {
        waitingRepo.getWaitingList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                adapter.updateList(entries);
                totalCountText.setText("Total waitlisted entrants: " + entries.size());
                Log.d(TAG, "Loaded " + entries.size() + " entrants");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching waiting list", e);
                Toast.makeText(WaitingListActivity.this,
                        "Failed to load waiting list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void runLottery(String eventId, int count) {
        lotteryService.drawLottery(eventId, count)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Lottery Completed! ✓", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Lottery completed successfully");
                    } else {
                        // Even if some operations failed, the lottery might have partially worked
                        Toast.makeText(this, "Lottery completed with warnings", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "Lottery completed with errors", task.getException());
                    }
                    
                    // Wait 1 second for Firebase to fully sync, then reload
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        loadWaitingList(eventId);
                    }, 1000);
                });
    }
}
