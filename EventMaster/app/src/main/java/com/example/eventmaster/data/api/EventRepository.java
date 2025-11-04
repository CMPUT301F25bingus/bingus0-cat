package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public interface EventRepository {
    /** Creates a new event doc and returns its generated ID. */
    Task<String> create(Event e);

    /** Partial update for an existing event. */
    Task<Void> update(String eventId, Map<String, Object> fields);

    /** Sets status=PUBLISHED. */
    Task<Void> publish(String eventId);
}