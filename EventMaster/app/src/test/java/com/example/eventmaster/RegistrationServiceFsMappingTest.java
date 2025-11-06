package com.example.eventmaster;

import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RegistrationServiceFsMappingTest {

    @Test
    public void toMap_roundTrips_through_fromMap_ACTIVE() {
        long created = 1000L;
        Map<String, Object> m = RegistrationServiceFs.toMap(
                "E1","U1", RegistrationStatus.ACTIVE, created, null);

        Registration r = RegistrationServiceFs.fromMap("U1", m);
        assertNotNull(r);
        assertEquals("E1", r.getEventId());
        assertEquals("U1", r.getEntrantId());
        assertEquals(created, r.getCreatedAtUtc());
        assertEquals(RegistrationStatus.ACTIVE, r.getStatus());
        assertNull(r.getCancelledAtUtc());
        assertEquals("U1", r.getId());
    }

    @Test
    public void toMap_roundTrips_cancelledByOrganizer() {
        long created = 2000L, cancelled = 3000L;
        Map<String, Object> m = RegistrationServiceFs.toMap(
                "E9","U9", RegistrationStatus.CANCELLED_BY_ORGANIZER, created, cancelled);

        Registration r = RegistrationServiceFs.fromMap("U9", m);
        assertNotNull(r);
        assertEquals(RegistrationStatus.CANCELLED_BY_ORGANIZER, r.getStatus());
        assertEquals(Long.valueOf(cancelled), r.getCancelledAtUtc());
    }

    @Test
    public void fromMap_missingStatus_defaultsToACTIVE() {
        Map<String, Object> m = Map.of(
                "eventId", "E2",
                "entrantId", "U2",
                "createdAtUtc", 42L
        );
        Registration r = RegistrationServiceFs.fromMap("U2", m);
        assertEquals(RegistrationStatus.ACTIVE, r.getStatus());
    }

    @Test
    public void fromMap_invalidStatus_defaultsToACTIVE() {
        Map<String, Object> m = Map.of(
                "eventId", "E3",
                "entrantId", "U3",
                "createdAtUtc", 1L,
                "status", "NOT_A_STATUS"
        );
        Registration r = RegistrationServiceFs.fromMap("U3", m);
        assertEquals(RegistrationStatus.ACTIVE, r.getStatus());
    }

    @Test
    public void fromMap_missingRequired_returnsNull() {
        Map<String, Object> m = Map.of("entrantId", "U4"); // no eventId
        assertNull(RegistrationServiceFs.fromMap("U4", m));
    }
}
