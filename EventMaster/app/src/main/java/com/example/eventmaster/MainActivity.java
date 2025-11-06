//package com.example.eventmaster;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity;
//import com.example.eventmaster.ui.organizer.OrganizerHomeActivity;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//public class MainActivity extends AppCompatActivity {
//
//    private static final String TAG = "MainActivity";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_organizer);
//
//        // ✅ Anonymous Firebase sign-in
//        FirebaseAuth.getInstance().signInAnonymously()
//                .addOnSuccessListener(result -> {
//                    Log.d(TAG, "Anon sign-in OK: " + result.getUser().getUid());
//                    Toast.makeText(this, "Signed in", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Anon sign-in FAILED", e);
//                    Toast.makeText(this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                });
//
//        // ✅ Firestore test ping
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        db.collection("ping").document("test").set(
//                        new java.util.HashMap<String, Object>() {{
//                            put("ts", System.currentTimeMillis());
//                        }}
//                ).addOnSuccessListener(v -> Log.d(TAG, "Ping OK"))
//                .addOnFailureListener(e -> Log.e(TAG, "Ping FAILED", e));
//
//        // ✅ Handle window insets
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // ✅ Buttons
//        Button btnCreate = findViewById(R.id.btnGoOrganizer);
//        Button btnHome = findViewById(R.id.btnGoOrganizerHome);
//
//        if (btnCreate != null) {
//            btnCreate.setOnClickListener(v ->
//                    startActivity(new Intent(this, OrganizerCreateEventActivity.class))
//            );
//        }
//
//        if (btnHome != null) {
//            btnHome.setOnClickListener(v ->
//                    startActivity(new Intent(this, OrganizerHomeActivity.class))
//            );
//        }
//    }
//}