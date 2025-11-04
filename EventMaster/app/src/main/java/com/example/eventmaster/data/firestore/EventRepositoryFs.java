package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.model.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore implementation of EventRepository.
 * Handles Firestore operations related to Events.
 */
public class EventRepositoryFs implements EventRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "events";

    public EventRepositoryFs() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getAllEvents(OnEventListListener listener) {
        db.collection(COLLECTION_NAME)
                .get()
                .addOnSuccessListener(query -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Event event = doc.toObject(Event.class);
                        event.setEventId(doc.getId()); // Ensure ID is set
                        events.add(event);
                    }
                    listener.onSuccess(events);
                })
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void getEventById(String eventId, OnEventListener listener) {
        db.collection(COLLECTION_NAME)
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event != null) {
                            event.setEventId(documentSnapshot.getId());
                            listener.onSuccess(event);
                        } else {
                            listener.onFailure(new Exception("Failed to parse event data"));
                        }
                    } else {
                        listener.onFailure(new Exception("Event not found"));
                    }
                })
                .addOnFailureListener(listener::onFailure);
    }
}