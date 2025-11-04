package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Event;
import com.google.android.gms.tasks.Task;

public interface EventReadService {
    Task<Event> get(String eventId);
}
