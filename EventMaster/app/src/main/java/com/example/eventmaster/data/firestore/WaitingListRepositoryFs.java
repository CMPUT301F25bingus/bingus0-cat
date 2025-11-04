package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Firestore implementation of WaitingListRepository.
 * Manages waiting list entries in the Firestore database.
 */
public class WaitingListRepositoryFs implements WaitingListRepository {

    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "waitingList";

    public WaitingListRepositoryFs() {
        this.db = FirebaseFirestore.getInstance();
    }

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
                .addOnSuccessListener(querySnapshot -> {
                    listener.onSuccess(querySnapshot.size());
                })
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void isUserInWaitingList(String eventId, String userId, OnCheckListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    listener.onSuccess(!querySnapshot.isEmpty());
                })
                .addOnFailureListener(listener::onFailure);
    }
}
