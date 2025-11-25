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
        // Compare timestamp seconds (WaitingListEntry stores as Timestamp internally)
        assertEquals(joinedDate.getTime() / 1000, entry.getJoinedDate().getSeconds());
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
        assertNull(entry.getlat());
        assertNull(entry.getlng());

        entry.setlat(53.5461);
        entry.setlng(-113.4938);

        assertEquals(Double.valueOf(53.5461), entry.getlat());
        assertEquals(Double.valueOf(-113.4938), entry.getlng());
    }

    @Test
    public void testSetters() {
        entry.setEntryId("entry_002");
        assertEquals("entry_002", entry.getEntryId());

        entry.setEventId("event_002");
        assertEquals("event_002", entry.getEventId());

        entry.setUserId("user_002");
        assertEquals("user_002", entry.getUserId());

        com.google.firebase.Timestamp newTimestamp = com.google.firebase.Timestamp.now();
        entry.setJoinedDate(newTimestamp);
        assertEquals(newTimestamp, entry.getJoinedDate());
    }

    @Test
    public void testNoArgConstructor() {
        WaitingListEntry emptyEntry = new WaitingListEntry();
        assertNotNull(emptyEntry);
    }

    @Test
    public void testNameEmailPhoneFields() {
        entry.setEntrantName("Noor");
        entry.setEmail("noor@example.com");
        entry.setPhone("+1-555-666-7777");

        assertEquals("Noor", entry.getEntrantName());
        assertEquals("noor@example.com", entry.getEmail());
        assertEquals("+1-555-666-7777", entry.getPhone());
    }

    @Test
    public void testFullConstructor() {
        Date now = new Date();

        WaitingListEntry e = new WaitingListEntry(
                "entry_999",
                "event_999",
                "user_999",
                "Test User",
                "test@example.com",
                "555-1111",
                now,
                "waiting"
        );

        assertEquals("entry_999", e.getEntryId());
        assertEquals("event_999", e.getEventId());
        assertEquals("user_999", e.getUserId());
        assertEquals("Test User", e.getEntrantName());
        assertEquals("test@example.com", e.getEmail());
        assertEquals("555-1111", e.getPhone());
        assertEquals("waiting", e.getStatus());
        assertEquals(now.getTime() / 1000, e.getJoinedDate().getSeconds());
    }

    @Test
    public void testStatus_invalidValuesAcceptedAsRawString() {
        entry.setStatus("THIS_IS_INVALID");
        assertEquals("THIS_IS_INVALID", entry.getStatus());
    }

    @Test
    public void testNullFieldsSafety() {
        WaitingListEntry e = new WaitingListEntry();
        assertNull(e.getEntryId());
        assertNull(e.getEventId());
        assertNull(e.getUserId());
        assertNull(e.getEntrantName());
        assertNull(e.getEmail());
        assertNull(e.getPhone());
    }

    @Test
    public void testToStringContainsUsefulInfo() {
        String str = entry.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testJoinedDate_setNull() {
        entry.setJoinedDate(null);
        assertNull(entry.getJoinedDate());
    }

}



