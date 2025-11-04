package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;

import com.example.eventmaster.model.Profile;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;

public class ProfileRepositoryFs {

    // Tiny local callbacks so the class is self-contained.
    public interface Callback<T> { void call(T value); }
    public interface Errback { void call(Exception e); }
    public interface Stream<T> { void emit(T value); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference col() { return db.collection("profiles"); }
    private DocumentReference doc(String id) { return col().document(id); }

    public void get(@NonNull String profileId,
                    @NonNull Callback<Profile> ok,
                    @NonNull Errback err) {
        doc(profileId).get()
                .addOnSuccessListener(snap -> {
                    Profile p = snap.toObject(Profile.class);
                    if (p == null) p = new Profile(profileId, "", "", "");
                    p.setId(snap.getId());
                    ok.call(p);
                })
                .addOnFailureListener(err::call);
    }

    public void upsert(@NonNull String profileId,
                       @NonNull Profile p,
                       @NonNull Callback<Void> ok,
                       @NonNull Errback err) {
        p.setId(profileId);
        if (p.getRole() == null) p.setRole("entrant");
        if (p.getBanned() == null) p.setBanned(false);
        if (p.getActive() == null) p.setActive(true);

        doc(profileId).set(p, SetOptions.merge())
                .addOnSuccessListener(v -> ok.call(null))
                .addOnFailureListener(err::call);
    }

    public void setBanned(@NonNull String profileId,
                          boolean banned,
                          @NonNull Callback<Void> ok,
                          @NonNull Errback err) {
        doc(profileId).update("banned", banned)
                .addOnSuccessListener(v -> ok.call(null))
                .addOnFailureListener(err::call);
    }

    public void delete(@NonNull String profileId,
                       @NonNull Callback<Void> ok,
                       @NonNull Errback err) {
        doc(profileId).delete()
                .addOnSuccessListener(v -> ok.call(null))
                .addOnFailureListener(err::call);
    }

    public ListenerRegistration listenOrganizers(@NonNull Stream<List<Profile>> onData,
                                                 @NonNull Stream<Throwable> onErr) {
        return col().whereEqualTo("role", "organizer")
                .addSnapshotListener((qs, e) -> {
                    if (e != null) { onErr.emit(e); return; }
                    onData.emit(toList(qs));
                });
    }

    public ListenerRegistration listenEntrants(@NonNull Stream<List<Profile>> onData,
                                               @NonNull Stream<Throwable> onErr) {
        return col().whereEqualTo("role", "entrant")
                .addSnapshotListener((qs, e) -> {
                    if (e != null) { onErr.emit(e); return; }
                    onData.emit(toList(qs));
                });
    }

    private List<Profile> toList(QuerySnapshot qs) {
        List<Profile> out = new ArrayList<>();
        if (qs == null) return out;
        for (DocumentSnapshot d : qs.getDocuments()) {
            Profile p = d.toObject(Profile.class);
            if (p != null) { p.setId(d.getId()); out.add(p); }
        }
        return out;
    }
}
