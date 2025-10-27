package com.example.eventmaster.ui.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.PosterRepository;
import com.example.eventmaster.data.api.QRManager;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.PosterRepositoryFs;
import com.example.eventmaster.data.firestore.QRManagerFs;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class OrganizerCreateEventActivity extends AppCompatActivity {

    // Inputs
    private EditText titleEt, descEt, locEt;
    private TextView tvOpen, tvClose;
    private Button publishBtn, btnPickPoster;
    private ImageView posterPreview;

    // Repos
    private EventRepository events;
    private QRManager qr;
    private PosterRepository posters;

    // Date/time state
    private Timestamp openTs = null;
    private Timestamp closeTs = null;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Poster selection
    private Uri selectedPosterUri = null;
    private ActivityResultLauncher<String> pickPosterLauncher; // GetContent => input is MIME type

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);

        // Bind views
        titleEt       = findViewById(R.id.editTitle);
        descEt        = findViewById(R.id.editDescription);
        locEt         = findViewById(R.id.editLocation);
        tvOpen        = findViewById(R.id.tvRegOpen);
        tvClose       = findViewById(R.id.tvRegClose);
        publishBtn    = findViewById(R.id.btnPublish);
        btnPickPoster = findViewById(R.id.btnPickPoster);
        posterPreview = findViewById(R.id.imgPosterPreview);

        // Repos
        events  = new EventRepositoryFs();
        qr      = new QRManagerFs();
        posters = new PosterRepositoryFs();

        // Date/time pickers
        tvOpen.setOnClickListener(v -> pickDateTime(true));
        tvClose.setOnClickListener(v -> pickDateTime(false));

        // Image picker: SAF "GetContent" (simple, stable)
        pickPosterLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPosterUri = uri;
                        posterPreview.setImageURI(uri);
                    }
                }
        );
        btnPickPoster.setOnClickListener(v -> pickPosterLauncher.launch("image/*"));

        // Publish
        publishBtn.setOnClickListener(v -> handlePublish());
    }

    private void pickDateTime(boolean isOpen) {
        final Calendar cal = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);

                    new TimePickerDialog(
                            this,
                            (tp, hour, minute) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hour);
                                cal.set(Calendar.MINUTE, minute);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);

                                Timestamp ts = new Timestamp(cal.getTime());
                                if (isOpen) {
                                    openTs = ts;
                                    tvOpen.setText("Reg Open: " + fmt.format(cal.getTime()));
                                } else {
                                    closeTs = ts;
                                    tvClose.setText("Reg Close: " + fmt.format(cal.getTime()));
                                }
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                    ).show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void handlePublish() {
        publishBtn.setEnabled(false);

        String title = safeText(titleEt);
        String desc  = safeText(descEt);
        String loc   = safeText(locEt);

        Event e = new Event(title, desc, loc, openTs, closeTs);
        String error = e.validate();
        if (error != null) {
            toast(error);
            publishBtn.setEnabled(true);
            return;
        }

        // 1) Create -> 2) (optional) poster upload -> 3) QR upload -> 4) update -> 5) publish
        events.create(e)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) return Tasks.forException(t.getException());
                    String eventId = t.getResult();

                    var posterTask = (selectedPosterUri != null)
                            ? posters.upload(eventId, selectedPosterUri)
                            : Tasks.forResult(null);

                    return posterTask
                            .continueWithTask(tp -> {
                                String posterUrl = (tp != null && tp.isSuccessful()) ? (String) tp.getResult() : null;

                                String payload = "event://" + eventId; // or your deep link
                                return qr.generateAndUpload(eventId, payload)
                                        .continueWithTask(tqr -> {
                                            if (!tqr.isSuccessful()) return Tasks.forException(tqr.getException());
                                            String qrUrl = tqr.getResult();

                                            HashMap<String, Object> fields = new HashMap<>();
                                            if (posterUrl != null) fields.put("posterUrl", posterUrl);
                                            fields.put("qrUrl", qrUrl);

                                            return events.update(eventId, fields)
                                                    .continueWithTask(tu -> {
                                                        if (!tu.isSuccessful()) return Tasks.forException(tu.getException());
                                                        return events.publish(eventId);
                                                    })
                                                    .continueWith(tDone -> eventId);
                                        });
                            });
                })
                .addOnSuccessListener(eventId -> {
                    toast("Event published!");
                    publishBtn.setEnabled(true);
                })
                .addOnFailureListener(ex -> {
                    toast("Failed: " + (ex.getMessage() == null ? "Unknown error" : ex.getMessage()));
                    publishBtn.setEnabled(true);
                });
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
