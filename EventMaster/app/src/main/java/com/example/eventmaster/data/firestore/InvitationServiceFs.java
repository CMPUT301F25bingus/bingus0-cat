package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.data.api.InvitationService;
import com.example.eventmaster.model.Invitation;
import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firestore implementation of InvitationService.
 * Writes are done with a batch for atomicity:
 * Update invitation status to ACCEPTED/DECLINED
 * If ACCEPTED: create or upsert a registration document under events/{eventId}/registrations/{entrantId}
 */

public class InvitationServiceFs implements InvitationService {
    private final FirebaseFirestore db;

    public InvitationServiceFs(FirebaseFirestore db){
        this.db = db;

    }
    @Override
    public Task<Void> respond(@NonNull String eventId, @NonNull String invitationId, boolean accept, @NonNull String entrantId) {
        DocumentReference invRef = db.collection("events").document(eventId)
                .collection("invitations").document(invitationId);

        WriteBatch batch = db.batch();
        batch.update(invRef, "status", accept ? "ACCEPTED" : "DECLINED");

        if (accept) {
            DocumentReference regRef = db.collection("events").document(eventId)
                    .collection("registrations").document(entrantId);
            Map<String, Object> reg = new HashMap<>();
            reg.put("eventId", eventId);
            reg.put("entrantId", entrantId);
            reg.put("status", "ACTIVE");
            reg.put("createdAtUtc", System.currentTimeMillis());
            batch.set(regRef, reg, SetOptions.merge());
        }

        return batch.commit();
    }

//    @Override
//    public Task<List<Invitation>> listByEntrant(String entrantId) {
//        return db.collectionGroup("invitations")
//                .whereEqualTo("entrantId", entrantId)
//               // .whereEqualTo("status", "PENDING")
//                .get()
//                .continueWith(task -> {
//                    List<Invitation> out = new ArrayList<>();
//                    if (!task.isSuccessful() || task.getResult() == null) return out;
//                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
//                        Invitation inv = d.toObject(Invitation.class);
//                        if (inv != null) {
//                            inv.setId(d.getId());
//                            out.add(inv);
//                        }
//                    }
//                    return out;
//                });
//    }

    // Above is the real implimnation, this is here for testing purposes - Chat helped
    @Override
    public Task<List<Invitation>> listByEntrant(String entrantId) {
        // DEBUG: bypass collectionGroup; hit the exact subcollection we seeded
        String eventId = "event-123"; // must match seeder in the fragment

        return db.collection("events").document(eventId)
                .collection("invitations")
                .whereEqualTo("entrantId", entrantId)
                .get()
                .continueWith(task -> {
                    List<Invitation> out = new ArrayList<>();
                    if (!task.isSuccessful() || task.getResult() == null) return out;
                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
                        Invitation inv = d.toObject(Invitation.class);
                        if (inv != null) {
                            inv.setId(d.getId());
                            out.add(inv);
                        }
                    }
                    return out;
                });
    }


}
