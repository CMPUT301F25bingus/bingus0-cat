package com.example.eventmaster;

import com.example.eventmaster.model.Invitation;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Invitation workflow.
 * Tests invitation creation, status transitions, and acceptance/decline flow.
 */
public class InvitationWorkflowTest {

    private Invitation invitation;
    private String eventId = "event_invite_001";
    private String entrantId = "user_invite_001";

    @Before
    public void setUp() {
        invitation = new Invitation();
        invitation.setId("invitation_001");
        invitation.setEventId(eventId);
        invitation.setEntrantId(entrantId);
        invitation.setStatus("PENDING");
    }

    @Test
    public void testInvitation_creation() {
        assertNotNull(invitation);
        assertEquals(eventId, invitation.getEventId());
        assertEquals(entrantId, invitation.getEntrantId());
        assertEquals("PENDING", invitation.getStatus());
    }

    @Test
    public void testInvitation_statusTransition_pendingToAccepted() {
        assertEquals("PENDING", invitation.getStatus());
        
        invitation.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", invitation.getStatus());
    }

    @Test
    public void testInvitation_statusTransition_pendingToDeclined() {
        assertEquals("PENDING", invitation.getStatus());
        
        invitation.setStatus("DECLINED");
        assertEquals("DECLINED", invitation.getStatus());
    }

    @Test
    public void testInvitation_acceptWorkflow() {
        // Initial state: PENDING
        invitation.setStatus("PENDING");
        assertEquals("PENDING", invitation.getStatus());
        
        // User accepts
        invitation.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", invitation.getStatus());
    }

    @Test
    public void testInvitation_declineWorkflow() {
        // Initial state: PENDING
        invitation.setStatus("PENDING");
        assertEquals("PENDING", invitation.getStatus());
        
        // User declines
        invitation.setStatus("DECLINED");
        assertEquals("DECLINED", invitation.getStatus());
    }

    @Test
    public void testInvitation_multipleInvitationsDifferentUsers() {
        Invitation invite1 = new Invitation();
        invite1.setId("inv_1");
        invite1.setEventId("event_001");
        invite1.setEntrantId("user_001");
        invite1.setStatus("PENDING");
        
        Invitation invite2 = new Invitation();
        invite2.setId("inv_2");
        invite2.setEventId("event_001");
        invite2.setEntrantId("user_002");
        invite2.setStatus("PENDING");
        
        assertNotEquals(invite1.getId(), invite2.getId());
        assertNotEquals(invite1.getEntrantId(), invite2.getEntrantId());
        assertEquals(invite1.getEventId(), invite2.getEventId());
    }

    @Test
    public void testInvitation_statusTransitions() {
        invitation.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", invitation.getStatus());
        
        // Model allows status changes, we just document the behavior
        invitation.setStatus("DECLINED");
        assertEquals("DECLINED", invitation.getStatus());
    }

    @Test
    public void testInvitation_completeLifecycle() {
        // 1. Created as PENDING
        Invitation lifecycle = new Invitation();
        lifecycle.setEventId("event_002");
        lifecycle.setEntrantId("user_002");
        lifecycle.setStatus("PENDING");
        
        assertEquals("PENDING", lifecycle.getStatus());
        
        // 2. User accepts
        lifecycle.setStatus("ACCEPTED");
        assertEquals("ACCEPTED", lifecycle.getStatus());
    }

    @Test
    public void testInvitation_allStatusValues() {
        String[] statuses = {"PENDING", "ACCEPTED", "DECLINED"};
        
        for (String status : statuses) {
            invitation.setStatus(status);
            assertEquals(status, invitation.getStatus());
        }
    }
}

