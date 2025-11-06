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

/**
 * Firestore-backed repository for /profiles.
 * Responsibilities:
 *  - get/upsert/delete a profile
 *  - stream organizers/entrants for admin screens
 *
 * Dependencies: FirebaseFirestore.
 * Notes: Requires collection "profiles" with docs keyed by user id.
 */
public class ProfileRepositoryFs {

    /**
     * Success callback used by async methods.
     * @param <T> value type delivered on success
     */
    public interface Callback<T> { void call(T value); }

    /** Error callback used by async methods. */
    public interface Errback { void call(Exception e); }

    /**
     * Stream emitter used by snapshot listeners.
     * @param <T> payload type for each emission
     */
    public interface Stream<T> { void emit(T value); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference col() { return db.collection("profiles"); }
    private DocumentReference doc(String id) { return col().document(id); }

    /**
     * Load a profile by id. If the document is missing, an empty shell profile is created
     * (with the given id) and returned so the UI can render safely.
     *
     * @param profileId Firestore document id (typically the FirebaseAuth uid)
     * @param ok success callback receiving the loaded (or shell) {@link Profile}
     * @param err error callback if the read fails
     */
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

    /**
     * Create or update a profile (merge semantics). Ensures sensible defaults for missing
     * {@code role}/{@code banned}/{@code active} fields before writing.
     *
     * @param profileId document id to write into
     * @param p profile data to persist (id will be set to {@code profileId})
     * @param ok success callback (receives {@code null})
     * @param err error callback
     */
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

    /**
     * Set the {@code banned} flag on a profile.
     *
     * @param profileId target profile id
     * @param banned new banned state
     * @param ok success callback (receives {@code null})
     * @param err error callback
     */
    public void setBanned(@NonNull String profileId,
                          boolean banned,
                          @NonNull Callback<Void> ok,
                          @NonNull Errback err) {
        doc(profileId).update("banned", banned)
                .addOnSuccessListener(v -> ok.call(null))
                .addOnFailureListener(err::call);
    }

    /**
     * Hard delete the profile document.
     * <p>Switch to a soft delete by writing {@code active=false} if desired.</p>
     *
     * @param profileId id to delete
     * @param ok success callback (receives {@code null})
     * @param err error callback
     */
    public void delete(@NonNull String profileId,
                       @NonNull Callback<Void> ok,
                       @NonNull Errback err) {
        doc(profileId).delete()
                .addOnSuccessListener(v -> ok.call(null))
                .addOnFailureListener(err::call);
    }

    /**
     * Start a live query of all organizer profiles.
     *
     * @param onData emission callback receiving the full list whenever the snapshot updates
     * @param onErr emission callback receiving the listener error
     * @return a {@link ListenerRegistration}; call {@link ListenerRegistration#remove()} to stop
     */
    public ListenerRegistration listenOrganizers(@NonNull Stream<List<Profile>> onData,
                                                 @NonNull Stream<Throwable> onErr) {
        return col().whereEqualTo("role", "organizer")
                .addSnapshotListener((qs, e) -> {
                    if (e != null) { onErr.emit(e); return; }
                    onData.emit(toList(qs));
                });
    }

    /**
     * Start a live query of all entrant profiles.
     *
     * @param onData emission callback receiving the full list whenever the snapshot updates
     * @param onErr emission callback receiving the listener error
     * @return a {@link ListenerRegistration}; call {@link ListenerRegistration#remove()} to stop
     */
    public ListenerRegistration listenEntrants(@NonNull Stream<List<Profile>> onData,
                                               @NonNull Stream<Throwable> onErr) {
        return col().whereEqualTo("role", "entrant")
                .addSnapshotListener((qs, e) -> {
                    if (e != null) { onErr.emit(e); return; }
                    onData.emit(toList(qs));
                });
    }

    /**
     * Convert a {@link QuerySnapshot} to a list of {@link Profile}s, copying each document id
     * into the {@code id} field.
     */
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
