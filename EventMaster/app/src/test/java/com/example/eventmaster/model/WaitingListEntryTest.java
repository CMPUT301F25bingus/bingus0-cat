package com.example.eventmaster.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the WaitingListEntry model class.
 * Tests waiting list entry creation and management.
 */
public class WaitingListEntryTest {

    private WaitingListEntry entry;
    private Date joinedDate;

    @Before
    public void setUp() {
        joinedDate = new Date();
        entry = new WaitingListEntry("entry_001", "event_001", "user_001", joinedDate);
    }

    @Test
    public void testEntryCreation() {
        assertNotNull(entry);
        assertEquals("entry_001", entry.getEntryId());
        assertEquals("event_001", entry.getEventId());
        assertEquals("user_001", entry.getUserId());
        assertEquals(joinedDate, entry.getJoinedDate());
    }

    @Test
    public void testDefaultStatus() {
        assertEquals("waiting", entry.getStatus());
    }

    @Test
    public void testStatusChange() {
        entry.setStatus("selected");
        assertEquals("selected", entry.getStatus());

        entry.setStatus("accepted");
        assertEquals("accepted", entry.getStatus());

        entry.setStatus("declined");
        assertEquals("declined", entry.getStatus());

        entry.setStatus("cancelled");
        assertEquals("cancelled", entry.getStatus());
    }

    @Test
    public void testGeolocation() {
        assertNull(entry.getLatitude());
        assertNull(entry.getLongitude());

        entry.setLatitude(53.5461);
        entry.setLongitude(-113.4938);

        assertEquals(Double.valueOf(53.5461), entry.getLatitude());
        assertEquals(Double.valueOf(-113.4938), entry.getLongitude());
    }

    @Test
    public void testSetters() {
        entry.setEntryId("entry_002");
        assertEquals("entry_002", entry.getEntryId());

        entry.setEventId("event_002");
        assertEquals("event_002", entry.getEventId());

        entry.setUserId("user_002");
        assertEquals("user_002", entry.getUserId());

        Date newDate = new Date();
        entry.setJoinedDate(newDate);
        assertEquals(newDate, entry.getJoinedDate());
    }

    @Test
    public void testNoArgConstructor() {
        WaitingListEntry emptyEntry = new WaitingListEntry();
        assertNotNull(emptyEntry);
    }
}



