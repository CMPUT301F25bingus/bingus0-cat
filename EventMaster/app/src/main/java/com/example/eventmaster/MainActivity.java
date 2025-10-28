package com.example.eventmaster;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.entrant.invitations.InvitationInboxFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Option A: pass a fixed entrant id (no FirebaseAuth)
            String entrantId = "demo-user"; // change to any test id you want
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, InvitationInboxFragment.newInstance("test-entrant-42"))
                    .commit();
        }
    }
}