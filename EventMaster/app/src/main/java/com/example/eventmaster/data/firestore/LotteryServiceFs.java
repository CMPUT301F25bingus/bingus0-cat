package com.example.eventmaster.data.firestore;

import android.util.Log;

import com.example.eventmaster.data.api.LotteryService;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore implementation of LotteryService.
 * 
 * Handles the lottery draw process for events:
 * - Randomly selects entrants from waiting list
 * - Moves winners to chosen_list
 * - Creates invitations for winners
 * - Sends notifications to winners and losers
 * - Removes all entrants from waiting_list after lottery
 */
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
                        List<WaitingListEntry> notChosen = entrants.subList(actualCount, entrants.size());

                        Log.d(TAG, "Lottery selecting " + actualCount + " entrants from " + entrants.size());

                        List<Task<Void>> writeTasks = new ArrayList<>();

                        // Process WINNERS
                        for (WaitingListEntry e : chosen) {
                            e.setStatus("selected");

                            // Write to chosen_list
                            Task<Void> addTask = db.collection("events")
                                    .document(eventId)
                                    .collection("chosen_list")
                                    .document(e.getUserId())
                                    .set(e)
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "✅ Added " + e.getUserId() + " to chosen_list"))
                                    .addOnFailureListener(ex -> 
                                        Log.e(TAG, "❌ Failed to add " + e.getUserId() + " to chosen_list", ex));
                            writeTasks.add(addTask);

                            // Remove from waiting_list
                            Task<Void> removeTask = db.collection("events")
                                    .document(eventId)
                                    .collection("waiting_list")
                                    .document(e.getUserId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "✅ Removed " + e.getUserId() + " from waiting_list"))
                                    .addOnFailureListener(ex -> 
                                        Log.e(TAG, "❌ Failed to remove " + e.getUserId() + " from waiting_list", ex));
                            writeTasks.add(removeTask);

                            // Create PENDING invitation
                            Map<String, Object> invitation = new HashMap<>();
                            invitation.put("eventId", eventId);
                            invitation.put("entrantId", e.getUserId());
                            invitation.put("status", "PENDING");
                            invitation.put("createdAtUtc", System.currentTimeMillis());

                            Task<Void> invitationTask = db.collection("events")
                                    .document(eventId)
                                    .collection("invitations")
                                    .document(e.getUserId())
                                    .set(invitation, SetOptions.merge())
                                    .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "📨 Created PENDING invitation for " + e.getUserId()))
                                    .addOnFailureListener(ex ->
                                        Log.e(TAG, "❌ Failed to create invitation for " + e.getUserId(), ex));
                            writeTasks.add(invitationTask);

                            // Send notification to winner
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("eventId", eventId);
                            notification.put("recipientId", e.getUserId());
                            notification.put("type", "LOTTERY_WON");
                            notification.put("title", "You've been selected!");
                            notification.put("message", "Congratulations! You've been selected in the lottery. Please accept or decline your invitation.");
                            notification.put("isRead", false);
                            notification.put("createdAt", Timestamp.now());

                            Task<Void> notificationTask = db.collection("notifications")
                                    .add(notification)
                                    .continueWith(t -> {
                                        if (t.isSuccessful()) {
                                            Log.d(TAG, "🔔 Sent notification to " + e.getUserId());
                                        } else {
                                            Log.e(TAG, "❌ Failed to send notification to " + e.getUserId(), t.getException());
                                        }
                                        return null;
                                    });
                            writeTasks.add(notificationTask);
                        }

                        // Process LOSERS (not selected)
                        for (WaitingListEntry e : notChosen) {
                            // Add them to not_selected
                            Task<Void> addNotSelectedTask = db.collection("events")
                                    .document(eventId)
                                    .collection("not_selected")
                                    .document(e.getUserId())
                                    .set(e)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "📁 Added " + e.getUserId() + " to not_selected"))
                                    .addOnFailureListener(ex ->
                                            Log.e(TAG, "❌ Failed to add " + e.getUserId() + " to not_selected", ex));
                            writeTasks.add(addNotSelectedTask);
                            // Remove from waiting_list
                            Task<Void> removeTask = db.collection("events")
                                    .document(eventId)
                                    .collection("waiting_list")
                                    .document(e.getUserId())
                                    .delete()
                                    .addOnSuccessListener(aVoid -> 
                                        Log.d(TAG, "✅ Removed non-selected " + e.getUserId() + " from waiting_list"))
                                    .addOnFailureListener(ex -> 
                                        Log.e(TAG, "❌ Failed to remove " + e.getUserId() + " from waiting_list", ex));
                            writeTasks.add(removeTask);

                            // Send notification to loser
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("eventId", eventId);
                            notification.put("recipientId", e.getUserId());
                            notification.put("type", "LOTTERY_NOT_SELECTED");
                            notification.put("title", "Lottery Results");
                            notification.put("message", "Thank you for your interest. Unfortunately, you were not selected in this lottery. "   + "But don't worry! a spot might still open if someone else changes their mind.");
                            notification.put("isRead", false);
                            notification.put("createdAt", Timestamp.now());

                            Task<Void> notificationTask = db.collection("notifications")
                                    .add(notification)
                                    .continueWith(t -> {
                                        if (t.isSuccessful()) {
                                            Log.d(TAG, "🔔 Sent 'not selected' notification to " + e.getUserId());
                                        } else {
                                            Log.e(TAG, "❌ Failed to send notification to " + e.getUserId(), t.getException());
                                        }
                                        return null;
                                    });
                            writeTasks.add(notificationTask);
                        }

                        Log.d(TAG, "Lottery selected " + chosen.size() + " entrants, not selected " + notChosen.size() + ", executing " + writeTasks.size() + " write tasks");
                        return Tasks.whenAll(writeTasks);
                    } catch (Exception e) {
                        Log.e(TAG, "Exception in lottery operation", e);
                        return Tasks.forException(e);
                    }
                });
    }
}
