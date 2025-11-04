//package com.example.eventmaster.ui.entrant;
//
//import android.os.Bundle;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.FragmentTransaction;
//
//import com.example.eventmaster.R;
//import com.example.eventmaster.ui.entrant.event.EventDetailsFragment;
//
///**
// * Activity that hosts the EventDetailsFragment.
// * Used to display event details and allow joining waiting list.
// */
//public class EventDetailsActivity extends AppCompatActivity {
//
//    public static final String EXTRA_EVENT_ID = "extra_event_id";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_event_details);
//
//        // Get event ID from intent
//        String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
//
//        if (eventId == null) {
//            // For testing, use a default event ID
//            eventId = "test_event_1";
//        }
//
//        // Load fragment
//        if (savedInstanceState == null) {
//            EventDetailsFragment fragment = EventDetailsFragment.newInstance(eventId);
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            transaction.replace(R.id.fragment_container, fragment);
//            transaction.commit();
//        }
//    }
//}
