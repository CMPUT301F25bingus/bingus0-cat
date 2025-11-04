package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Event;
import java.util.List;

/**
 * Interface for managing events.
 * Handles operations like creating, retrieving, updating, and deleting events.
 */
public interface EventRepository {

    /**
     * Retrieves all events from the database.
     *
     * @param listener Callback with the list of events
     */
    void getAllEvents(OnEventListListener listener);

    /**
     * Retrieves a single event by ID.
     *
     * @param eventId  The event ID
     * @param listener Callback with the event
     */
    void getEventById(String eventId, OnEventListener listener);

    /**
     * Callback interface for retrieving a list of events.
     */
    interface OnEventListListener {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }

    /**
     * Callback interface for retrieving a single event.
     */
    interface OnEventListener {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }
}
