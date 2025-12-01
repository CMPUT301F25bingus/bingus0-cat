package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.organizer.adapters.WaitingListAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying the waiting list for an event.
 * Allows organizers to:
 * - View all entrants in the waiting list
 * - Send waiting-list notifications
 */
public class WaitingListActivity extends AppCompatActivity {

    private static final String TAG = "WaitingListActivity";

    private RecyclerView recyclerView;
    private WaitingListAdapter adapter;
    private TextView totalCountText;
    private ImageButton btnBack;
    private TextView textSendNotification;
    private TextView emptyStateText;

    private final WaitingListRepositoryFs waitingRepo = new WaitingListRepositoryFs();
    private final LotteryServiceFs lotteryService = new LotteryServiceFs();
    private final NotificationService notificationService = new NotificationServiceFs();
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

    private String eventId;
    private List<WaitingListEntry> currentWaitingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_activity_waiting_list);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Error: No event ID provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.recyclerViewWaitingList);
        totalCountText = findViewById(R.id.textTotalCount);
        btnBack = findViewById(R.id.btnBack);
        textSendNotification = findViewById(R.id.textSendNotification);
        emptyStateText = findViewById(R.id.empty_state_text);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitingListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // 📩 Send notification to WAITING LIST (only)
        textSendNotification.setOnClickListener(v -> {
            if (currentWaitingList.isEmpty()) {
                Toast.makeText(this, "No entrants on waiting list to notify", Toast.LENGTH_SHORT).show();
                return;
            }
            showSendNotificationDialog();
        });

        // Load initial waiting list
        loadWaitingList(eventId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWaitingList(eventId);
    }

    private void loadWaitingList(String eventId) {
        waitingRepo.getWaitingList(eventId, new WaitingListRepositoryFs.OnListLoadedListener() {
            @Override
            public void onSuccess(List<WaitingListEntry> entries) {
                currentWaitingList = entries;
                adapter.updateList(entries);
                totalCountText.setText("Total waitlisted entrants: " + entries.size());
                Log.d(TAG, "Loaded " + entries.size() + " waiting entrants");
                
                // Show/hide empty state
                if (entries.isEmpty()) {
                    if (emptyStateText != null) emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    if (emptyStateText != null) emptyStateText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error fetching waiting list", e);
                Toast.makeText(WaitingListActivity.this,
                        "Failed to load waiting list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows confirmation to notify waiting list.
     */
    private void showSendNotificationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notify Waiting List")
                .setMessage("Notify " + currentWaitingList.size() + " entrants on the waiting list?")
                .setPositiveButton("Send", (dialog, which) -> sendNotificationToWaitingList())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Sends notification to WAITING LIST entrants only.
     */
    private void sendNotificationToWaitingList() {
        if (currentWaitingList.isEmpty()) {
            Toast.makeText(this, "No entrants on waiting list", Toast.LENGTH_SHORT).show();
            return;
        }

        textSendNotification.setEnabled(false);
        Toast.makeText(this, "Sending notifications...", Toast.LENGTH_SHORT).show();

        fetchEventNameAndNotify();
    }

    private void fetchEventNameAndNotify() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    String eventName = doc.exists() && doc.getString("name") != null
                            ? doc.getString("name")
                            : "Event";
                    notifyWaitingList(eventName);
                })
                .addOnFailureListener(e -> {
                    notifyWaitingList("Event");
                });
    }

    private void notifyWaitingList(String eventName) {

        List<Profile> profiles = new ArrayList<>();
        for (WaitingListEntry entry : currentWaitingList) {
            Profile p = entry.getProfile();
            if (p != null) {
                if (p.getUserId() == null || p.getUserId().isEmpty()) {
                    p.setUserId(p.getId());
                }
                profiles.add(p);
            }
        }

        if (profiles.isEmpty()) {
            Toast.makeText(this, "No profiles found", Toast.LENGTH_SHORT).show();
            textSendNotification.setEnabled(true);
            return;
        }

        String title = "📢 " + eventName + " – Waiting List Update";
        String message = eventName + ": You are currently on the waiting list.";

        notificationService.sendNotificationToWaitingList(
                eventId,
                profiles,
                title,
                message,
                () -> handleSuccess(profiles.size()),
                err -> handleFailure(err)
        );
    }

    private void handleSuccess(int count) {
        runOnUiThread(() -> {
            textSendNotification.setEnabled(true);
            Toast.makeText(this, "Sent to " + count + " waiting entrants!", Toast.LENGTH_LONG).show();
            Log.i(TAG, "Waiting list notifications sent.");
        });
    }

    private void handleFailure(String error) {
        runOnUiThread(() -> {
            textSendNotification.setEnabled(true);
            Toast.makeText(this, "Failed: " + error, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Notification failed: " + error);
        });
    }
}
