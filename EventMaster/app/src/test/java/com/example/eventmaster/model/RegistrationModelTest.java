package com.example.eventmaster;

import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegistrationModelTest {

    @Test
    public void constructor_setsDefaults() {
        long now = System.currentTimeMillis();
        Registration r = new Registration("E1", "U1", now);

        assertEquals("E1", r.getEventId());
        assertEquals("U1", r.getEntrantId());
        assertEquals(now, r.getCreatedAtUtc());
        // getStatus() guards null → ACTIVE:
        assertEquals(RegistrationStatus.ACTIVE, r.getStatus());
        assertNull(r.getCancelledAtUtc());
        assertNull(r.getId());
    }

    @Test
    public void setters_roundTrip() {
        Registration r = new Registration("E2", "U9", 123L);
        r.setId("reg-123");
        r.setStatus(RegistrationStatus.CANCELLED_BY_ENTRANT);
        r.setCancelledAtUtc(456L);

        assertEquals("reg-123", r.getId());
        assertEquals(RegistrationStatus.CANCELLED_BY_ENTRANT, r.getStatus());
        assertEquals(Long.valueOf(456L), r.getCancelledAtUtc());
    }

    @Test
    public void status_guard_returnsACTIVE_whenNull() {
        Registration r = new Registration("E3", "U3", 1L);
        r.setStatus(null); // simulate bad set
        assertEquals(RegistrationStatus.ACTIVE, r.getStatus());
    }

}