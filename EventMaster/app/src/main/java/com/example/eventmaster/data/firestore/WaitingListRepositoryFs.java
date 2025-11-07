package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Collections;
import java.util.List;

/**
 * Firestore implementation of WaitingListRepository.
 * 
 * Data Structure: events/{eventId}/waiting_list/{userId}
 *                events/{eventId}/chosen_list/{userId}
 */
public class WaitingListRepositoryFs implements WaitingListRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void addToWaitingList(WaitingListEntry entry, OnWaitingListOperationListener listener) {
        entry.setStatus("waiting");
        db.collection("events")
                .document(entry.getEventId())
                .collection("waiting_list")
                .document(entry.getUserId())
                .set(entry)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void removeFromWaitingList(String entryId, OnWaitingListOperationListener listener) {
        listener.onFailure(new Exception("Use removeFromWaitingList(eventId, userId) instead"));
    }

    @Override
    public void getWaitingListCount(String eventId, OnCountListener listener) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .addOnSuccessListener(querySnapshot -> listener.onSuccess(querySnapshot.size()))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void isUserInWaitingList(String eventId, String userId, OnCheckListener listener) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> listener.onSuccess(doc.exists()))
                .addOnFailureListener(listener::onFailure);
    }

    // ===== Organizer Methods =====

    @Override
    public void getWaitingList(String eventId, OnListLoadedListener listener) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .addOnSuccessListener(q -> listener.onSuccess(q.toObjects(WaitingListEntry.class)))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void getChosenList(String eventId, OnListLoadedListener listener) {
        db.collection("events")
                .document(eventId)
                .collection("chosen_list")
                .get()
                .addOnSuccessListener(q -> listener.onSuccess(q.toObjects(WaitingListEntry.class)))
                .addOnFailureListener(listener::onFailure);
    }

    @Override
    public void runLottery(String eventId, int numberToSelect, OnWaitingListOperationListener listener) {
        // Use LotteryServiceFs.drawLottery() instead for complete workflow
        listener.onFailure(new Exception("Use LotteryServiceFs.drawLottery() instead"));
    }
}
