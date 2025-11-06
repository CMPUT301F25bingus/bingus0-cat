package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EventReadServiceFs implements EventReadService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public Task<Event> get(String eventId) {
        return db.collection("events").document(eventId).get()
                .continueWith(t -> {
                    DocumentSnapshot d = t.getResult();
                    if (d == null || !d.exists()) return null;
                    Event e = d.toObject(Event.class);
                    if (e != null) e.setId(d.getId());
                    return e;
                });
    }

    @Override
    public Task<List<Event>> listByOrganizer(String organizerId) {
        return db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .continueWith(t -> {
                    List<Event> out = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Event e = d.toObject(Event.class);
                        if (e != null) { e.setId(d.getId()); out.add(e); }
                    }
                    return out;
                });
    }
}
