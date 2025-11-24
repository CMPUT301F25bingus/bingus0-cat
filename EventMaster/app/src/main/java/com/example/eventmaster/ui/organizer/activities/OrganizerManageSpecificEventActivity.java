package com.example.eventmaster.ui.organizer.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.ui.organizer.fragments.OrganizerEntrantsHubFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

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
    private TextView eventType;


    // Action buttons
    private MaterialButton btnViewEntrants;
    private MaterialButton btnRunLottery;
    private MaterialButton btnNotifications;
    private MaterialButton btnCancelEvent;
    private MaterialButton btnViewMap;
    private ImageView editPosterIcon;
    private Uri newPosterUri = null;

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

        // BACK BUTTON
        backButton.setOnClickListener(v -> onBackPressed());

        // BUTTON ACTIONS
        btnViewEntrants.setOnClickListener(v -> openEntrantsHub());
//        btnRunLottery.setOnClickListener(v -> runLottery());

        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Notifications — coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnCancelEvent.setOnClickListener(v ->
                Toast.makeText(this, "Cancel Event — coming soon!", Toast.LENGTH_SHORT).show()
        );

        editPosterIcon.setOnClickListener(v -> pickNewPoster.launch("image/*"));

        btnViewMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, OrganizerEntrantMapActivity.class);
            mapIntent.putExtra("eventId", eventId);
            startActivity(mapIntent);
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                fragmentContainer.setVisibility(View.GONE);
            }
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
        btnCancelEvent = findViewById(R.id.btnCancelEvent);

        btnViewMap = findViewById(R.id.btnViewMap);
        editPosterIcon = findViewById(R.id.edit_poster_icon);
        eventType = findViewById(R.id.event_type_text);

        fragmentContainer = findViewById(R.id.fragment_container);
    }

    private final ActivityResultLauncher<String> pickNewPoster =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    newPosterUri = uri;
                    Glide.with(this).load(uri).into(eventPoster);
                    uploadUpdatedPoster(uri);
                }
            });

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

                    // Poster
                    String posterUrl = doc.getString("posterUrl");
                    if (posterUrl != null && !posterUrl.isEmpty()) {
                        Glide.with(this).load(posterUrl).into(eventPoster);
                    }

                    eventName.setText(doc.getString("title"));
                    // Replace organizerId with organizerName (fallback to ID)
                    String organizerId = doc.getString("organizerId");

                    if (organizerId != null) {
                        FirebaseFirestore.getInstance()
                                .collection("profiles")
                                .document(organizerId)
                                .get()
                                .addOnSuccessListener(profileDoc -> {
                                    if (profileDoc.exists()) {
                                        String organizerName = profileDoc.getString("name");

                                        if (organizerName != null && !organizerName.isEmpty()) {
                                            eventOrganizer.setText("Hosted by: " + organizerName);
                                        } else {
                                            eventOrganizer.setText("Hosted by: " + organizerId);
                                        }

                                    } else {
                                        eventOrganizer.setText("Hosted by: " + organizerId);
                                    }
                                })
                                .addOnFailureListener(e -> eventOrganizer.setText("Hosted by: " + organizerId));
                    } else {
                        eventOrganizer.setText("Hosted by: Unknown");
                    }

                    // Price
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

                    String type = doc.getString("eventType");
                    if (type != null && !type.isEmpty()) {
                        eventType.setText("Type: " + type);
                    } else {
                        eventType.setText("Type: Not specified");
                    }

                    com.google.firebase.Timestamp start = doc.getTimestamp("registrationOpen");
                    com.google.firebase.Timestamp end = doc.getTimestamp("registrationClose");

                    if (start != null && end != null) {
                        String s = new java.text.SimpleDateFormat("MMM d").format(start.toDate());
                        String e = new java.text.SimpleDateFormat("MMM d").format(end.toDate());
                        eventDates.setText("📅 " + s + " → " + e);

                        // ---------- LOTTERY BUTTON LOCK LOGIC ----------
                        Date now = new Date();

                        if (now.before(end.toDate())) {
                            // Registration still open → DISABLE lottery button
                            btnRunLottery.setEnabled(false);
                            btnRunLottery.setAlpha(0.4f);    // Dim the button visually

                            btnRunLottery.setOnClickListener(v ->
                                    Toast.makeText(
                                            this,
                                            "For fairness, the lottery can only be run after registration closes.",
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                        } else {
                            // Registration closed → enable lottery normally
                            btnRunLottery.setEnabled(true);
                            btnRunLottery.setAlpha(1f);

                            btnRunLottery.setOnClickListener(v -> runLottery());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void uploadUpdatedPoster(Uri uri) {
        if (uri == null) return;

        Toast.makeText(this, "Updating poster...", Toast.LENGTH_SHORT).show();

        try {
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("events/" + eventId + "/poster.jpg");

            byte[] bytes = readAllBytes(uri);

            ref.putBytes(bytes)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(url -> {
                        FirebaseFirestore.getInstance()
                                .collection("events")
                                .document(eventId)
                                .update("posterUrl", url.toString())
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "Poster updated!", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed updating Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        } catch (Exception e) {
            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readAllBytes(Uri uri) throws IOException {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
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

    // Map navigation
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
                                        .addOnSuccessListener(aVoid -> {

                                            Toast.makeText(
                                                    this,
                                                    "Lottery completed! Invitations sent.",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            // 👉 NOW ask for reply-by date
                                            showReplyByDatePicker();

                                        })
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

    // Hide the overlay when back is pressed
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            fragmentContainer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
    private void showReplyByDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    selected.set(Calendar.HOUR_OF_DAY, 23);
                    selected.set(Calendar.MINUTE, 59);
                    selected.set(Calendar.SECOND, 59);

                    Date replyByDate = selected.getTime();

                    saveReplyByDateToInvitations(replyByDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        picker.setTitle("Select Reply-By Date for Invitations");
        picker.show();
    }

    private void saveReplyByDateToInvitations(Date replyByDate) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events")
                .document(eventId)
                .collection("invitations")
                .get()
                .addOnSuccessListener(snap -> {

                    db.runBatch(batch -> {
                        for (var doc : snap.getDocuments()) {
                            batch.update(doc.getReference(), "replyBy", replyByDate);
                        }
                    }).addOnSuccessListener(unused ->
                            Toast.makeText(
                                    this,
                                    "Reply-by date set for all invitations!",
                                    Toast.LENGTH_LONG
                            ).show()
                    ).addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "Failed to update invitations: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                });
    }



}
