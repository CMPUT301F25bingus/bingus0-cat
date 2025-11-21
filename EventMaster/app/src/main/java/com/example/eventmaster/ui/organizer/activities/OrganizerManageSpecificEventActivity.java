package com.example.eventmaster.ui.organizer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.ui.organizer.fragments.OrganizerEntrantsHubFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class OrganizerManageSpecificEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    private String eventId;
    private String eventTitle;

    // UI ELEMENTS
    private ImageView backButton;
    private ImageView eventPoster;
    private TextView eventName;
    private TextView eventOrganizer;
    private TextView eventPrice;
    private TextView eventLocation;
    private TextView eventCapacity;
    private TextView eventDescription;
    private TextView eventDates;

    // Action buttons
    private MaterialButton btnViewEntrants;
    private MaterialButton btnRunLottery;
    private MaterialButton btnNotifications;
    private MaterialButton btnEditEvent;
    private MaterialButton btnCancelEvent;
    private MaterialButton btnViewMap; // <-- YOUR MAP BUTTON

    private FrameLayout fragmentContainer;

    private final LotteryServiceFs lotteryService = new LotteryServiceFs();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.organizer_manage_specific_event_page);

        Intent i = getIntent();
        eventId = i.getStringExtra(EXTRA_EVENT_ID);
        eventTitle = i.getStringExtra(EXTRA_EVENT_TITLE);

        if (eventTitle == null || eventTitle.isEmpty()) {
            eventTitle = "Event Details";
        }

        bindViews();
        loadEventDetails();

        // BACK BUTTON ACTION
        backButton.setOnClickListener(v -> finish());

        // EXISTING ACTION BUTTONS
        btnViewEntrants.setOnClickListener(v -> openEntrantsHub());
        btnRunLottery.setOnClickListener(v -> runLottery());

        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Notifications — coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnEditEvent.setOnClickListener(v ->
                Toast.makeText(this, "Edit Event — coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnCancelEvent.setOnClickListener(v ->
                Toast.makeText(this, "Cancel Event — coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnViewMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, OrganizerEntrantMapActivity.class);
            mapIntent.putExtra("eventId", eventId);
            startActivity(mapIntent);
        });

    }

    private void bindViews() {
        backButton = findViewById(R.id.back_button);

        eventPoster = findViewById(R.id.event_poster_image);
        eventName = findViewById(R.id.event_name_text);
        eventOrganizer = findViewById(R.id.event_organizer_text);
        eventPrice = findViewById(R.id.event_price_text);
        eventLocation = findViewById(R.id.event_location_text);
        eventCapacity = findViewById(R.id.event_capacity_text);
        eventDescription = findViewById(R.id.event_description_text);
        eventDates = findViewById(R.id.event_date_text);

        btnViewEntrants = findViewById(R.id.btnViewEntrants);
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnEditEvent = findViewById(R.id.btnEditEvent);
        btnCancelEvent = findViewById(R.id.btnCancelEvent);

        btnViewMap = findViewById(R.id.btnViewMap);


        fragmentContainer = findViewById(R.id.fragment_container);
    }

    private void loadEventDetails() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean geo = doc.getBoolean("geolocationRequired");
                    btnViewMap.setVisibility(geo != null && geo ? View.VISIBLE : View.GONE);

                    if (geo != null && geo) {
                        btnViewMap.setVisibility(View.VISIBLE);
                    } else {
                        btnViewMap.setVisibility(View.GONE);
                    }

                    // --------------------------------------

                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // POSTER
                    String posterUrl = doc.getString("posterUrl");
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        Glide.with(this).load(posterUrl).into(eventPoster);
                    }

                    eventName.setText(doc.getString("title"));
                    eventOrganizer.setText("Hosted by: " + doc.getString("organizerId"));

                    Double price = doc.getDouble("price");
                    if (price != null) {
                        if (price % 1 == 0)
                            eventPrice.setText("$" + price.intValue());
                        else
                            eventPrice.setText(String.format("$%.2f", price));
                    }

                    eventLocation.setText("📍 " + doc.getString("location"));

                    Long cap = doc.getLong("capacity");
                    if (cap != null) eventCapacity.setText("👥 " + cap);

                    eventDescription.setText(doc.getString("description"));

                    com.google.firebase.Timestamp start = doc.getTimestamp("registrationOpen");
                    com.google.firebase.Timestamp end = doc.getTimestamp("registrationClose");

                    if (start != null && end != null) {
                        String s = new java.text.SimpleDateFormat("MMM d").format(start.toDate());
                        String e = new java.text.SimpleDateFormat("MMM d").format(end.toDate());
                        eventDates.setText("📅 " + s + " → " + e);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void openEntrantsHub() {
        fragmentContainer.setVisibility(View.VISIBLE);
        fragmentContainer.bringToFront();

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
    }

    // NEW METHOD: Navigate to map page
    private void openMapScreen() {
        Intent i = new Intent(this, OrganizerEntrantMapActivity.class);
        i.putExtra("eventId", eventId);
        startActivity(i);
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

                    Long capLong = doc.getLong("capacity");
                    if (capLong == null) {
                        Toast.makeText(this, "Capacity not set.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int capacity = Math.toIntExact(capLong);

                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Run Lottery")
                            .setMessage("Select " + capacity + " winners from the waiting list?")
                            .setPositiveButton("Run", (d, w) -> {
                                LotteryServiceFs lottery = new LotteryServiceFs();
                                lottery.drawLottery(eventId, capacity)
                                        .addOnSuccessListener(aVoid ->
                                                Toast.makeText(this, "Lottery completed!", Toast.LENGTH_SHORT).show()
                                        )
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Lottery failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed loading event.", Toast.LENGTH_SHORT).show()
                );
    }
}
