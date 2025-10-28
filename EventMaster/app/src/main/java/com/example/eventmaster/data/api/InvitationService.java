package com.example.eventmaster.data.api;

import com.example.eventmaster.model.Invitation;
import java.util.List;
import com.google.android.gms.tasks.Task;

/**
 * Interface/"Contract" for reading and updating invitation state (accepted, declined, pending...)
 * */

public interface InvitationService {

    /**
     * Records the entrants response to an invitation, and (on accept) creates a registration.
     *
     * @param eventId       id of the event that issued the invitation
     * @param invitationId  id of the invitation document
     * @param accept        true for accept, false for decline
     * @param entrantId     id of the entrant responding
     * @return Task that completes when the write is committed
     */
    Task<Void> respond(String eventId, String invitationId, boolean accept, String entrantId);

    /**
     * Lists all invitations for a given entrant.set to PENDING by default
     *
     * @param entrantId unique identifier of the entrant
     * @return Task that resolves to the entrant's invitations (may be empty)
     */
    Task<List<Invitation>> listByEntrant(String entrantId);
}
