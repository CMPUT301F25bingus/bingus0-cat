package com.example.eventmaster.data.api;

import com.example.eventmaster.ui.organizer.enrollments.EntrantRow;
import com.google.android.gms.tasks.Task;

import java.util.List;

public interface EnrollmentRepository {
    Task<List<EntrantRow>> listCancelled(String eventId); // US 02.06.02
    Task<List<EntrantRow>> listFinal(String eventId);     // US 02.06.03
}
