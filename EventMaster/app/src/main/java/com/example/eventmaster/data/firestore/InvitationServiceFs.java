package com.example.eventmaster.data.firestore;

import static com.google.firebase.firestore.FieldValue.serverTimestamp;

import androidx.annotation.NonNull;

import com.example.eventmaster.model.Invitation;
import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Firestore service for managing event invitations.
 * 
 * Handles invitation lifecycle:
 * - Retrieving user's invitation for an event
 * - Accepting invitations (creates ACTIVE registration, removes from chosen_list)
 * - Declining invitations (creates CANCELLED_BY_ENTRANT registration, removes from chosen_list)
 */
public class InvitationServiceFs {

    private final FirebaseFirestore db;

    public InvitationServiceFs() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ---- Public API ---------------------------------------------------------

    /** Returns the user's invitation for an event if one exists (PENDING/ACCEPTED/DECLINED). */
    public void getMyInvitation(@NonNull String eventId,
                                @NonNull String userId,
                                @NonNull Consumer<Invitation> onSuccess,
                                @NonNull Consumer<Throwable> onError) {
        db.collection("events").document(eventId)
                .collection("invitations")
                .whereEqualTo("entrantId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    Invitation inv = null;
                    if (!snap.isEmpty()) {
                        DocumentSnapshot d = snap.getDocuments().get(0);
                        inv = d.toObject(Invitation.class);
                        if (inv != null) inv.setId(d.getId());
                    }
                    onSuccess.accept(inv);
                })
                .addOnFailureListener(onError::accept);
    }

    /** Accept the invitation: mark ACCEPTED and upsert Registration(status=ACTIVE). */
    public void accept(@NonNull String invitationId,
                       @NonNull String eventId,
                       @NonNull String userId,
                       @NonNull Consumer<Void> onSuccess,
                       @NonNull Consumer<Throwable> onError) {
        respond(invitationId, eventId, userId, true, onSuccess, onError);
    }

    /** Decline the invitation: mark DECLINED and upsert Registration(status=CANCELLED_BY_ENTRANT). */
    public void decline(@NonNull String invitationId,
                        @NonNull String eventId,
                        @NonNull String userId,
                        @NonNull Consumer<Void> onSuccess,
                        @NonNull Consumer<Throwable> onError) {
        respond(invitationId, eventId, userId, false, onSuccess, onError);
    }

    // ---- Internal -----------------------------------------------------------

    private void respond(@NonNull String invitationId,
                         @NonNull String eventId,
                         @NonNull String userId,
                         boolean accept,
                         @NonNull Consumer<Void> onSuccess,
                         @NonNull Consumer<Throwable> onError) {

        DocumentReference invRef = db.collection("events").document(eventId)
                .collection("invitations").document(invitationId);
        DocumentReference regRef = db.collection("events").document(eventId)
                .collection("registrations").document(userId);
        DocumentReference chosenRef = db.collection("events").document(eventId)
                .collection("chosen_list").document(userId);

        WriteBatch batch = db.batch();

        // 1. Update invitation status
        Map<String, Object> invUpdate = new HashMap<>();
        invUpdate.put("status", accept ? "ACCEPTED" : "DECLINED");
        invUpdate.put("respondedAtUtc", serverTimestamp());
        batch.set(invRef, invUpdate, SetOptions.merge());

        // 2. Create/update registration
        Map<String, Object> reg = new HashMap<>();
        reg.put("eventId", eventId);
        reg.put("entrantId", userId);
        reg.put(accept ? "enrolledAtUtc" : "cancelledAtUtc", serverTimestamp());
        reg.put("status", accept ? "ACTIVE" : "CANCELLED_BY_ENTRANT");
        batch.set(regRef, reg, SetOptions.merge());

        // 3. Remove from chosen_list (they've responded, so no longer "pending")
        batch.delete(chosenRef);

        batch.commit()
                .addOnSuccessListener(x -> onSuccess.accept(null))
                .addOnFailureListener(onError::accept);
    }
}
