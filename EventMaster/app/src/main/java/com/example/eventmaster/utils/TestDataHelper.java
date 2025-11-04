package com.example.eventmaster.utils;

import android.util.Log;

import com.example.eventmaster.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

/**
 * Helper class to populate Firestore with test data for development and testing.
 * IMPORTANT: This should only be used during development.
 */
public class TestDataHelper {

    private static final String TAG = "TestDataHelper";
    private final FirebaseFirestore db;

    public TestDataHelper() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Creates sample events in Firestore for testing.
     */
    public void createSampleEvents() {
        createSwimmingLessonsEvent();
        createPianoLessonsEvent();
        createDanceClassEvent();
    }

    private void createSwimmingLessonsEvent() {
        Calendar cal = Calendar.getInstance();

        // Event happens in 2 weeks
        cal.add(Calendar.DAY_OF_MONTH, 14);
        Date eventDate = cal.getTime();

        // Registration started yesterday
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date regStart = cal.getTime();

        // Registration ends in 2 days
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date regEnd = cal.getTime();

        Event event = new Event(
                "test_event_1",
                "Swimming Lessons for Beginners",
                "Learn the basics of swimming in a safe and fun environment. " +
                        "Perfect for adults who want to learn how to swim!",
                "Local Recreation Centre Pool",
                eventDate,
                regStart,
                regEnd,
                "org_001",
                "City Recreation Centre",
                20,
                60.00
        );

        db.collection("events")
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Swimming Lessons event created"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to create Swimming Lessons event", e));
    }

    private void createPianoLessonsEvent() {
        Calendar cal = Calendar.getInstance();

        // Event happens in 1 month
        cal.add(Calendar.MONTH, 1);
        Date eventDate = cal.getTime();

        // Registration opens in 2 days
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date regStart = cal.getTime();

        // Registration ends in 7 days
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        Date regEnd = cal.getTime();

        Event event = new Event(
                "test_event_2",
                "Piano Lessons - Beginner to Intermediate",
                "Join our comprehensive piano course designed for beginners. " +
                        "Learn music theory, proper technique, and play your favorite songs!",
                "Community Arts Centre",
                eventDate,
                regStart,
                regEnd,
                "org_002",
                "Arts & Music Academy",
                15,
                75.00
        );

        db.collection("events")
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Piano Lessons event created"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to create Piano Lessons event", e));
    }

    private void createDanceClassEvent() {
        Calendar cal = Calendar.getInstance();

        // Event happens in 10 days
        cal.add(Calendar.DAY_OF_MONTH, 10);
        Date eventDate = cal.getTime();

        // Registration started 3 days ago
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -3);
        Date regStart = cal.getTime();

        // Registration ends in 5 days
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 5);
        Date regEnd = cal.getTime();

        Event event = new Event(
                "test_event_3",
                "Interpretive Dance - Safety Basics",
                "Learn the fundamental safety principles of interpretive dance. " +
                        "We'll cover proper warm-ups, injury prevention, and respectful partner work.",
                "Downtown Dance Studio",
                eventDate,
                regStart,
                regEnd,
                "org_003",
                "Dance Masters Inc.",
                60,
                45.00
        );

        event.setWaitingListLimit(100); // Optional: Set a limit

        db.collection("events")
                .document(event.getEventId())
                .set(event)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Dance Class event created"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to create Dance Class event", e));
    }
}


