package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.ui.organizer.fragments.OrganizerEntrantsHubFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizerManageSpecificEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    private String eventId;
    private String eventTitle;

    private FrameLayout fragmentContainer;
    private final LotteryServiceFs lotteryService = new LotteryServiceFs();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_manage_specific_event_page);

        // Retrieve event ID and title
        Intent i = getIntent();
        eventId = i.getStringExtra(EXTRA_EVENT_ID);
        eventTitle = i.getStringExtra(EXTRA_EVENT_TITLE);

        if (eventTitle == null || eventTitle.isEmpty()) {
            eventTitle = "Manage Event";
        }

        // Top toolbar
        MaterialToolbar toolbar = findViewById(R.id.topBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Big header
        TextView titleHeader = findViewById(R.id.manageEventTitle);
        titleHeader.setText(eventTitle);

        // Fragment container
        fragmentContainer = findViewById(R.id.fragment_container);

        // "View Entrants" : open hub fragment
        findViewById(R.id.btnViewEntrants).setOnClickListener(v ->
                openEntrantsHub()
        );


        // now to have lottery running:

        findViewById(R.id.btnRunLottery).setOnClickListener(v -> {
            runLottery();
        });

        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            // TODO: Notifications
            Toast.makeText(this, "Notifications — coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnEditEvent).setOnClickListener(v -> {
            // TODO: Edit event
            Toast.makeText(this, "Edit Event — coming soon!", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnCancelEvent).setOnClickListener(v -> {
            // TODO: Cancel event
            Toast.makeText(this, "Cancel Event — coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void openEntrantsHub() {
        fragmentContainer.setVisibility(View.VISIBLE);

        OrganizerEntrantsHubFragment frag =
                OrganizerEntrantsHubFragment.newInstance(eventId);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
        );
        ft.replace(R.id.fragment_container, frag);
        ft.addToBackStack("entrantsHub");
        ft.commit();

        // Handle back button properly
        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                            getSupportFragmentManager().popBackStack();
                            fragmentContainer.setVisibility(View.GONE);
                        } else {
                            finish();
                        }
                    }
                }
        );
    }

    private void runLottery() {

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Read capacity (required)
                    Long capLong = doc.getLong("capacity");
                    if (capLong == null) {
                        Toast.makeText(this, "Capacity not set for this event.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity = capLong.intValue();

                    // Show confirmation dialog
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Run Lottery")
                            .setMessage(
                                    "This will select **" + capacity + "** winners from the waiting list.\n\n" +
                                            "All other entrants will receive a 'not selected' notification.\n\n" +
                                            "You cannot undo this action.\n\n" +
                                            "Proceed?"
                            )
                            .setPositiveButton("Run Lottery", (d, w) -> {

                                Toast.makeText(this, "Running lottery...", Toast.LENGTH_SHORT).show();

                                LotteryServiceFs lottery = new LotteryServiceFs();

                                lottery.drawLottery(eventId, capacity)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this,
                                                    "Lottery completed successfully!",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this,
                                                    "Lottery failed: " + e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();

                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load event capacity.", Toast.LENGTH_SHORT).show();
                });
    }

}
