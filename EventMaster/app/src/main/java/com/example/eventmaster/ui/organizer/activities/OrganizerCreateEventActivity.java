/**
 * OrganizerCreateEventActivity
 *
 * Role:
 *  - Allows organizers (authenticated via email/password) to publish new events.
 *  - Organizer selects poster image (optional), chooses registration window, sets details, uploads to Firestore.
 *  - Creates event document:
 *      fields: title, description, location, registrationOpen, registrationClose,
 *              organizerId, posterUrl, qrUrl, createdAt, capacity.
 *  - Uploads poster to Firebase Storage, optionally generates QR code.
 *
 * Authentication:
 *  - Organizers are already authenticated via OrganizerLoginActivity.
 *  
 * Storage:
 *  - Firebase Storage uses paths: events/{eventId}/poster.jpg and events/{eventId}/qr.png
 *
 * Outstanding:
 *  - Registration period enforcement is handled on the Entrant side.
 *  - This class focuses only on event creation & uploading.
 */

package com.example.eventmaster.ui.organizer.activities;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.MainActivity;
import com.example.eventmaster.R;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class OrganizerCreateEventActivity extends AppCompatActivity {

    // UI Components
    private ImageView imgPosterPreview;
    private android.widget.ImageButton btnPickPoster;
    public MaterialButton tvRegOpen, tvRegClose, tvEventDate;
    private MaterialButton btnPublish;
    private MaterialCheckBox cbGenerateQr;
    private TextInputEditText editTitle, editDescription, editLocation, editCapacity, editWaitingLimit;
    private ProgressBar progress;
    private MaterialCheckBox cbRequireLocation;
    private TextInputEditText editPrice;
    private AutoCompleteTextView eventTypeDropdown;
    private String selectedEventType = null;


    // Local state
    private Uri posterUri = null;
    public String regStartIso = null;
    public String regEndIso = null;
    public String eventDateIso = null;

    // Poster picker launcher
    private final ActivityResultLauncher<String> pickPoster =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Show confirmation dialog with image preview
                    showPosterConfirmationDialog(uri);
                }
            });

    // Permission for poster selection (Android 13+)
    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) pickPoster.launch("image/*");
                else toast("Permission denied.");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * IMPORTANT:
         * We DO NOT sign in anonymously here.
         * Organizers are already authenticated via OrganizerLoginActivity.
         * Anonymous sign-in would override the authenticated session and break organizer IDs.
         */

        setContentView(R.layout.organizer_activity_create_event);

        // Bind views
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        btnPickPoster = findViewById(R.id.btnPickPoster);
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        tvRegOpen = findViewById(R.id.tvRegOpen);
        tvRegClose = findViewById(R.id.tvRegClose);
        tvEventDate = findViewById(R.id.tvEventDate);
        cbGenerateQr = findViewById(R.id.cbGenerateQr);
        btnPublish = findViewById(R.id.btnPublish);
        progress = findViewById(R.id.progress);
        editCapacity = findViewById(R.id.editCapacity);
        cbRequireLocation = findViewById(R.id.cbRequireLocation);
        editPrice = findViewById(R.id.editPrice);
        editWaitingLimit = findViewById(R.id.editWaitingListLimit);




        // Back button handler
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // Registration start picker
        tvRegOpen.setOnClickListener(v -> showDatePickerDialog(date -> {
            regStartIso = date;
            tvRegOpen.setText(date);
        }));

        // Registration end picker
        tvRegClose.setOnClickListener(v -> showDatePickerDialog(date -> {
            regEndIso = date;
            tvRegClose.setText(date);
        }));

        // Event date picker (when the event actually happens)
        tvEventDate.setOnClickListener(v -> showDatePickerDialog(date -> {
            eventDateIso = date;
            tvEventDate.setText(date);
        }));

        // Poster selection handler
        btnPickPoster.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        // Publish event button
        btnPublish.setOnClickListener(v -> publishEvent());
        eventTypeDropdown = findViewById(R.id.eventTypeDropdown);

    // Generic event type list
        String[] eventTypes = new String[]{
                "Sports",
                "Food",
                "Music",
                "Education",
                "Workshop",
                "Volunteer",
                "Social",
                "Fitness",
                "Family",
                "Arts & Culture",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                eventTypes
        );

        eventTypeDropdown.setAdapter(adapter);

        eventTypeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            selectedEventType = eventTypes[position];
        });

    }

    /**
     * Validates inputs, creates Firestore event document, uploads poster (optional),
     * generates QR (optional), and redirects to Manage Events.
     */
    private void publishEvent() {
        String title = textOf(editTitle);
        String desc = textOf(editDescription);
        String location = textOf(editLocation);
        String capStr = textOf(editCapacity);
        String waitLimitStr = textOf(editWaitingLimit);
        Integer waitingListLimit = null;  // null = unlimited

        if (TextUtils.isEmpty(selectedEventType)) {
            toast("Select an event type");
            return;
        }

        // ---- Input Validation ----
        if (TextUtils.isEmpty(title)) { editTitle.setError("Required"); return; }
        if (TextUtils.isEmpty(regStartIso)) { toast("Pick a start date"); return; }
        if (TextUtils.isEmpty(regEndIso)) { toast("Pick an end date"); return; }
        if (TextUtils.isEmpty(eventDateIso)) { toast("Pick an event date"); return; }

        Timestamp regStart = parseDateToTimestamp(regStartIso);
        Timestamp regEnd = parseDateToTimestamp(regEndIso);
        Timestamp eventDate = parseDateToTimestamp(eventDateIso);

        if (regStart == null || regEnd == null || eventDate == null) { 
            toast("Invalid date/time"); 
            return; 
        }
        if (regEnd.compareTo(regStart) < 0) { 
            toast("Registration end must be after start"); 
            return; 
        }

        if (TextUtils.isEmpty(capStr)) { editCapacity.setError("Required"); return; }
        int capacity;
        try {
            capacity = Integer.parseInt(capStr);
        } catch (Exception e) {
            editCapacity.setError("Invalid number");
            return;
        }
        if (capacity <= 0) { editCapacity.setError("Capacity must be greater than 0"); return; }

        //OPTIONAL Waiting List Limit
        // If empty → waitingListLimit stays null (unlimited)
        if (!TextUtils.isEmpty(waitLimitStr)) {
            try {
                int parsed = Integer.parseInt(waitLimitStr);
                if (parsed < 0) {
                    editWaitingLimit.setError("Limit must be ≥ 0");
                    return;
                }
                waitingListLimit = parsed;   // valid limit
            } catch (Exception e) {
                editWaitingLimit.setError("Invalid number");
                return;
            }
        }

        String priceStr = textOf(editPrice);
        double price;

        if (TextUtils.isEmpty(priceStr)) {
            editPrice.setError("Required");
            return;
        }

        try {
            price = Double.parseDouble(priceStr);
            if (price < 0) {
                editPrice.setError("Price cannot be negative");
                return;
            }
        } catch (Exception e) {
            editPrice.setError("Invalid price");
            return;
        }

        // Organizer identity (already authenticated)
        String organizerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setBusy(true);

        // ---- Create event document ----
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("events").document();
        String eventId = doc.getId();

        // Base event fields
        Map<String, Object> base = new HashMap<>();
        base.put("eventId", eventId);
        base.put("name", title);
        base.put("title", title);
        base.put("description", desc);
        base.put("location", location);
        base.put("registrationOpen", regStart);
        base.put("registrationClose", regEnd);
        base.put("organizerId", organizerId);
        base.put("posterUrl", null);
        base.put("qrUrl", null);
        base.put("capacity", capacity);
        base.put("createdAt", Timestamp.now());
        base.put("geolocationRequired", cbRequireLocation.isChecked());
        base.put("price", price);
        base.put("waitingListLimit", waitingListLimit);
        base.put("eventType", selectedEventType);
        base.put("eventDate", eventDate);


        // Write base event document
        doc.set(base)
                .addOnSuccessListener(unused -> {
                    // Poster upload → then QR (optional)
                    if (posterUri != null) {
                        uploadPoster(doc, eventId, posterUri, () -> maybeGenerateQr(doc, eventId));
                    } else {
                        maybeGenerateQr(doc, eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("Failed to create event: " + e.getMessage());
                });
    }

    /**
     * Decides whether to generate QR based on checkbox.
     * If not checked, publish instantly.
     */
    private void maybeGenerateQr(DocumentReference doc, String eventId) {
        if (!cbGenerateQr.isChecked()) {
            setBusy(false);
            toast("Event published!");
            redirectToDetails(eventId);
            return;
        }
        generateAndUploadQrThenFinish(doc, eventId);
    }

    /**
     * Uploads poster to Storage as events/{eventId}/poster.jpg
     * and updates Firestore with poster URL.
     */
    private void uploadPoster(DocumentReference doc, String eventId, Uri uri, Runnable onDone) {
        try {
            byte[] bytes = readAllBytes(uri);
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("events/" + eventId + "/poster.jpg");

            ref.putBytes(bytes)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful())
                            throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(posterUrl ->
                            doc.update("posterUrl", posterUrl.toString())
                                    .addOnSuccessListener(unused -> onDone.run())
                                    .addOnFailureListener(e -> {
                                        setBusy(false);
                                        toast("Poster URL save failed: " + e.getMessage());
                                    })
                    )
                    .addOnFailureListener(e -> {
                        setBusy(false);
                        toast("Poster upload failed: " + e.getMessage());
                    });

        } catch (Exception e) {
            setBusy(false);
            toast("Poster read failed: " + e.getMessage());
        }
    }

    /**
     * Generates QR bitmap, uploads it to Storage under events/{eventId}/qr.png,
     * updates Firestore, then redirects.
     */
    private void generateAndUploadQrThenFinish(DocumentReference doc, String eventId) {
        Bitmap qr = com.example.eventmaster.utils.QRCodeGenerator.generateQRCode(eventId);

        if (qr == null) {
            setBusy(false);
            toast("QR generation failed.");
            return;
        }

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] qrBytes = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("events/" + eventId + "/qr.png");

        ref.putBytes(qrBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful())
                        throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(qrUrl ->
                        doc.update("qrUrl", qrUrl.toString())
                                .addOnSuccessListener(unused -> {
                                    setBusy(false);
                                    toast("Event published!");
                                    redirectToDetails(eventId);
                                })
                                .addOnFailureListener(e -> {
                                    setBusy(false);
                                    toast("Failed saving QR URL: " + e.getMessage());
                                })
                )
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("QR upload failed: " + e.getMessage());
                });
    }

    /**
     * Redirects back to Manage Events screen after creation.
     */
    private void redirectToDetails(String eventId) {
        Intent i = new Intent(this, OrganizerManageEventsActivity.class);
        i.putExtra("eventId", eventId);
        startActivity(i);
        finish();
    }

    // -----------------------
    // Helper Utilities
    // -----------------------

    private interface DatePicked { void onPick(String yyyyMmDd); }

    /**
     * Shows DatePicker → then TimePicker, returns "yyyy-MM-dd HH:mm".
     */
    private void showDatePickerDialog(DatePicked cb) {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            new android.app.TimePickerDialog(this, (tView, hour, min) -> {
                String dateTime = String.format("%04d-%02d-%02d %02d:%02d",
                        y, m + 1, d, hour, min);
                cb.onPick(dateTime);
            }, 12, 0, true).show();
        }, year, month, day).show();
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    /**
     * Parses "yyyy-MM-dd" or "yyyy-MM-dd HH:mm" into Firestore Timestamp.
     */
    public Timestamp parseDateToTimestamp(String input) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                if (input.contains(" ")) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(input, fmt);
                    long millis = java.util.Date.from(
                            dt.atZone(java.time.ZoneId.systemDefault()).toInstant()
                    ).getTime();
                    return new Timestamp(new java.util.Date(millis));
                } else {
                    LocalDate d = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
                    long millis = java.util.Date.from(
                            d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                    ).getTime();
                    return new Timestamp(new java.util.Date(millis));
                }
            } else {
                // Fallback parsing for API < 26
                String[] p = input.split("[\\s:-]");
                int y = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]) - 1;
                int d = Integer.parseInt(p[2]);
                int h = p.length > 3 ? Integer.parseInt(p[3]) : 0;
                int min = p.length > 4 ? Integer.parseInt(p[4]) : 0;

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(y, m, d, h, min, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                return new Timestamp(cal.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showPosterConfirmationDialog(Uri uri) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Confirm Poster");
        
        // Create a layout for the dialog with image preview
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 16);
        
        // Image preview
        ImageView preview = new ImageView(this);
        preview.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(200)));
        preview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        preview.setAdjustViewBounds(true);
        preview.setImageURI(uri);
        preview.setBackgroundColor(0xFFE0F2F1);
        
        layout.addView(preview);
        
        builder.setView(layout);
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            posterUri = uri;
            imgPosterPreview.setImageURI(uri);
            imgPosterPreview.setVisibility(View.VISIBLE);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Do nothing, user cancelled
        });
        builder.show();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private byte[] readAllBytes(Uri uri) throws IOException {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    /**
     * Controls UI visibility during long operations (poster/QR uploads).
     */
    private void setBusy(boolean busy) {
        btnPublish.setEnabled(!busy);
        btnPickPoster.setEnabled(!busy);
        progress.setVisibility(busy ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
