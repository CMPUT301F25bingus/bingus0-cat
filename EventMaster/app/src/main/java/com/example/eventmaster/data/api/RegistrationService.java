package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Registration;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public interface RegistrationService {
    Task<Void> createFromInvitation(String eventId, String invitationId, String entrantId);
    Task<List<Registration>> listFinal(String eventId);
    Task<List<Registration>> listCancelled(String eventId);
    Task<List<Registration>> listByEntrant(String entrantId);
    ListenerRegistration listenByEntrant(String entrantId,
                                         java.util.function.Consumer<List<Registration>> onData,
                                         java.util.function.Consumer<Throwable> onErr);
}