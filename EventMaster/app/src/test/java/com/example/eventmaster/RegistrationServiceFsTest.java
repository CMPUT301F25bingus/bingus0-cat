package com.example.eventmaster;

import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.RegistrationStatus;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class RegistrationServiceFsTest {

    private RegistrationStatus callParseStatus(String in) throws Exception {
        // parseStatus is private static → invoke with null instance
        Method m = RegistrationServiceFs.class.getDeclaredMethod("parseStatus", String.class);
        m.setAccessible(true);
        return (RegistrationStatus) m.invoke(null, in);
    }

    @Test
    public void parseStatus_null_returnsACTIVE() throws Exception {
        assertEquals(RegistrationStatus.ACTIVE, callParseStatus(null));
    }

    @Test
    public void parseStatus_invalid_returnsACTIVE() throws Exception {
        assertEquals(RegistrationStatus.ACTIVE, callParseStatus("NOT_A_STATUS"));
    }

    @Test
    public void parseStatus_ACTIVE_ok() throws Exception {
        assertEquals(RegistrationStatus.ACTIVE, callParseStatus("ACTIVE"));
    }

    @Test
    public void parseStatus_CANCELLED_BY_ORGANIZER_ok() throws Exception {
        assertEquals(RegistrationStatus.CANCELLED_BY_ORGANIZER,
                callParseStatus("CANCELLED_BY_ORGANIZER"));
    }

    @Test
    public void parseStatus_CANCELLED_BY_ENTRANT_ok() throws Exception {
        assertEquals(RegistrationStatus.CANCELLED_BY_ENTRANT,
                callParseStatus("CANCELLED_BY_ENTRANT"));
    }
}
