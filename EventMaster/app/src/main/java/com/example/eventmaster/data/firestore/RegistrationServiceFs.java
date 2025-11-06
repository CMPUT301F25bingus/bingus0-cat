package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.data.api.RegistrationService;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * - Implements RegistrationService (dev)
 * - Keeps dev methods (createFromInvitation, listFinal, listCancelled, listByEntrant, listenByEntrant)
 * - Adds: enroll, cancel, cancelIfExists, listByStatus (Task-based)
 */
public class RegistrationServiceFs implements RegistrationService {
    private final FirebaseFirestore db;

    // Dev-style DI constructor
    public RegistrationServiceFs(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    // Convenience no-arg ctor (from your version)
    public RegistrationServiceFs() {
        this(FirebaseFirestore.getInstance());
    }

  @Override
    public Task<Void> createFromInvitation(String eventId, String invitationId, String entrantId) {
        DocumentReference regRef = regRef(eventId, entrantId);
        Registration r = new Registration(eventId, entrantId, System.currentTimeMillis());
        // Default to ACTIVE on creation from invitation (align with your semantics if needed)
        r.setStatus(RegistrationStatus.ACTIVE);
        return regRef.set(r, SetOptions.merge());
    }

    @Override
    public Task<List<Registration>> listFinal(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", RegistrationStatus.ACTIVE.name())
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(attachId(d, r));
                    }
                    return list;
                });
    }

    @Override
    public Task<List<Registration>> listCancelled(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList(
                        RegistrationStatus.CANCELLED_BY_ORGANIZER.name(),
                        RegistrationStatus.CANCELLED_BY_ENTRANT.name()))
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(attachId(d, r));
                    }
                    return list;
                });
    }

    @Override
    public Task<List<Registration>> listByEntrant(String entrantId) {
        return db.collectionGroup("registrations")
                .whereEqualTo("entrantId", entrantId)
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(attachId(d, r));
                    }
                    return list;
                });
    }

    @Override
    public ListenerRegistration listenByEntrant(String entrantId,
                                                java.util.function.Consumer<List<Registration>> onData,
                                                java.util.function.Consumer<Throwable> onErr) {
        return db.collectionGroup("registrations")
                .whereEqualTo("entrantId", entrantId)
                .addSnapshotListener((qs, e) -> {
                    if (e != null) { onErr.accept(e); return; }
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(attachId(d, r));
                    }
                    onData.accept(list);
                });
    }


    /** Enroll (final list) → ACTIVE. Returns Task<Registration>. */
    public Task<Registration> enroll(@NonNull String eventId, @NonNull String entrantId) {
        DocumentReference regRef = regRef(eventId, entrantId);
        long now = System.currentTimeMillis();
        return regRef.get().continueWithTask(tSnap -> {
            DocumentSnapshot snap = tSnap.getResult();
            Long existingCreated = (snap != null && snap.exists())
                    ? snap.getLong("createdAtUtc") : null;

            HashMap<String, Object> data = new HashMap<>();
            data.put("eventId", eventId);
            data.put("entrantId", entrantId);
            data.put("status", RegistrationStatus.ACTIVE.name());
            data.put("createdAtUtc", existingCreated != null ? existingCreated : now);
            data.put("cancelledAtUtc", null);

            return regRef.set(data, SetOptions.merge()).continueWith(tSet -> {
                Registration r = new Registration(eventId, entrantId,
                        existingCreated != null ? existingCreated : now);
                r.setId(entrantId);
                r.setStatus(RegistrationStatus.ACTIVE);
                r.setCancelledAtUtc(null);
                return r;
            });
        });
    }

    /** Cancel by organizer or entrant. Returns Task<Void>. */
    public Task<Void> cancel(@NonNull String eventId,
                             @NonNull String entrantId,
                             boolean byOrganizer) {
        DocumentReference ref = regRef(eventId, entrantId);
        long now = System.currentTimeMillis();
        return ref.get().continueWithTask(tSnap -> {
            DocumentSnapshot snap = tSnap.getResult();
            Long created = (snap != null && snap.exists())
                    ? snap.getLong("createdAtUtc") : now;

            HashMap<String, Object> data = new HashMap<>();
            data.put("eventId", eventId);
            data.put("entrantId", entrantId);
            data.put("status", (byOrganizer
                    ? RegistrationStatus.CANCELLED_BY_ORGANIZER
                    : RegistrationStatus.CANCELLED_BY_ENTRANT).name());
            data.put("createdAtUtc", created);
            data.put("cancelledAtUtc", now);

            return ref.set(data, SetOptions.merge());
        });
    }

    /** Cancel only if exists. Returns Task<Void>. */
    public Task<Void> cancelIfExists(@NonNull String eventId, @NonNull String entrantId) {
        DocumentReference ref = regRef(eventId, entrantId);
        return ref.get().continueWithTask(tSnap -> {
            DocumentSnapshot d = tSnap.getResult();
            if (d != null && d.exists()) {
                return cancel(eventId, entrantId, false);
            }
            return ref.set(new HashMap<>(), SetOptions.merge()); // no-op write; resolves as success
        });
    }

    /** Generic list by exact status string (e.g., "ACTIVE"). */
    public Task<List<Registration>> listByStatus(@NonNull String eventId, @NonNull String status) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", status)
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (QueryDocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(attachId(d, r));
                    }
                    return list;
                });
    }

    // Helpers
    private DocumentReference regRef(String eventId, String entrantId) {
        return db.collection("events").document(eventId)
                .collection("registrations").document(entrantId);
    }

    private static Registration attachId(DocumentSnapshot d, Registration r) {
        // convenience: store doc id if you rely on it elsewhere
        if (r != null) r.setId(d.getId());
        return r;
    }
}
