package com.example.eventmaster.ui.organizer;

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
    private ImageView imgPosterPreview;
    private MaterialButton btnPickPoster, tvRegOpen, tvRegClose, btnPublish;
    private MaterialCheckBox cbGenerateQr;
    private TextInputEditText editTitle, editDescription, editLocation;
    private ProgressBar progress;

    private Uri posterUri = null;
    private String regStartIso = null, regEndIso = null; // yyyy-MM-dd HH:mm

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

        setContentView(R.layout.activity_organizer_create_event);

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
    }

    private void publishEvent() {
        String title = textOf(editTitle);
        String desc = textOf(editDescription);
        String location = textOf(editLocation);

        if (TextUtils.isEmpty(title)) { editTitle.setError("Required"); return; }
        if (TextUtils.isEmpty(regStartIso)) { toast("Pick a start date"); return; }
        if (TextUtils.isEmpty(regEndIso)) { toast("Pick an end date"); return; }

        Timestamp regStart = parseDateToTimestamp(regStartIso);
        Timestamp regEnd = parseDateToTimestamp(regEndIso);
        if (regStart == null || regEnd == null) { toast("Invalid date/time"); return; }
        if (regEnd.compareTo(regStart) < 0) { toast("End must be after start"); return; }

        String organizerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        setBusy(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc = db.collection("events").document();
        String eventId = doc.getId();

        Map<String, Object> base = new HashMap<>();
        base.put("title", title);
        base.put("description", desc);
        base.put("location", location);
        base.put("regStart", regStart);
        base.put("regEnd", regEnd);
        base.put("organizerId", organizerId);
        base.put("posterUrl", null);
        base.put("qrUrl", null);
        base.put("createdAt", Timestamp.now());

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

    private void maybeGenerateQr(DocumentReference doc, String eventId) {
        if (!cbGenerateQr.isChecked()) {
            setBusy(false);
            toast("Event published!");
            redirectToDetails(eventId);
            return;
        }
        generateAndUploadQrThenFinish(doc, eventId);
    }

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

    private void generateAndUploadQrThenFinish(DocumentReference doc, String eventId) {
        String deepLink = "eventmaster://event/" + eventId; // replace with HTTPS if needed

        Bitmap qr = createQrBitmap(deepLink, 640);
        if (qr == null) { setBusy(false); toast("QR generation failed."); return; }

        byte[] qrPng = bitmapToPng(qr);
        StorageReference ref = FirebaseStorage.getInstance()
                .getReference("events/" + eventId + "/qr.png");

        ref.putBytes(qrPng)
                .continueWithTask(task -> { if (!task.isSuccessful()) throw task.getException(); return ref.getDownloadUrl(); })
                .addOnSuccessListener(qrUrl ->
                        doc.update("qrUrl", qrUrl.toString())
                                .addOnSuccessListener(unused -> {
                                    setBusy(false);
                                    toast("Event published!");
                                    redirectToDetails(eventId);
                                })
                                .addOnFailureListener(e -> { setBusy(false); toast("Failed saving QR URL: " + e.getMessage()); })
                )
                .addOnFailureListener(e -> { setBusy(false); toast("QR upload failed: " + e.getMessage()); });
    }

    private void redirectToDetails(String eventId) {
        Intent i = new Intent(this, EventDetailsActivity.class);
        i.putExtra("eventId", eventId);
        startActivity(i);
        finish();
    }

    // ---------- Helpers ----------

    private interface DatePicked { void onPick(String yyyyMmDd); }

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

    // ✅ supports both "yyyy-MM-dd" and "yyyy-MM-dd HH:mm"
    private Timestamp parseDateToTimestamp(String input) {
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

    private Bitmap createQrBitmap(String data, int size) {
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

    private byte[] bitmapToPng(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private void setBusy(boolean busy) {
        btnPublish.setEnabled(!busy);
        btnPickPoster.setEnabled(!busy);
        progress.setVisibility(busy ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
