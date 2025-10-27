package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> {
                    Log.d("Auth", "Anon OK uid=" + r.getUser().getUid());
                    Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("Auth", "Anon FAILED", e);
                    Toast.makeText(this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("ping").document("test").set(
                        new java.util.HashMap<String, Object>() {{ put("ts", System.currentTimeMillis()); }}
                ).addOnSuccessListener(v -> Log.d("Ping", "OK"))
                .addOnFailureListener(e -> Log.e("Ping", "FAILED", e));

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep your existing inset handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1) Sign in anonymously so Firebase rules allow writes
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(result -> Log.d(TAG, "Anon sign-in OK: " + result.getUser().getUid()))
                .addOnFailureListener(e -> Log.e(TAG, "Anon sign-in FAILED", e));

        // 2) Navigate to OrganizerCreateEventActivity
        Button btnGo = findViewById(R.id.btnGoOrganizer);
        if (btnGo != null) {
            btnGo.setOnClickListener(v ->
                    startActivity(new Intent(this, OrganizerCreateEventActivity.class))
            );
        } else {
            Log.w(TAG, "btnGoOrganizer not found in activity_main.xml (add a button to navigate).");
        }
    }
}
