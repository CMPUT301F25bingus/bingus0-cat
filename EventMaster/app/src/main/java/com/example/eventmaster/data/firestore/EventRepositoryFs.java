package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EventRepositoryFs implements EventRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Map<String, Object> toMap(@NonNull Event e) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", e.getTitle());
        m.put("description", e.getDescription());
        m.put("location", e.getLocation());
        m.put("registrationOpen", e.getRegistrationOpen());
        m.put("registrationClose", e.getRegistrationClose());
        m.put("posterUrl", e.getPosterUrl());
        m.put("qrUrl", e.getQrUrl());
        m.put("status", e.getStatus());
        m.put("updatedAt", Timestamp.now());
        return m;
    }

    @Override
    public Task<String> create(Event e) {
        String err = e.validate();
        if (err != null) return Tasks.forException(new IllegalArgumentException(err));

        Map<String, Object> data = toMap(e);
        data.put("createdAt", Timestamp.now());

        DocumentReference ref = db.collection("events").document();
        return ref.set(data).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getId();
        });
    }

    @Override
    public Task<Void> update(String eventId, Map<String, Object> fields) {
        fields.put("updatedAt", Timestamp.now());
        return db.collection("events").document(eventId).update(fields);
    }

    @Override
    public Task<Void> publish(String eventId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", "PUBLISHED");
        fields.put("publishedAt", Timestamp.now());
        return update(eventId, fields);
    }
}