package com.example.eventmaster;

import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;

import org.junit.Test;

import static org.junit.Assert.*;

public class RegistrationServiceFsLogicOnlyTest {

    @Test
    public void testEnrollMapping_createsProperRegistration() {
        long now = System.currentTimeMillis();

        // simulate newly created registration
        Registration r = new Registration("EV1", "USR1", now);
        r.setStatus(RegistrationStatus.ACTIVE);

        assertEquals("EV1", r.getEventId());
        assertEquals("USR1", r.getEntrantId());
        assertEquals(now, r.getCreatedAtUtc());
        assertNull(r.getCancelledAtUtc());
    }

    @Test
    public void testCancelMapping_setsCorrectFields() {
        long created = 1000L;
        long cancelled = 2000L;

        // simulate service logic
        Registration r = new Registration("EV1", "USR1", created);
        r.setStatus(RegistrationStatus.CANCELLED_BY_ORGANIZER);
        r.setCancelledAtUtc(cancelled);

        assertEquals(RegistrationStatus.CANCELLED_BY_ORGANIZER, r.getStatus());
        assertEquals(Long.valueOf(cancelled), r.getCancelledAtUtc());
    }

    @Test
    public void testCancelIfExists_noRecord_noCrash() {
        // logic test: cancelIfExists simply returns success if record missing
        // we simulate this logically:
        Registration r = RegistrationServiceFs.fromMap("ID", new java.util.HashMap<>());

        assertNull(r); // meaning "no record exists" → cancelIfExists is a no-op
    }

}
