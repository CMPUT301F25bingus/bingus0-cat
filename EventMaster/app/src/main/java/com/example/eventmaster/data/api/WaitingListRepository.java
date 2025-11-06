package com.example.eventmaster.data.api;

import com.example.eventmaster.model.WaitingListEntry;

/**
 * Interface for managing event waiting lists.
 */
public interface WaitingListRepository {

    /**
     * Adds an entrant to an event's waiting list.
     *
     * @param entry    The waiting list entry to add
     * @param listener Callback for success/failure
     */
    void addToWaitingList(WaitingListEntry entry, OnWaitingListOperationListener listener);

    /**
     * Removes an entrant from an event's waiting list.
     *
     * @param entryId  The waiting list entry ID to remove
     * @param listener Callback for success/failure
     */
    void removeFromWaitingList(String entryId, OnWaitingListOperationListener listener);

    /**
     * Gets the count of entrants in a waiting list for an event.
     *
     * @param eventId  The event ID
     * @param listener Callback with the count
     */
    void getWaitingListCount(String eventId, OnCountListener listener);

    /**
     * Checks if a user is already in the waiting list for an event.
     *
     * @param eventId  The event ID
     * @param userId   The user ID
     * @param listener Callback with true/false
     */
    void isUserInWaitingList(String eventId, String userId, OnCheckListener listener);

    // ==== Organizer-specific additions ====

    /**
     * Loads all waiting list entries for an event (Organizer use).
     */
    void getWaitingList(String eventId, OnListLoadedListener listener);

    /**
     * Loads all chosen list entries for an event (Organizer use).
     */
    void getChosenList(String eventId, OnListLoadedListener listener);

    /**
     * Runs a lottery that moves random entrants from "waiting" to "chosen".
     */
    void runLottery(String eventId, int numberToSelect, OnWaitingListOperationListener listener);

    // ==== Common Callback Interfaces ====
    interface OnWaitingListOperationListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    interface OnCountListener {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    interface OnCheckListener {
        void onSuccess(boolean exists);
        void onFailure(Exception e);
    }

    interface OnListLoadedListener {
        void onSuccess(java.util.List<WaitingListEntry> list);
        void onFailure(Exception e);
    }
}
