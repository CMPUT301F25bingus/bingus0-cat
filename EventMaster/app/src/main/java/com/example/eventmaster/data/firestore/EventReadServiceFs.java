package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventReadServiceFs implements EventReadService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public Task<Event> get(String eventId) {
        return db.collection("events").document(eventId)
                .get()
                .continueWith(t -> {
                    Event e = t.getResult().toObject(Event.class);
                    if (e != null) e.setId(eventId);
                    return e;
                });
    }
}
