package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import java.util.List;

public interface RegistrationService {
    Task<Void> createFromInvitation(String eventId, String invitationId, String entrantId);
    Task<List<Registration>> listFinal(String eventId);
    Task<List<Registration>> listCancelled(String eventId);
}
