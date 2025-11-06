package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firestore implementation of EventRepository that supports:
 *  - Task-based create/update/publish/getAll/getById
 *  - Callback-based getAll/getById (legacy/adapter-friendly)
 *
 * Storage notes:
 *  - Writes both "name" and "title" for backwards compatibility.
 *  - Registration window stored as "registrationOpen"/"registrationClose".
 *  - If present, "eventDate" is stored as a Timestamp.
 *  - Doc ID is not stored; we set it on the model after reads.
 */
public class EventRepositoryFs implements EventRepository {

    private static final String COLL = "events";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ----------- Mapping helpers -----------

    private Map<String, Object> toMap(@NonNull Event e, boolean includeAuditCreate) {
        Map<String, Object> m = new HashMap<>();

        // Canonical + compatibility keys
        // Prefer the model's getName(); mirror to "title" so old code keeps working.
        // If your model only has getTitle(), ensure Event exposes getName() or adapt here.
        String name = e.getName();
        if (name == null && e.getTitle() != null) {
            name = e.getTitle();
        }
        m.put("name", name);
        m.put("title", name); // keep old code working if it queried "title"

        // Core fields
        putIfNotNull(m, "description", e.getDescription());
        putIfNotNull(m, "location", e.getLocation());

        // Optional event date as Timestamp
        putIfNotNull(m, "eventDate", e.getEventDateTimestamp());

        // Registration window
        putIfNotNull(m, "registrationOpen", e.getRegistrationOpen());
        putIfNotNull(m, "registrationClose", e.getRegistrationClose());

        // Media
        putIfNotNull(m, "posterUrl", e.getPosterUrl());
        putIfNotNull(m, "qrUrl", e.getQrUrl());

        // Organizer
        putIfNotNull(m, "organizerId", e.getOrganizerId());
        putIfNotNull(m, "organizerName", e.getOrganizerName());

        // Constraints / options
        m.put("capacity", e.getCapacity());
        if (e.getWaitingListLimit() != null) m.put("waitingListLimit", e.getWaitingListLimit());
        m.put("geolocationRequired", e.isGeolocationRequired());
        m.put("price", e.getPrice());

        // Lifecycle
        putIfNotNull(m, "status", e.getStatus());

        // Audit
        m.put("updatedAt", Timestamp.now());
        if (includeAuditCreate) {
            m.put("createdAt", Timestamp.now());
        }

        return m;
    }

    private static void putIfNotNull(Map<String, Object> m, String key, @Nullable Object val) {
        if (val != null) m.put(key, val);
    }

    private Event fromSnapshot(@NonNull DocumentSnapshot doc) {
        Event e = doc.toObject(Event.class);
        if (e == null) e = new Event();
        // Ensure ID is set (both aliases supported by model)
        e.setId(doc.getId());
        return e;
    }

    // ----------- Task-based writes -----------

    @Override
    public Task<String> create(@NonNull Event e) {
        String err = e.validate();
        if (err != null) return Tasks.forException(new IllegalArgumentException(err));

        Map<String, Object> data = toMap(e, /*includeAuditCreate*/ true);
        DocumentReference ref = db.collection(COLL).document();
        return ref.set(data).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return ref.getId();
        });
    }

    @Override
    public Task<Void> update(@NonNull String eventId, @NonNull Map<String, Object> fields) {
        // Always update audit
        fields.put("updatedAt", Timestamp.now());

        // Mirror "name" to "title" if provided, to preserve older clients
        if (fields.containsKey("name") && fields.get("name") instanceof String) {
            fields.put("title", fields.get("name"));
        }

        return db.collection(COLL).document(eventId).update(fields);
    }

    @Override
    public Task<Void> publish(@NonNull String eventId) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", "PUBLISHED");
        fields.put("publishedAt", Timestamp.now());
        return update(eventId, fields);
    }

    // ----------- Task-based reads -----------

    @Override
    public Task<List<Event>> getAllEvents() {
        return db.collection(COLL)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    List<Event> out = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        out.add(fromSnapshot(doc));
                    }
                    return out;
                });
    }

    @Override
    public Task<Event> getEventById(@NonNull String eventId) {
        return db.collection(COLL)
                .document(eventId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentSnapshot doc = task.getResult();
                    if (!doc.exists()) throw new IllegalStateException("Event not found: " + eventId);
                    return fromSnapshot(doc);
                });
    }

    // ----------- Callback-based reads (delegate to Task versions) -----------

    @Override
    public void getAllEvents(OnEventListListener listener) {
        getAllEvents()
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void getEventById(String eventId, OnEventListener listener) {
        getEventById(eventId)
                .addOnSuccessListener(listener::onSuccess)
                .addOnFailureListener(listener::onFailure);
    }


    /** Lists events created by the currently signed-in organizer. */
    public void listByOrganizer(@NonNull Consumer<List<Map<String,Object>>> onSuccess,
                                @NonNull Consumer<Throwable> onError) {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            onError.accept(new IllegalStateException("Not signed in"));
            return;
        }

        db.collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String,Object>> out = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        Map<String,Object> m = d.getData();
                        // Ensure the adapter can read an id:
                        m.put("eventId", d.getString("eventId") != null ? d.getString("eventId") : d.getId());
                        out.add(m);
                    }
                    onSuccess.accept(out);
                })
                .addOnFailureListener(onError::accept);
    }
}
