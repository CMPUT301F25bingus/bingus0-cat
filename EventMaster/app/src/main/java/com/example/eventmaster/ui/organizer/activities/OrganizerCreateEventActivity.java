/**
 * OrganizerCreateEventActivity
 *
 * Role:
 *  - Form for organizers to publish a new event.
 *  - Selects poster image (with preview) and uploads to Firebase Storage.
 *  - Sets registration start/end (date + time) and stores as Firestore Timestamps.
 *  - Creates Firestore event doc, generates & uploads QR, then redirects to details.
 *
 * Data:
 *  - Firestore: collection("events")/{eventId}
 *      fields: title, description, location, regStart, regEnd, organizerId, posterUrl, qrUrl, createdAt
 *  - Storage:
 *      events/{eventId}/poster.jpg
 *      events/{eventId}/qr.png
 *
 * Notes / Outstanding:
 *  - Join-window enforcement happens on Entrant side (not in this Activity).
 *  - Anonymous FirebaseAuth sign-in used for Storage writes.
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class OrganizerCreateEventActivity extends AppCompatActivity {

    private MaterialToolbar topBar;
    ImageView imgPosterPreview;
    private MaterialButton btnPickPoster;
    public MaterialButton tvRegOpen;
    public MaterialButton tvRegClose;
    private MaterialButton btnPublish;
    MaterialCheckBox cbGenerateQr;
    private TextInputEditText editTitle, editDescription, editLocation, editCapacity;
    private ProgressBar progress;

    private Uri posterUri = null;
    public String regStartIso = null;
    public String regEndIso = null; // yyyy-MM-dd HH:mm

    private final ActivityResultLauncher<String> pickPoster =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    posterUri = uri;
                    imgPosterPreview.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) pickPoster.launch("image/*");
                else toast("Permission denied.");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Always sign in (anonymous) so Storage uploads work
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> {})
                .addOnFailureListener(e -> Toast.makeText(this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        setContentView(R.layout.organizer_activity_create_event);

        topBar = findViewById(R.id.topBar);
        imgPosterPreview = findViewById(R.id.imgPosterPreview);
        btnPickPoster = findViewById(R.id.btnPickPoster);
        editTitle = findViewById(R.id.editTitle);
        editDescription = findViewById(R.id.editDescription);
        editLocation = findViewById(R.id.editLocation);
        tvRegOpen = findViewById(R.id.tvRegOpen);
        tvRegClose = findViewById(R.id.tvRegClose);
        cbGenerateQr = findViewById(R.id.cbGenerateQr);
        btnPublish = findViewById(R.id.btnPublish);
        progress = findViewById(R.id.progress);
        editCapacity = findViewById(R.id.editCapacity);


        if (topBar != null) topBar.setNavigationOnClickListener(v -> onBackPressed());

        tvRegOpen.setOnClickListener(v -> showDatePickerDialog(date -> {
            regStartIso = date;
            tvRegOpen.setText("Start: " + date);
        }));

        tvRegClose.setOnClickListener(v -> showDatePickerDialog(date -> {
            regEndIso = date;
            tvRegClose.setText("End: " + date);
        }));

        btnPickPoster.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        btnPublish.setOnClickListener(v -> publishEvent());

        //for Testing:
        boolean testMode = getIntent().getBooleanExtra("TEST_MODE", false);
        if (!testMode) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(r -> {})
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

    }

    /**
     * Publishes a new event:
     * 1) validates inputs; 2) writes base event doc; 3) uploads poster (optional);
     * 4) generates + uploads QR (optional via checkbox); 5) navigates to details.
     */
    private void publishEvent() {
        String title = textOf(editTitle);
        String desc = textOf(editDescription);
        String location = textOf(editLocation);
        String capStr = textOf(editCapacity);


        if (TextUtils.isEmpty(title)) { editTitle.setError("Required"); return; }
        if (TextUtils.isEmpty(regStartIso)) { toast("Pick a start date"); return; }
        if (TextUtils.isEmpty(regEndIso)) { toast("Pick an end date"); return; }

        Timestamp regStart = parseDateToTimestamp(regStartIso);
        Timestamp regEnd = parseDateToTimestamp(regEndIso);
        if (regStart == null || regEnd == null) { toast("Invalid date/time"); return; }
        if (regEnd.compareTo(regStart) < 0) { toast("End must be after start"); return; }

        //capacity for US 02.05.02
        if (TextUtils.isEmpty(capStr)) { editCapacity.setError("Required"); return; }
        int capacity = 0;
        try {
            capacity = Integer.parseInt(capStr);
        } catch (NumberFormatException e) {
            editCapacity.setError("Invalid number");
            return;
        }

        if (capacity <= 0) {
            editCapacity.setError("Capacity must be greater than 0");
            return;
        }

        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        setBusy(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("events").document();
        String eventId = doc.getId();

        Map<String, Object> base = new HashMap<>();
        base.put("eventId", eventId);
        // canonical & backward-compatible field names
        base.put("name", title);
        base.put("title", title);
        base.put("description", desc);
        base.put("location", location);

        // use consistent Firestore keys
        base.put("registrationOpen", regStart);
        base.put("registrationClose", regEnd);

        base.put("organizerId", organizerId);
        base.put("posterUrl", null);
        base.put("qrUrl", null);
        base.put("createdAt", Timestamp.now());
        base.put("capacity", capacity);



        doc.set(base).addOnSuccessListener(unused -> {
            if (posterUri != null) {
                uploadPoster(doc, eventId, posterUri, () -> maybeGenerateQr(doc, eventId));
            } else {
                maybeGenerateQr(doc, eventId);
            }

        }).addOnFailureListener(e -> {
            setBusy(false);
            toast("Failed to create event: " + e.getMessage());
        });
    }

    /**
     * After poster upload completes (or if none), decides whether to generate QR.
     * @param doc Firestore document for the event
     * @param eventId ID of the event document
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
     * Uploads the selected poster bytes to Firebase Storage: events/{eventId}/poster.jpg
     * and persists the download URL to the event doc.
     * @param doc Firestore document reference for the event
     * @param eventId event identifier
     * @param uri local content Uri of the poster image
     * @param onDone callback invoked after Firestore is updated
     */
    private void uploadPoster(DocumentReference doc, String eventId, Uri uri, Runnable onDone) {
        try {
            byte[] bytes = readAllBytes(uri);
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("events/" + eventId + "/poster.jpg");
            ref.putBytes(bytes)
                    .continueWithTask(task -> { if (!task.isSuccessful()) throw task.getException(); return ref.getDownloadUrl(); })
                    .addOnSuccessListener(posterUrl ->
                            doc.update("posterUrl", posterUrl.toString())
                                    .addOnSuccessListener(unused -> onDone.run())
                                    .addOnFailureListener(e -> { setBusy(false); toast("Poster URL save failed: " + e.getMessage()); })
                    )
                    .addOnFailureListener(e -> { setBusy(false); toast("Poster upload failed: " + e.getMessage()); });
        } catch (Exception e) {
            setBusy(false);
            toast("Poster read failed: " + e.getMessage());
        }
    }

    /**
     * Generates a QR bitmap for a deep link to this event and uploads it to Storage,
     * then writes the public URL back to the event document and redirects to details.
     * @param doc Firestore document reference
     * @param eventId event identifier
     */
    private void generateAndUploadQrThenFinish(DocumentReference doc, String eventId) {
        Bitmap qr = com.example.eventmaster.utils.QRCodeGenerator.generateQRCode(eventId);

        if (qr == null) {
            setBusy(false);
            toast("QR generation failed.");
            return;
        }

        // Convert bitmap to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] qrBytes = baos.toByteArray();

        // Upload to Firebase Storage
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("events/" + eventId + "/qr.png");

        ref.putBytes(qrBytes)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(qrUrl -> {
                    doc.update("qrUrl", qrUrl.toString())
                            .addOnSuccessListener(unused -> {
                                setBusy(false);
                                toast("Event published!");
                                redirectToDetails(eventId);
                            })
                            .addOnFailureListener(e -> {
                                setBusy(false);
                                toast("Failed saving QR URL: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setBusy(false);
                    toast("QR upload failed: " + e.getMessage());
                });
    }


    /**
     * After successful event creation, redirect the organizer
     * to the Manage Events screen instead of the details page.
     */
    private void redirectToDetails(String eventId) {
        Intent i = new Intent(this, OrganizerManageEventsActivity.class);
        i.putExtra("eventId", eventId);
        startActivity(i);
        finish(); // close create screen
    }


    // ---------- Helpers ----------

    private interface DatePicked { void onPick(String yyyyMmDd); }

    /**
     * Opens a DatePicker followed by a TimePicker; returns "yyyy-MM-dd HH:mm" via callback.
     * @param cb callback receiving the combined date-time string
     */
    private void showDatePickerDialog(DatePicked cb) {
        final java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        new android.app.DatePickerDialog(this, (view, y, m, d) -> {
            // After picking the date, show a time picker
            new android.app.TimePickerDialog(this, (tView, hour, min) -> {
                String dateTime = String.format("%04d-%02d-%02d %02d:%02d", y, m + 1, d, hour, min);
                cb.onPick(dateTime);  // Pass full date+time string
            }, 12, 0, true).show();
        }, year, month, day).show();
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    /**
     * Parses either "yyyy-MM-dd" or "yyyy-MM-dd HH:mm" into a Firestore Timestamp.
     * @param input date or date-time string
     * @return Timestamp or null if parsing fails
     */
    public Timestamp parseDateToTimestamp(String input) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                if (input.contains(" ")) {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    java.time.LocalDateTime dt = java.time.LocalDateTime.parse(input, fmt);
                    long millis = java.util.Date.from(dt.atZone(java.time.ZoneId.systemDefault()).toInstant()).getTime();
                    return new Timestamp(new java.util.Date(millis));
                } else {
                    LocalDate d = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE);
                    long millis = java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()).getTime();
                    return new Timestamp(new java.util.Date(millis));
                }
            } else {
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

    private byte[] readAllBytes(Uri uri) throws IOException {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    public Bitmap createQrBitmap(String data, int size) {
        try {
            com.google.zxing.common.BitMatrix m = new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }

    public byte[] bitmapToPng(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private void setBusy(boolean busy) {
        btnPublish.setEnabled(!busy);
        btnPickPoster.setEnabled(!busy);
        progress.setVisibility(busy ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /**
     * Simple toast helper.
     * @param m message to show
     */
    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }

    // Used only for UI tests — lets Espresso simulate a poster being chosen
    public void testSetPosterPreview(android.net.Uri uri) {
        if (uri != null && imgPosterPreview != null) {
            imgPosterPreview.setImageURI(uri);
        }
    }

}
