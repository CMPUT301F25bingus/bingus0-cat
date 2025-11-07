package com.example.eventmaster.model;

import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for Registration model Timestamp compatibility.
 * Tests the new overloaded setters that handle Firestore Timestamp objects.
 */
public class RegistrationTimestampTest {

    private Registration registration;

    @Before
    public void setUp() {
        registration = new Registration("event_001", "user_001", System.currentTimeMillis());
    }

    @Test
    public void testSetCancelledAtUtc_withLong() {
        long cancelTime = System.currentTimeMillis();
        registration.setCancelledAtUtc(cancelTime);
        assertEquals(Long.valueOf(cancelTime), registration.getCancelledAtUtc());
    }

    @Test
    public void testSetCancelledAtUtc_withNull() {
        registration.setCancelledAtUtc((Long) null);
        assertNull(registration.getCancelledAtUtc());
    }

    @Test
    public void testSetCancelledAtUtc_withTimestamp() {
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);
        registration.setCancelledAtUtc(timestamp);
        
        // Should convert Timestamp to Long (milliseconds)
        assertNotNull(registration.getCancelledAtUtc());
        assertEquals(now.getTime(), registration.getCancelledAtUtc().longValue());
    }

    @Test
    public void testSetCancelledAtUtc_withNullTimestamp() {
        registration.setCancelledAtUtc((Timestamp) null);
        assertNull(registration.getCancelledAtUtc());
    }

    @Test
    public void testSetCreatedAtUtc_withLong() {
        long createTime = System.currentTimeMillis();
        registration.setCreatedAtUtc(createTime);
        assertEquals(createTime, registration.getCreatedAtUtc());
    }

    @Test
    public void testSetCreatedAtUtc_withTimestamp() {
        Date now = new Date();
        Timestamp timestamp = new Timestamp(now);
        registration.setCreatedAtUtc(timestamp);
        
        // Should convert Timestamp to long (milliseconds)
        assertEquals(now.getTime(), registration.getCreatedAtUtc());
    }

    @Test
    public void testSetCreatedAtUtc_withNullTimestamp() {
        registration.setCreatedAtUtc((Timestamp) null);
        assertEquals(0L, registration.getCreatedAtUtc());
    }

    @Test
    public void testRegistrationStatus_afterCancellation() {
        registration.setStatus(RegistrationStatus.CANCELLED_BY_ENTRANT);
        long cancelTime = System.currentTimeMillis();
        registration.setCancelledAtUtc(cancelTime);
        
        assertEquals(RegistrationStatus.CANCELLED_BY_ENTRANT, registration.getStatus());
        assertEquals(Long.valueOf(cancelTime), registration.getCancelledAtUtc());
    }

    @Test
    public void testRegistrationStatus_activeWithNoCancelTime() {
        registration.setStatus(RegistrationStatus.ACTIVE);
        registration.setCancelledAtUtc((Long) null);
        
        assertEquals(RegistrationStatus.ACTIVE, registration.getStatus());
        assertNull(registration.getCancelledAtUtc());
    }
}

