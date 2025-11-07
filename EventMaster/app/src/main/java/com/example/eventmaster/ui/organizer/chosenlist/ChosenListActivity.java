package com.example.eventmaster.ui.organizer.chosenlist;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.WaitingListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying the list of entrants chosen by the lottery.
 * 
 * Shows entrants who have been selected but haven't yet responded (accepted/declined).
 * Once they respond, they move to either Selected (accepted) or Cancelled (declined) lists.
 */
public class ChosenListActivity extends AppCompatActivity {

    private static final String TAG = "ChosenListActivity";
    private RecyclerView recyclerView;
    private ChosenListAdapter adapter;
    private TextView totalChosenText;
    private final WaitingListRepositoryFs repo = new WaitingListRepositoryFs();
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_chosen_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Log.e(TAG, "No eventId provided!");
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewChosenList);
        totalChosenText = findViewById(R.id.textTotalChosen);
        TextView btnBack = findViewById(R.id.btnBack);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChosenListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        loadChosenList(eventId);
    }

    private void loadChosenList(String eventId) {
        repo.getChosenList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                adapter.updateList(entries);
                totalChosenText.setText("Total chosen entrants: " + entries.size());
            }

            @Override
            public void onFailure(Exception e) {
                totalChosenText.setText("Failed to load chosen entrants");
                Log.e("ChosenList", "Error loading chosen list", e);
            }
        });
    }
}

