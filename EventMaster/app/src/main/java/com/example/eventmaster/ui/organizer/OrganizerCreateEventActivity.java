package com.example.eventmaster.ui.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.QRManager;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.QRManagerFs;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class OrganizerCreateEventActivity extends AppCompatActivity {

    private EditText titleEt, descEt, locEt;
    private TextView tvOpen, tvClose;
    private Button publishBtn;

    private EventRepository events;
    private QRManager qr;

    // Picked values (source of truth)
    private Timestamp openTs = null;
    private Timestamp closeTs = null;

    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);

        titleEt = findViewById(R.id.editTitle);
        descEt  = findViewById(R.id.editDescription);
        locEt   = findViewById(R.id.editLocation);
        tvOpen  = findViewById(R.id.tvRegOpen);
        tvClose = findViewById(R.id.tvRegClose);
        publishBtn = findViewById(R.id.btnPublish);

        events = new EventRepositoryFs();
        qr = new QRManagerFs();

        tvOpen.setOnClickListener(v -> pickDateTime(true));
        tvClose.setOnClickListener(v -> pickDateTime(false));
        publishBtn.setOnClickListener(v -> handlePublish());
    }

    /** Show a DatePicker then a TimePicker, set open/close Timestamp + label text. */
    private void pickDateTime(boolean isOpen) {
        final Calendar cal = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

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

        // 1) Create -> 2) Generate+upload QR -> 3) Save qrUrl -> 4) Publish
        events.create(e)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) return Tasks.forException(t.getException());
                    String eventId = t.getResult();
                    String payload = "event://" + eventId; // your deep link if any
                    return qr.generateAndUpload(eventId, payload)
                            .continueWithTask(t2 -> {
                                if (!t2.isSuccessful()) return Tasks.forException(t2.getException());
                                String qrUrl = t2.getResult();
                                HashMap<String, Object> fields = new HashMap<>();
                                fields.put("qrUrl", qrUrl);
                                return events.update(eventId, fields)
                                        .continueWithTask(t3 -> {
                                            if (!t3.isSuccessful()) return Tasks.forException(t3.getException());
                                            return events.publish(eventId);
                                        })
                                        .continueWith(t4 -> eventId);
                            });
                })
                .addOnSuccessListener(eventId -> {
                    toast("Event published!");
                    // TODO: navigate to a detail screen with eventId if desired
                    publishBtn.setEnabled(true);
                })
                .addOnFailureListener(ex -> {
                    toast("Failed: " + ex.getMessage());
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
