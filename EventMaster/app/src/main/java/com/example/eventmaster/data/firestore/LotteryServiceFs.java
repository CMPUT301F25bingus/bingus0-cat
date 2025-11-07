package com.example.eventmaster.data.firestore;

import android.util.Log;

import com.example.eventmaster.data.api.LotteryService;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LotteryServiceFs implements LotteryService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "LotteryServiceFs";

    @Override
    public Task<Void> drawLottery(String eventId, int numberToSelect) {
        return db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .continueWithTask(task -> {
                    try {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Failed to fetch waiting list", task.getException());
                            throw task.getException();
                        }

                        List<WaitingListEntry> entrants = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            WaitingListEntry entry = doc.toObject(WaitingListEntry.class);
                            entrants.add(entry);
                        }

                        if (entrants.isEmpty()) {
                            Log.w(TAG, "No entrants found for event " + eventId);
                            return Tasks.forResult(null);
                        }

                        // Randomly shuffle and pick
                        Collections.shuffle(entrants);
                        int actualCount = Math.min(numberToSelect, entrants.size());
                        List<WaitingListEntry> chosen = entrants.subList(0, actualCount);

                        Log.d(TAG, "Lottery selecting " + actualCount + " entrants from " + entrants.size());

                        List<Task<Void>> writeTasks = new ArrayList<>();

                        for (WaitingListEntry e : chosen) {
                            e.setStatus("selected");

                            // Write to chosen_list
                            Task<Void> addTask = db.collection("events")
                                    .document(eventId)
                                    .collection("chosen_list")
                                    .document(e.getUserId())
                                    .set(e)
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "Added " + e.getUserId() + " to chosen_list"))
                                    .addOnFailureListener(ex -> 
                                        Log.e(TAG, "Failed to add " + e.getUserId() + " to chosen_list", ex));
                            writeTasks.add(addTask);

                            // Remove from waiting_list
                            Task<Void> removeTask = db.collection("events")
                                    .document(eventId)
                                    .collection("waiting_list")
                                    .document(e.getUserId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "Removed " + e.getUserId() + " from waiting_list"))
                                    .addOnFailureListener(ex -> 
                                        Log.e(TAG, "Failed to remove " + e.getUserId() + " from waiting_list", ex));
                            writeTasks.add(removeTask);
                        }

                        Log.d(TAG, "Lottery selected " + chosen.size() + " entrants, executing " + writeTasks.size() + " write tasks");
                        return Tasks.whenAll(writeTasks);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in lottery operation", e);
                        return Tasks.forException(e);
                    }
                });
    }
}
