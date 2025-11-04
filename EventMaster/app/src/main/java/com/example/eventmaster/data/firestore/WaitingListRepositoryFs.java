package com.example.eventmaster.data.firestore;

import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WaitingListRepositoryFs implements WaitingListRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public Task<Void> addEntrant(String eventId, WaitingListEntry entry) {
        return db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .document(entry.getEntrantId())
                .set(entry);
    }

    @Override
    public Task<Void> removeEntrant(String eventId, String entrantId) {
        return db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .document(entrantId)
                .delete();
    }

    @Override
    public Task<List<WaitingListEntry>> getWaitingList(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .continueWith(task -> {
                    List<WaitingListEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        list.add(doc.toObject(WaitingListEntry.class));
                    }
                    return list;
                });
    }

    @Override
    public Task<List<WaitingListEntry>> getChosenList(String eventId) {
        return db.collection("events")
                .document(eventId)
                .collection("chosen_list")
                .get()
                .continueWith(task -> {
                    List<WaitingListEntry> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        list.add(doc.toObject(WaitingListEntry.class));
                    }
                    return list;
                });
    }

}
