package com.example.eventmaster.data.api;

import com.example.eventmaster.model.WaitingListEntry;
import com.google.android.gms.tasks.Task;
import java.util.List;

public interface WaitingListRepository {
    Task<Void> addEntrant(String eventId, WaitingListEntry entry);
    Task<Void> removeEntrant(String eventId, String entrantId);
    Task<List<WaitingListEntry>> getWaitingList(String eventId);
    Task<List<WaitingListEntry>> getChosenList(String eventId);
}
