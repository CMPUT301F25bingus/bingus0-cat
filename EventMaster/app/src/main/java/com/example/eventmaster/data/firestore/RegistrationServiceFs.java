package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.RegistrationService;
import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegistrationServiceFs  implements RegistrationService {
    private final FirebaseFirestore db;

    public RegistrationServiceFs(FirebaseFirestore db) {
        this.db = db;
    }

    @Override
    public Task<Void> createFromInvitation(String eventId, String invitationId, String entrantId) {
        DocumentReference regRef = db.collection("events").document(eventId)
                .collection("registrations").document(entrantId);
        Registration r = new Registration(eventId, entrantId, System.currentTimeMillis());
        return regRef.set(r, SetOptions.merge());
    }

    @Override
    public Task<List<Registration>> listFinal(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", "ACTIVE")
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(r);
                    }
                    return list;
                });
    }

    @Override
    public Task<List<Registration>> listCancelled(String eventId) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereIn("status", Arrays.asList("CANCELLED_BY_ORGANIZER", "CANCELLED_BY_ENTRANT"))
                .get()
                .continueWith(t -> {
                    List<Registration> list = new ArrayList<>();
                    for (DocumentSnapshot d : t.getResult()) {
                        Registration r = d.toObject(Registration.class);
                        if (r != null) list.add(r);
                    }
                    return list;
                });
    }
}
