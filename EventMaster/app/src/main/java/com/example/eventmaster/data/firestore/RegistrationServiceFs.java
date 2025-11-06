package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firestore-backed Registration service.
 * Writes epoch-millis timestamps and enum statuses to match Registration.java.
 *
 * Firestore document path:
 *   events/{eventId}/registrations/{entrantId}
 *
 * Fields written:
 *   eventId: string
 *   entrantId: string
 *   status: string  (enum name: ACTIVE, CANCELLED_BY_ORGANIZER, CANCELLED_BY_ENTRANT)
 *   createdAtUtc: long (epoch millis)
 *   cancelledAtUtc: long? (epoch millis, nullable)
 */
public class RegistrationServiceFs {

    private final FirebaseFirestore db;

    public RegistrationServiceFs() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ---------------------------------------------------------------------
    // Mutations
    // ---------------------------------------------------------------------

    /**
     * Enroll a user in an event (final list) — sets status ACTIVE.
     * If a registration doc exists, we keep its createdAtUtc; otherwise we set it now.
     */
    public void enroll(@NonNull String eventId,
                       @NonNull String entrantId,
                       @NonNull Consumer<Registration> onSuccess,
                       @NonNull Consumer<Throwable> onError) {

        DocumentReference regRef = regRef(eventId, entrantId);
        regRef.get().addOnSuccessListener(snap -> {
            long now = System.currentTimeMillis();

            Long existingCreated = (snap.exists())
                    ? getLongSafe(snap, "createdAtUtc")
                    : null;

            Map<String, Object> data = new HashMap<>();
            data.put("eventId", eventId);
            data.put("entrantId", entrantId);
            data.put("status", RegistrationStatus.ACTIVE.name());
            data.put("createdAtUtc", existingCreated != null ? existingCreated : now);
            data.put("cancelledAtUtc", null);

            regRef.set(data, SetOptions.merge())
                    .addOnSuccessListener(x -> {
                        Registration r = new Registration(eventId, entrantId,
                                existingCreated != null ? existingCreated : now);
                        r.setId(entrantId);
                        r.setStatus(RegistrationStatus.ACTIVE);
                        r.setCancelledAtUtc(null);
                        onSuccess.accept(r);
                    })
                    .addOnFailureListener(onError::accept);
        }).addOnFailureListener(onError::accept);
    }

    /**
     * Cancel (by organizer or entrant).
     * Sets status to CANCELLED_BY_ORGANIZER or CANCELLED_BY_ENTRANT and stamps cancelledAtUtc.
     * Keeps createdAtUtc as-is if present.
     */
    public void cancel(@NonNull String eventId,
                       @NonNull String entrantId,
                       boolean byOrganizer,
                       @NonNull Consumer<Void> onSuccess,
                       @NonNull Consumer<Throwable> onError) {

        DocumentReference ref = regRef(eventId, entrantId);
        ref.get().addOnSuccessListener(snap -> {
            long now = System.currentTimeMillis();
            Long created = snap.exists() ? getLongSafe(snap, "createdAtUtc") : now;

            Map<String, Object> data = new HashMap<>();
            data.put("eventId", eventId);
            data.put("entrantId", entrantId);
            data.put("status", (byOrganizer
                    ? RegistrationStatus.CANCELLED_BY_ORGANIZER
                    : RegistrationStatus.CANCELLED_BY_ENTRANT).name());
            data.put("createdAtUtc", created);
            data.put("cancelledAtUtc", now);

            ref.set(data, SetOptions.merge())
                    .addOnSuccessListener(x -> onSuccess.accept(null))
                    .addOnFailureListener(onError::accept);
        }).addOnFailureListener(onError::accept);
    }

    /** Convenience for decline flow: cancel only if a registration doc already exists. */
    public void cancelIfExists(@NonNull String eventId,
                               @NonNull String entrantId,
                               @NonNull Consumer<Void> onSuccess,
                               @NonNull Consumer<Throwable> onError) {
        regRef(eventId, entrantId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        cancel(eventId, entrantId, false, onSuccess, onError);
                    } else {
                        onSuccess.accept(null);
                    }
                })
                .addOnFailureListener(onError::accept);
    }

    // ---------------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------------

    /** Final list = ACTIVE. */
    public void listFinal(@NonNull String eventId,
                          @NonNull Consumer<List<Registration>> onSuccess,
                          @NonNull Consumer<Throwable> onError) {
        db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", RegistrationStatus.ACTIVE.name())
                .get()
                .addOnSuccessListener(snap -> onSuccess.accept(hydrateList(snap.getDocuments())))
                .addOnFailureListener(onError::accept);
    }

    /** Cancelled list = CANCELLED_BY_ORGANIZER ∪ CANCELLED_BY_ENTRANT. */
    public void listCancelled(@NonNull String eventId,
                              @NonNull Consumer<List<Registration>> onSuccess,
                              @NonNull Consumer<Throwable> onError) {
        db.collection("events").document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList(
                        RegistrationStatus.CANCELLED_BY_ORGANIZER.name(),
                        RegistrationStatus.CANCELLED_BY_ENTRANT.name()))
                .get()
                .addOnSuccessListener(snap -> onSuccess.accept(hydrateList(snap.getDocuments())))
                .addOnFailureListener(onError::accept);
    }

    /** Generic by single status (e.g., "ACTIVE" or a specific CANCELLED_*). */
    public void listByStatus(@NonNull String eventId,
                             @NonNull String status,
                             @NonNull Consumer<List<Registration>> onSuccess,
                             @NonNull Consumer<Throwable> onError) {
        db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", status)
                .get()
                .addOnSuccessListener(snap -> onSuccess.accept(hydrateList(snap.getDocuments())))
                .addOnFailureListener(onError::accept);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private DocumentReference regRef(String eventId, String entrantId) {
        return db.collection("events").document(eventId)
                .collection("registrations").document(entrantId);
    }

    private List<Registration> hydrateList(List<DocumentSnapshot> docs) {
        List<Registration> out = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            Registration r = hydrate(d);
            if (r != null) out.add(r);
        }
        return out;
    }

    /**
     * Manual hydration → works even without a no-arg constructor in Registration.java.
     */
    private Registration hydrate(DocumentSnapshot d) {
        if (d == null || !d.exists()) return null;

        String eventId   = getStringSafe(d, "eventId");
        String entrantId = getStringSafe(d, "entrantId");
        Long created     = getLongSafe(d, "createdAtUtc");
        String statusStr = getStringSafe(d, "status");
        Long cancelled   = getLongNullable(d, "cancelledAtUtc");

        if (eventId == null || entrantId == null) return null;

        long createdVal = created != null ? created : 0L;
        Registration reg = new Registration(eventId, entrantId, createdVal);
        reg.setId(d.getId());
        reg.setStatus(parseStatus(statusStr));
        reg.setCancelledAtUtc(cancelled);
        return reg;
    }

    private static RegistrationStatus parseStatus(String s) {
        try {
            return s == null ? RegistrationStatus.ACTIVE : RegistrationStatus.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return RegistrationStatus.ACTIVE;
        }
    }

    private static String getStringSafe(DocumentSnapshot d, String key) {
        Object o = d.get(key);
        return (o instanceof String) ? (String) o : null;
    }

    private static Long getLongSafe(DocumentSnapshot d, String key) {
        Object o = d.get(key);
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        return null;
    }

    private static Long getLongNullable(DocumentSnapshot d, String key) {
        Object o = d.get(key);
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        return null;
    }


    //for testing purposes:
    public static Map<String, Object> toMap(String eventId, String entrantId,
                                     RegistrationStatus status, long createdAtUtc,
                                     Long cancelledAtUtc) {
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("entrantId", entrantId);
        data.put("status", status.name());
        data.put("createdAtUtc", createdAtUtc);
        data.put("cancelledAtUtc", cancelledAtUtc);
        return data;
    }

    public static Registration fromMap(String docId, Map<String, Object> m) {
        String eventId   = (String) m.get("eventId");
        String entrantId = (String) m.get("entrantId");
        Long created     = m.get("createdAtUtc") instanceof Number ? ((Number)m.get("createdAtUtc")).longValue() : null;
        String status    = (String) m.get("status");
        Long cancelled   = m.get("cancelledAtUtc") instanceof Number ? ((Number)m.get("cancelledAtUtc")).longValue() : null;

        if (eventId == null || entrantId == null) return null;
        long createdVal = created != null ? created : 0L;

        Registration r = new Registration(eventId, entrantId, createdVal);
        r.setId(docId);
        r.setStatus(parseStatus(status));
        r.setCancelledAtUtc(cancelled);
        return r;
    }
}
