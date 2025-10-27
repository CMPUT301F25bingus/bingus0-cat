package com.example.eventmaster.ui.organizer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.Calendar;
import java.util.HashMap;

public class OrganizerCreateEventActivity extends AppCompatActivity {

    private EditText titleEt, descEt, locEt, openEt, closeEt;
    private Button publishBtn;

    private EventRepository events;
    private QRManager qr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_create_event);

        // Inputs (make sure your XML uses these IDs)
        titleEt = findViewById(R.id.editTitle);
        descEt  = findViewById(R.id.editDescription);
        locEt   = findViewById(R.id.editLocation);
        openEt  = findViewById(R.id.editRegOpen);   // "YYYY-MM-DD HH:MM"
        closeEt = findViewById(R.id.editRegClose);  // "YYYY-MM-DD HH:MM"

        // Button
        publishBtn = findViewById(R.id.btnPublish);

        // Repos
        events = new EventRepositoryFs();
        qr = new QRManagerFs();

        // Click handler
        publishBtn.setOnClickListener(v -> handlePublish());
    }

    private void handlePublish() {
        publishBtn.setEnabled(false);

        String title = safeText(titleEt);
        String desc  = safeText(descEt);
        String loc   = safeText(locEt);

        Timestamp openTs  = parseTimestamp(safeText(openEt));
        Timestamp closeTs = parseTimestamp(safeText(closeEt));

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
                    String payload = "event://" + eventId; // replace with your deep link if you have one
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
                    // TODO: navigate to a detail/success screen with eventId if desired
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

    /** Simple parser for "YYYY-MM-DD HH:MM"; returns null if invalid (caught by validate()). */
    private @Nullable Timestamp parseTimestamp(String text) {
        try {
            String[] parts = text.split(" ");
            String[] d = parts[0].split("-");
            String[] t = parts.length > 1 ? parts[1].split(":") : new String[]{"0","0"};
            Calendar c = Calendar.getInstance();
            c.set(
                    Integer.parseInt(d[0]),
                    Integer.parseInt(d[1]) - 1,
                    Integer.parseInt(d[2]),
                    Integer.parseInt(t[0]),
                    Integer.parseInt(t[1]),
                    0
            );
            return new Timestamp(new java.util.Date(c.getTimeInMillis()));
        } catch (Exception e) {
            return null;
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
