package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Map;

/**
 * Unified EventRepository API (supports both Task-based and callback-based styles).
 *
 * Use whichever style your caller prefers:
 *  - Task-based: create/update/publish + getAllEvents()/getEventById() returning Task<T>
 *  - Callback-based: getAllEvents(listener), getEventById(eventId, listener)
 */
public interface EventRepository {

    // ---------- Write operations (Task-based) ----------
    /** Creates a new event doc and returns its generated ID. */
    Task<String> create(Event e);

    /** Partial update for an existing event. (e.g., { "posterUrl": "...", "capacity": 60 }) */
    Task<Void> update(String eventId, Map<String, Object> fields);

    /** Sets status=PUBLISHED (or whatever publish semantics your impl uses). */
    Task<Void> publish(String eventId);

    /** Deletes an event by ID. */
    Task<Void> delete(String eventId);

    // ---------- Read operations (Task-based) ----------
    /** Retrieves all events as a Task. */
    Task<List<Event>> getAllEvents();

    /** Retrieves a single event by ID as a Task. */
    Task<Event> getEventById(String eventId);

    // ---------- Read operations (Callback-based) ----------
    /**
     * Retrieves all events using a callback listener.
     * Useful for legacy code or when avoiding Tasks in presenters/adapters.
     */
    void getAllEvents(OnEventListListener listener);

    /**
     * Retrieves a single event by ID using a callback listener.
     */
    void getEventById(String eventId, OnEventListener listener);

    // ---------- Listener types (for callback-based usage) ----------
    interface OnEventListListener {
        void onSuccess(List<Event> events);
        void onFailure(Exception e);
    }

    interface OnEventListener {
        void onSuccess(Event event);
        void onFailure(Exception e);
    }
}
