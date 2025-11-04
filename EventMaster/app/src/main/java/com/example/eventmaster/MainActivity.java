package com.example.eventmaster;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.entrant.event.EventDetailsFragment;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Option A: pass a fixed entrant id (no FirebaseAuth)
//            String entrantId = "demo-user"; // change to any test id you want
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.main_container, InvitationInboxFragment.newInstance("test-entrant-42"))
//                    .commit();
//            //for organizer:
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container,
                            com.example.eventmaster.ui.organizer.enrollments.OrganizerEntrantsHubFragment
                                    .newInstance("event-123")) // <-- use a real eventId that has registrations
                    .commit();
//            String eventId = "test_event_1";
//            String userId = "testUser1";
//
//
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.main_container, EventDetailsFragment.newInstance(eventId, userId))
//                    .commit();


        }
    }
}