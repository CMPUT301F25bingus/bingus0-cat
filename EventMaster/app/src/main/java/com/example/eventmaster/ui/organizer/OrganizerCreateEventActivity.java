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
import com.example.eventmaster.data.firestore.EventRepositoryFs;
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

    // Date/time state
    private Timestamp openTs = null;
    private Timestamp closeTs = null;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Poster selection (local preview only for now)
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

        // Repo
        events  = new EventRepositoryFs();

        // Date/time pickers
        tvOpen.setOnClickListener(v -> pickDateTime(true));
        tvClose.setOnClickListener(v -> pickDateTime(false));

        // Image picker: SAF "GetContent" (simple, stable). Local preview only (no upload yet).
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

    /** DEV MODE (no Storage/Auth): create → update placeholders → publish */
    private void handlePublish() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            toast("Not signed in yet. Try again in a second.");
            publishBtn.setEnabled(true);
            android.util.Log.w("CreateFlow", "Blocked: no currentUser");
            return;
        }
        publishBtn.setEnabled(false);
        android.util.Log.d("CreateFlow", "Publish clicked (dev no-uploads)");

        String title = safeText(titleEt);
        String desc  = safeText(descEt);
        String loc   = safeText(locEt);

        Event e = new Event(title, desc, loc, openTs, closeTs);
        String error = e.validate();
        if (error != null) {
            toast(error);
            publishBtn.setEnabled(true);
            android.util.Log.w("CreateFlow", "Validation failed: " + error);
            return;
        }

        android.util.Log.d("CreateFlow", "Creating event…");
        events.create(e)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) return Tasks.forException(t.getException());
                    String eventId = t.getResult();
                    android.util.Log.d("CreateFlow", "Created eventId=" + eventId);

                    // No uploads. Save placeholders so downstream UI doesn't break.
                    HashMap<String, Object> fields = new HashMap<>();
                    if (selectedPosterUri != null) {
                        fields.put("posterUrl", "poster-skipped-dev"); // placeholder
                    }
                    fields.put("qrUrl", "qr-skipped-dev"); // placeholder

                    android.util.Log.d("CreateFlow", "Updating event with placeholder URLs…");
                    return events.update(eventId, fields)
                            .continueWithTask(tu -> {
                                if (!tu.isSuccessful()) return Tasks.forException(tu.getException());
                                android.util.Log.d("CreateFlow", "Publishing event…");
                                return events.publish(eventId);
                            })
                            .continueWith(tDone -> eventId);
                })
                .addOnSuccessListener(eventId -> {
                    android.util.Log.d("CreateFlow", "Done. Event published: " + eventId);
                    toast("Event published (no uploads).");
                    publishBtn.setEnabled(true);
                })
                .addOnFailureListener(ex -> {
                    String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    toast("Failed: " + msg);
                    publishBtn.setEnabled(true);
                    android.util.Log.e("CreateFlow", "Flow failed", ex);
                });

        // Watchdog so the button doesn't stay stuck if something hangs
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (!publishBtn.isEnabled()) {
                publishBtn.setEnabled(true);
                toast("Taking longer than expected. Check Firestore setup.");
                android.util.Log.w("CreateFlow", "Watchdog re-enabled button (possible Firestore issue).");
            }
        }, 15000);
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
