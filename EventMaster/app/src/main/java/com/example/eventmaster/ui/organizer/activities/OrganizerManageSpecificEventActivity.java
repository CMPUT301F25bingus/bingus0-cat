package com.example.eventmaster.ui.organizer.activities;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.LotteryServiceFs;
import com.example.eventmaster.ui.organizer.fragments.OrganizerEntrantsHubFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * Activity used by organizers to view and manage a specific event.
 * This screen loads event details, displays the poster, capacity,
 * date range, pricing, organizer name, and allows quick access to
 * all associated management tools.
 *
 * The organizer can:
 * - View entrants through the Entrants Hub
 * - Run the lottery once registration has closed
 * - Edit the event poster
 * - View the event location on a map (if geolocation is enabled)
 * - Open a fragment overlay for additional management tools
 * - Assign reply-by dates to invitations after running the lottery
 *
 * The activity retrieves the selected event using the eventId passed
 * through the Intent and keeps all UI in sync with Firestore.
 */
public class OrganizerManageSpecificEventActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    public static final String EXTRA_EVENT_TITLE = "eventTitle";

    private String eventId;
    private String eventTitle;

    // UI ELEMENTS
    private ImageButton backButton;
    private ImageView eventPoster;
    private TextView eventName;
    private TextView eventOrganizer;
    private TextView eventPrice;
    private TextView eventLocation;
    private TextView eventCapacity;
    private TextView eventDescription;
    private TextView eventDates;
    private TextView eventRegistrationStart;
    private TextView eventRegistrationEnd;
    private TextView eventType;

    // Description editing
    private MaterialButton btnEditDescription;
    private MaterialButton btnSaveDescription;
    private MaterialButton btnCancelDescription;
    private TextInputLayout layoutDescription;
    private TextInputEditText editDescription;
    private LinearLayout descriptionEditButtons;
    private boolean isEditingDescription = false;

    // Action buttons
    private MaterialButton btnViewEntrants;
    private MaterialButton btnRunLottery;
    private MaterialButton btnViewMap;
    private MaterialButton btnDeleteEvent;
    private ImageButton editPosterIcon;
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


        editPosterIcon.setOnClickListener(v -> pickNewPoster.launch("image/*"));

        btnViewMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, OrganizerEntrantMapActivity.class);
            mapIntent.putExtra("eventId", eventId);
            startActivity(mapIntent);
        });

        // Description editing
        btnEditDescription.setOnClickListener(v -> enterDescriptionEditMode());
        btnSaveDescription.setOnClickListener(v -> saveDescription());
        btnCancelDescription.setOnClickListener(v -> exitDescriptionEditMode());

        // Delete event
        btnDeleteEvent.setOnClickListener(v -> confirmDeleteEvent());

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
        eventRegistrationStart = findViewById(R.id.event_registration_start_text);
        eventRegistrationEnd = findViewById(R.id.event_registration_end_text);
        eventType = findViewById(R.id.event_type_text);

        // Description editing
        btnEditDescription = findViewById(R.id.btnEditDescription);
        btnSaveDescription = findViewById(R.id.btnSaveDescription);
        btnCancelDescription = findViewById(R.id.btnCancelDescription);
        layoutDescription = findViewById(R.id.layoutDescription);
        editDescription = findViewById(R.id.editDescription);
        descriptionEditButtons = findViewById(R.id.descriptionEditButtons);

        btnViewEntrants = findViewById(R.id.btnViewEntrants);
        btnRunLottery = findViewById(R.id.btnRunLottery);
        btnViewMap = findViewById(R.id.btnViewMap);
        btnDeleteEvent = findViewById(R.id.btnDeleteEvent);
        editPosterIcon = findViewById(R.id.edit_poster_icon);

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

    /**
     * Loads all event details from Firestore and populates the UI.
     * This includes poster, title, organizer name, capacity, dates,
     * description, event type, and lottery button availability.
     */
    private void loadEventDetails() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(doc -> {

                    Boolean geo = doc.getBoolean("geolocationRequired");
                    btnViewMap.setVisibility(geo != null && geo ? View.VISIBLE : View.GONE);

                    if (!doc.exists()) {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // --- POSTER + PLACEHOLDER HANDLING ---
                    ImageView posterImage = findViewById(R.id.event_poster_image);
                    TextView placeholder = findViewById(R.id.poster_placeholder_text);

                    String posterUrl = doc.getString("posterUrl");

                    if (posterUrl != null && !posterUrl.isEmpty()) {

                        Glide.with(this)
                                .load(posterUrl)
                                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {

                                    @Override
                                    public boolean onLoadFailed(
                                            @Nullable GlideException e,
                                            Object model,
                                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                            boolean isFirstResource
                                    ) {
                                        // ❌ Poster failed to load → show placeholder
                                        placeholder.setVisibility(View.VISIBLE);
                                        return false;
                                    }

                                    @Override
                                    public boolean onResourceReady(
                                            android.graphics.drawable.Drawable resource,
                                            Object model,
                                            com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                            com.bumptech.glide.load.DataSource dataSource,
                                            boolean isFirstResource
                                    ) {
                                        // ✔ Poster loaded → hide placeholder
                                        placeholder.setVisibility(View.GONE);
                                        return false;
                                    }
                                })
                                .into(posterImage);

                    } else {
                        // ❌ No poster URL in database
                        placeholder.setVisibility(View.VISIBLE);
                    }

                    // --- NAME ---
                    eventName.setText(doc.getString("title"));

                    // --- ORGANIZER NAME ---
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
                                .addOnFailureListener(e ->
                                        eventOrganizer.setText("Hosted by: " + organizerId)
                                );
                    } else {
                        eventOrganizer.setText("Hosted by: Unknown");
                    }

                    // --- PRICE ---
                    Double price = doc.getDouble("price");
                    if (price != null) {
                        if (price % 1 == 0)
                            eventPrice.setText("$" + price.intValue());
                        else
                            eventPrice.setText(String.format("$%.2f", price));
                    }

                    // --- LOCATION / CAPACITY / DESCRIPTION ---
                    String location = doc.getString("location");
                    eventLocation.setText(location != null && !location.isEmpty() ? location : "Not specified");

                    Long cap = doc.getLong("capacity");
                    if (cap != null) eventCapacity.setText(String.valueOf(cap));

                    eventDescription.setText(doc.getString("description"));

                    // --- EVENT TYPE ---
                    String type = doc.getString("eventType");
                    eventType.setText(type != null && !type.isEmpty()
                            ? type
                            : "Not specified");

                    // --- REGISTRATION DATES ---
                    com.google.firebase.Timestamp regStart = doc.getTimestamp("registrationOpen");
                    com.google.firebase.Timestamp regEnd = doc.getTimestamp("registrationClose");

                    if (regStart != null && regEnd != null) {
                        // Format with date and time
                        java.text.SimpleDateFormat dateTimeFormat = new java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a");
                        String startStr = dateTimeFormat.format(regStart.toDate());
                        String endStr = dateTimeFormat.format(regEnd.toDate());
                        
                        eventRegistrationStart.setText(startStr);
                        eventRegistrationEnd.setText(endStr);

                        // --- LOTTERY BUTTON ENABLE/DISABLE LOGIC ---
                        Date now = new Date();

                        if (now.before(regEnd.toDate())) {
                            // Registration still open → disable lottery
                            btnRunLottery.setEnabled(false);
                            btnRunLottery.setAlpha(0.4f);

                            btnRunLottery.setOnClickListener(v ->
                                    Toast.makeText(
                                            this,
                                            "Lottery can only be run after registration closes.",
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                        } else {
                            // Registration closed → enable lottery
                            btnRunLottery.setEnabled(true);
                            btnRunLottery.setAlpha(1f);

                            btnRunLottery.setOnClickListener(v -> runLottery());
                        }
                    } else {
                        eventRegistrationStart.setText("Not set");
                        eventRegistrationEnd.setText("Not set");
                    }

                    // --- EVENT DATE ---
                    com.google.firebase.Timestamp eventDate = doc.getTimestamp("eventDate");
                    if (eventDate != null) {
                        java.text.SimpleDateFormat dateTimeFormat = new java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a");
                        String eventDateStr = dateTimeFormat.format(eventDate.toDate());
                        eventDates.setText(eventDateStr);
                    } else {
                        // Fallback to registration close date
                        if (regEnd != null) {
                            java.text.SimpleDateFormat dateTimeFormat = new java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a");
                            String eventDateStr = dateTimeFormat.format(regEnd.toDate());
                            eventDates.setText(eventDateStr);
                        } else {
                            eventDates.setText("Not set");
                        }
                    }

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Uploads a new poster image to Firebase Storage, retrieves its
     * download URL, and updates the event document in Firestore.
     *
     * @param uri the selected image file chosen by the organizer.
     */
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
    /**
     * Reads the contents of a given Uri into a byte array so it
     * can be uploaded to Firebase Storage.
     *
     * @param uri the selected image file
     * @return all bytes from the file
     */
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

    /**
     * Opens the Entrants Hub fragment, which provides shortcuts
     * to the selected, cancelled, and waiting list views.
     */
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

    /**
     * Initiates the event lottery once registration is closed.
     * A confirmation dialog is shown, then the LotteryService is
     * used to select winners and send invitations. Afterward,
     * the organizer is prompted to set a reply-by date.
     */
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
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            fragmentContainer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Shows a date picker that lets the organizer choose the reply-by
     * deadline for all invitations created by the lottery.
     */
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

    /**
     * Saves the selected reply-by date into every invitation document
     * under the event. This ensures all chosen entrants share the same
     * deadline for responding.
     *
     * @param replyByDate the date selected in the picker dialog.
     */
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

    /**
     * Enters edit mode for description.
     */
    private void enterDescriptionEditMode() {
        isEditingDescription = true;
        String currentDesc = eventDescription.getText() != null 
            ? eventDescription.getText().toString() 
            : "";
        editDescription.setText(currentDesc);
        eventDescription.setVisibility(View.GONE);
        layoutDescription.setVisibility(View.VISIBLE);
        descriptionEditButtons.setVisibility(View.VISIBLE);
        btnEditDescription.setVisibility(View.GONE);
    }

    /**
     * Exits edit mode for description without saving.
     */
    private void exitDescriptionEditMode() {
        isEditingDescription = false;
        eventDescription.setVisibility(View.VISIBLE);
        layoutDescription.setVisibility(View.GONE);
        descriptionEditButtons.setVisibility(View.GONE);
        btnEditDescription.setVisibility(View.VISIBLE);
    }

    /**
     * Saves the updated description to Firestore.
     */
    private void saveDescription() {
        String newDescription = editDescription.getText() != null 
            ? editDescription.getText().toString().trim() 
            : "";
        
        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .update("description", newDescription)
            .addOnSuccessListener(v -> {
                eventDescription.setText(newDescription);
                exitDescriptionEditMode();
                Toast.makeText(this, "Description updated!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
    }

    /**
     * Shows confirmation dialog before deleting the event.
     */
    private void confirmDeleteEvent() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Event?")
            .setMessage("Are you sure you want to delete this event? This action cannot be undone and will remove all associated data.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, which) -> deleteEvent())
            .show();
    }

    /**
     * Deletes the event from Firestore and navigates back.
     */
    private void deleteEvent() {
        Toast.makeText(this, "Deleting event...", Toast.LENGTH_SHORT).show();
        
        // Delete the event document
        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .delete()
            .addOnSuccessListener(v -> {
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                finish(); // Go back to Manage Events page
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to delete: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
            });
    }

}
