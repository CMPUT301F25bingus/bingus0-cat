package com.example.eventmaster.ui.organizer;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "eventId";
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private TextView tTitle, tDesc, tLoc, tReg;
    private ImageView imgPoster, imgQr;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        String id = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (id == null) {
            Toast.makeText(this, "Missing eventId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tTitle = findViewById(R.id.tvTitle);
        tDesc = findViewById(R.id.tvDesc);
        tLoc = findViewById(R.id.tvLoc);
        tReg = findViewById(R.id.tvReg);
        imgPoster = findViewById(R.id.imgPoster);
        imgQr = findViewById(R.id.imgQr);

        loadEventDetails(id);
    }

    private void loadEventDetails(String id) {
        FirebaseFirestore.getInstance().collection("events").document(id).get()
                .addOnSuccessListener(this::bindEvent)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void bindEvent(DocumentSnapshot d) {
        if (!d.exists()) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tTitle.setText(d.getString("title"));
        tDesc.setText(d.getString("description"));
        tLoc.setText(d.getString("location"));

        var open = d.getTimestamp("regStart");
        var close = d.getTimestamp("regEnd");
        String range = (open != null ? fmt.format(open.toDate()) : "?") + " → " +
                (close != null ? fmt.format(close.toDate()) : "?");
        tReg.setText(range);

        String posterUrl = d.getString("posterUrl");
        String qrUrl = d.getString("qrUrl");

        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this).load(posterUrl).into(imgPoster);
        }

        if (qrUrl != null && !qrUrl.isEmpty()) {
            Glide.with(this).load(qrUrl).into(imgQr);
        }
    }
}
