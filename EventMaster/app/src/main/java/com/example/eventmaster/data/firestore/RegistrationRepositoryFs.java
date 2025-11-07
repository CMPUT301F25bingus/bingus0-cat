package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventmaster.data.api.RegistrationRepository;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.ui.organizer.enrollments.EntrantRow;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistrationRepositoryFs implements RegistrationRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public Task<List<Registration>> listByEvent(@NonNull String eventId, @Nullable String status) {
        com.google.firebase.firestore.Query q =
                db.collection("events").document(eventId).collection("registrations");
        if (status != null) q = q.whereEqualTo("status", status);

        return q.get().continueWith(task -> {
            List<Registration> out = new ArrayList<>();
            if (!task.isSuccessful() || task.getResult() == null) return out;
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                Registration r = d.toObject(Registration.class);
                if (r != null) { r.setId(d.getId()); out.add(r); }
            }
            return out;
        });

//        var q = db.collection("events").document(eventId)
//                .collection("registrations");
//        if (status != null) q = (com.google.firebase.firestore.CollectionReference) q.whereEqualTo("status", status);
//
//        return q.get().continueWith(task -> {
//            List<Registration> out = new ArrayList<>();
//            if (!task.isSuccessful() || task.getResult() == null) return out;
//            for (DocumentSnapshot d : task.getResult().getDocuments()) {
//                Registration r = d.toObject(Registration.class);
//                if (r != null) {
//                    r.setId(d.getId());
//                    out.add(r);
//                }
//            }
//            return out;
//        });
    }

    @Override
    public Task<List<Registration>> listActiveByEvent(@NonNull String eventId) {
        return listByEvent(eventId, "ACTIVE");
    }

    @Override
    public Task<List<EntrantRow>> listByStatus(@NonNull String eventId, @NonNull String status) {
        return db.collection("events").document(eventId)
                .collection("registrations")
                .whereEqualTo("status", status)
                .get()
                .continueWithTask(snapTask -> {
                    if (!snapTask.isSuccessful() || snapTask.getResult() == null) {
                        Exception e = snapTask.getException() != null ? snapTask.getException() :
                                new IllegalStateException("Query failed");
                        return Tasks.forException(e);
                    }

                    List<DocumentSnapshot> regs = snapTask.getResult().getDocuments();
                    if (regs.isEmpty()) return Tasks.forResult(new ArrayList<>());

                    List<EntrantRow> out = new ArrayList<>(regs.size());
                    AtomicInteger remaining = new AtomicInteger(regs.size());
                    TaskCompletion<List<EntrantRow>> tc = new TaskCompletion<>();

                    for (DocumentSnapshot r : regs) {
                        String entrantId = r.getString("entrantId");  // doc id is entrantId
                        assert entrantId != null;
                        db.collection("profiles").document(entrantId).get()
                                .addOnSuccessListener(p -> {
                                    String name = p.getString("name");
                                    String email = p.getString("email");
                                    String phone = p.getString("phone");
                                    out.add(new EntrantRow(entrantId,
                                            name != null ? name : entrantId,
                                            email != null ? email : "",
                                            phone != null ? phone : ""));
                                    if (remaining.decrementAndGet() == 0) tc.success(out);
                                })
                                .addOnFailureListener(e -> {
                                    out.add(new EntrantRow(entrantId, entrantId, "", ""));
                                    if (remaining.decrementAndGet() == 0) tc.success(out);
                                });
                    }
                    return tc.task();
                });
    }

    /** tiny helper to turn callbacks into a Task without extra libs */
    private static class TaskCompletion<T> {
        private final com.google.android.gms.tasks.TaskCompletionSource<T> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();
        Task<T> task() { return tcs.getTask(); }
        void success(T v) { tcs.setResult(v); }
        void fail(Exception e) { tcs.setException(e); }
    }

}
