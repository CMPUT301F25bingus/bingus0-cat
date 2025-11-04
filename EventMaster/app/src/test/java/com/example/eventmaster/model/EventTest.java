package com.example.eventmaster.model;

import static org.junit.Assert.*;

import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

public class EventTest {

    @Test
    public void validate_validEvent_returnsNull() {
        Timestamp start = new Timestamp(new Date(System.currentTimeMillis()));
        Timestamp end = new Timestamp(new Date(System.currentTimeMillis() + 3600_000));
        Event e = new Event("Tech Fair", "Desc", "UofA", start, end);

        assertNull(e.validate());
    }

    @Test
    public void validate_missingTitle_returnsError() {
        Timestamp now = new Timestamp(new Date());
        Event e = new Event("", "Desc", "UofA", now, now);
        assertEquals("Title is required.", e.validate());
    }

    @Test
    public void validate_endBeforeStart_returnsError() {
        Timestamp start = new Timestamp(new Date(System.currentTimeMillis()));
        Timestamp end = new Timestamp(new Date(System.currentTimeMillis() - 1000));
        Event e = new Event("Hackathon", null, null, start, end);

        assertEquals("Registration open time must be before or equal to close time.", e.validate());
    }
}
