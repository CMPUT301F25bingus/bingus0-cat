package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Collections;
import java.util.List;

/**
 * Firestore implementation of WaitingListRepository.
 */
public class WaitingListRepositoryFs implements WaitingListRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "waitingList";

    @Override
    public void addToWaitingList(WaitingListEntry entry, OnWaitingListOperationListener listener) {
        db.collection(COLLECTION_NAME)
                .document(entry.getEntryId())
                .set(entry)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void removeFromWaitingList(String entryId, OnWaitingListOperationListener listener) {
        db.collection(COLLECTION_NAME)
                .document(entryId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void getWaitingListCount(String eventId, OnCountListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(querySnapshot -> listener.onSuccess(querySnapshot.size()))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void isUserInWaitingList(String eventId, String userId, OnCheckListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(q -> listener.onSuccess(!q.isEmpty()))
                .addOnFailureListener(listener::onFailure);
    }

    // ===== Organizer Methods =====

    @Override
    public void getWaitingList(String eventId, OnListLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(q -> listener.onSuccess(q.toObjects(WaitingListEntry.class)))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void getChosenList(String eventId, OnListLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "chosen")
                .get()
                .addOnSuccessListener(q -> listener.onSuccess(q.toObjects(WaitingListEntry.class)))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void runLottery(String eventId, int numberToSelect, OnWaitingListOperationListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "waiting")
                .get()
                .addOnSuccessListener(query -> {
                    List<WaitingListEntry> entries = query.toObjects(WaitingListEntry.class);
                    if (entries.isEmpty()) {
                        listener.onFailure(new Exception("No entrants in waiting list."));
                        return;
                    }
                    Collections.shuffle(entries);
                    List<WaitingListEntry> chosen = entries.subList(0, Math.min(numberToSelect, entries.size()));
                    for (WaitingListEntry e : chosen) {
                        e.setStatus("chosen");
                        db.collection(COLLECTION_NAME).document(e.getEntryId()).set(e);
                    }
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }
}
