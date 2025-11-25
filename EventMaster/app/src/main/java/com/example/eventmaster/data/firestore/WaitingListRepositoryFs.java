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
        try {
            entry.setStatus("waiting");
            
            // Debug logging
            android.util.Log.d("WaitingListRepo", "Adding to waiting list: " + 
                    "eventId=" + entry.getEventId() + 
                    ", userId=" + entry.getUserId() + 
                    ", status=" + entry.getStatus());

            String userId = entry.getUserId(); // This should be the Firebase UID
            if (userId == null || userId.isEmpty()) {
                listener.onFailure(new Exception("userId is required"));
                return;
            }
            
            db.collection("events")
                    .document(entry.getEventId())
                    .collection("waiting_list")
                    .document(userId)
                    .set(entry)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("WaitingListRepo", "Successfully added to waiting list");
                        listener.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("WaitingListRepo", "Failed to add to waiting list", e);
                        listener.onFailure(e);
                    });
        } catch (Exception e) {
            android.util.Log.e("WaitingListRepo", "Exception while adding to waiting list", e);
            listener.onFailure(e);
        }
    }
    public void joinWithLimitCheck(WaitingListEntry entry, OnWaitingListOperationListener listener) {

        String eventId = entry.getEventId();

        // Step 1: load event to get limit
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {

                    Integer limit = eventDoc.get("waitingListLimit", Integer.class);

                    // Step 2: get count
                    db.collection("events")
                            .document(eventId)
                            .collection("waiting_list")
                            .get()
                            .addOnSuccessListener(q -> {

                                int current = q.size();

                                if (limit != null && limit > 0 && current >= limit) {
                                    listener.onFailure(new Exception("Waiting list is full"));
                                    return;
                                }

                                // Otherwise proceed normally
                                addToWaitingList(entry, listener);

                            }).addOnFailureListener(listener::onFailure);

                }).addOnFailureListener(listener::onFailure);
    }


    @Override
    public void removeFromWaitingList(String entryId, OnWaitingListOperationListener listener) {
        listener.onFailure(new Exception("Use removeFromWaitingList(eventId, userId) instead"));
    }

    /**
     * Remove user from waiting list using eventId and userId.
     * This is the preferred method for the nested subcollection structure.
     */
    public void removeFromWaitingList(String eventId, String userId, OnWaitingListOperationListener listener) {
        db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
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
