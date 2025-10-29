package com.example.eventmaster.ui.organizer;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eventmaster.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_EVENT_ID = "eventId";
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        String id = getIntent().getStringExtra(EXTRA_EVENT_ID);
        if (id == null) { Toast.makeText(this,"Missing eventId",Toast.LENGTH_LONG).show(); finish(); return; }

        TextView tTitle = findViewById(R.id.tvTitle);
        TextView tDesc  = findViewById(R.id.tvDesc);
        TextView tLoc   = findViewById(R.id.tvLoc);
        TextView tReg   = findViewById(R.id.tvReg);
        TextView tQR    = findViewById(R.id.tvQrUrl);
        TextView tPoster= findViewById(R.id.tvPosterUrl);
        TextView tStat  = findViewById(R.id.tvStatus);

        FirebaseFirestore.getInstance().collection("events").document(id).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) { Toast.makeText(this,"Not found",Toast.LENGTH_LONG).show(); finish(); return; }
                    tTitle.setText(d.getString("title"));
                    tDesc.setText(d.getString("description"));
                    tLoc.setText(d.getString("location"));
                    var open = d.getTimestamp("registrationOpen");
                    var close= d.getTimestamp("registrationClose");
                    tReg.setText((open!=null?fmt.format(open.toDate()):"?") + "  →  " + (close!=null?fmt.format(close.toDate()):"?"));
                    tQR.setText(String.valueOf(d.get("qrUrl")));
                    tPoster.setText(String.valueOf(d.get("posterUrl")));
                    tStat.setText(String.valueOf(d.get("status")));
                })
                .addOnFailureListener(e -> { Toast.makeText(this,"Load failed: "+e.getMessage(),Toast.LENGTH_LONG).show(); finish(); });

    }
}
