package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import com.example.eventmaster.ui.organizer.enrollments.EntrantRow;
import java.util.List;

public interface RegistrationRepository {
    Task<List<Registration>> listByEvent(String eventId, String status);
    Task<List<Registration>> listActiveByEvent(String eventId);
    Task<List<EntrantRow>> listByStatus(String eventId, String status);

}
