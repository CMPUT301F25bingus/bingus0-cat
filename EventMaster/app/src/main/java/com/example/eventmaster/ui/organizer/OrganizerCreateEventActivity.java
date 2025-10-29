package com.example.eventmaster.ui.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

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
    private ProgressBar progress; // optional spinner in layout

    // Repo
    private EventRepository events;

    // Date/time state
    private Timestamp openTs = null;
    private Timestamp closeTs = null;
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    // Poster selection (local preview only for now)
    private Uri selectedPosterUri = null;
    private ActivityResultLauncher<String> pickPosterLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);

        MaterialToolbar bar = findViewById(R.id.topBar);
        bar.setNavigationOnClickListener(v -> onBackPressed());

        // Bind views
        titleEt       = findViewById(R.id.editTitle);
        descEt        = findViewById(R.id.editDescription);
        locEt         = findViewById(R.id.editLocation);
        tvOpen        = findViewById(R.id.tvRegOpen);
        tvClose       = findViewById(R.id.tvRegClose);
        publishBtn    = findViewById(R.id.btnPublish);
        btnPickPoster = findViewById(R.id.btnPickPoster);
        posterPreview = findViewById(R.id.imgPosterPreview);
        progress      = findViewById(R.id.progress); // may be null if not in XML

        // Repo
        events = new EventRepositoryFs();

        // Date/time pickers
        tvOpen.setOnClickListener(v -> { pickDateTime(true); updatePublishEnabled(); });
        tvClose.setOnClickListener(v -> { pickDateTime(false); updatePublishEnabled(); });

        // Enable/disable Publish based on validity
        TextWatcher w = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { updatePublishEnabled(); }
            public void afterTextChanged(Editable s) {}
        };
        titleEt.addTextChangedListener(w);
        locEt.addTextChangedListener(w);
        updatePublishEnabled();

        // Image picker (local preview only; no Storage yet)
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

        // Try to sign in (non-blocking)
        trySignInSilently();

        // Publish (no auth gating; works with open Firestore rules)
        publishBtn.setOnClickListener(v -> doPublish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        trySignInSilently();   // best-effort on every show
        updatePublishEnabled();
    }

    /** Attempt anonymous auth, but don't block UI if it fails. */
    private void trySignInSilently() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) return;
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> android.util.Log.d("Auth", "Anon OK uid=" + r.getUser().getUid()))
                .addOnFailureListener(e -> android.util.Log.w("Auth", "Anon FAILED: " + e.getMessage()));
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
                                updatePublishEnabled();
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

    /** DEV MODE (no Storage): create -> update placeholders -> publish -> details */
    private void doPublish() {
        publishBtn.setEnabled(false);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        android.util.Log.d("CreateFlow", "Publish clicked (dev no-uploads)");

        String title = safeText(titleEt);
        String desc  = safeText(descEt);
        String loc   = safeText(locEt);

        Event e = new Event(title, desc, loc, openTs, closeTs);
        String error = e.validate();
        if (error != null) {
            toast(error);
            publishBtn.setEnabled(true);
            if (progress != null) progress.setVisibility(View.GONE);
            android.util.Log.w("CreateFlow", "Validation failed: " + error);
            return;
        }

        android.util.Log.d("CreateFlow", "Creating event…");
        events.create(e)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) return Tasks.forException(t.getException());
                    String eventId = t.getResult();
                    android.util.Log.d("CreateFlow", "Created eventId=" + eventId);

                    // No uploads yet; save placeholders so UI downstream doesn't break
                    HashMap<String, Object> fields = new HashMap<>();
                    if (selectedPosterUri != null) fields.put("posterUrl", "poster-skipped-dev");
                    fields.put("qrUrl", "qr-skipped-dev");

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
                    if (progress != null) progress.setVisibility(View.GONE);

                    startActivity(new Intent(this, EventDetailsActivity.class)
                            .putExtra(EventDetailsActivity.EXTRA_EVENT_ID, eventId));
                })
                .addOnFailureListener(ex -> {
                    String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error";
                    toast("Failed: " + msg);
                    publishBtn.setEnabled(true);
                    if (progress != null) progress.setVisibility(View.GONE);
                    android.util.Log.e("CreateFlow", "Flow failed", ex);
                });

        // Safety: never leave the button stuck
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (!publishBtn.isEnabled()) {
                publishBtn.setEnabled(true);
                if (progress != null) progress.setVisibility(View.GONE);
                toast("Taking longer than expected. Check Firestore setup.");
                android.util.Log.w("CreateFlow", "Watchdog re-enabled button.");
            }
        }, 15000);
    }

    private void updatePublishEnabled() {
        boolean hasTitle = !safeText(titleEt).isEmpty();
        boolean hasLoc   = !safeText(locEt).isEmpty();
        boolean timesOk  = openTs != null && closeTs != null && !openTs.toDate().after(closeTs.toDate());
        publishBtn.setEnabled(hasTitle && hasLoc && timesOk);
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
