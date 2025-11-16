package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.data.api.EnrollmentRepository;
import com.example.eventmaster.ui.organizer.adapters.EntrantRow;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore schema expectations:
 *   /events/{eventId}/registrations/{entrantId} {
 *       status: "ACTIVE" | "CANCELLED" | ...
 *       name?: "Entrant Name"
 *       email?: "x@y.com"
 *   }
 */
public class EnrollmentRepositoryFs implements EnrollmentRepository {
    private final FirebaseFirestore db;

    public EnrollmentRepositoryFs(@NonNull FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public Task<List<EntrantRow>> listCancelled(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", "CANCELLED")
                .get()
                .continueWith(task -> {
                    List<EntrantRow> out = new ArrayList<>();
                    if (!task.isSuccessful() || task.getResult() == null) return out;
                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
                        String entrantId = d.getId();
                        String name  = d.getString("name");
                        String email = d.getString("email");
                        String phone = d.getString("phone");
                        out.add(new EntrantRow(
                                entrantId, name != null ? name : entrantId, email, phone
                        ));
                    }
                    return out;
                });
    }

    @Override
    public Task<List<EntrantRow>> listFinal(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .continueWith(task -> {
                    List<EntrantRow> out = new ArrayList<>();
                    if (!task.isSuccessful() || task.getResult() == null) return out;
                    for (DocumentSnapshot d : task.getResult().getDocuments()) {
                        String entrantId = d.getId();
                        String name  = d.getString("name");
                        String email = d.getString("email");
                        String phone = d.getString("phone");
                        out.add(new EntrantRow(
                                entrantId,
                                name != null ? name : entrantId,
                                email, phone
                        ));
                    }
                    return out;
                });
    }
}
