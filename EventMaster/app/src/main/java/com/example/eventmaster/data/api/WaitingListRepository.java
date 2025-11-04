package com.example.eventmaster.data.api;

import com.example.eventmaster.model.WaitingListEntry;

/**
 * Interface for managing event waiting lists.
 * Handles operations like joining, leaving, and querying waiting lists.
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

    /**
     * Callback interface for waiting list operations.
     */
    interface OnWaitingListOperationListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Callback interface for count operations.
     */
    interface OnCountListener {
        void onSuccess(int count);
        void onFailure(Exception e);
    }

    /**
     * Callback interface for check operations.
     */
    interface OnCheckListener {
        void onSuccess(boolean exists);
        void onFailure(Exception e);
    }
}