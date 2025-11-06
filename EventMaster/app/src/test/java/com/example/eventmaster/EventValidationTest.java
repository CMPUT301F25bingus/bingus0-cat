package com.example.eventmaster;

import com.example.eventmaster.model.Event;
import com.google.firebase.Timestamp;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * User story focus:
 *  - US 02.01.04: Set registration period (open ≤ close, required fields)
 */
public class EventValidationTest {

    private static Timestamp ts(long seconds) {
        return new Timestamp(seconds, 0);
    }

    @Test
    public void valid_whenOpenBeforeClose_andTitlePresent() {
        Event e = new Event(
                "Yoga",              // title (required, non-empty)
                "Morning session",   // description (optional)
                "Studio A",          // location (optional)
                ts(1_000),           // open
                ts(2_000)            // close
        );
        assertNull("Expected valid Event", e.validate());
    }

    @Test
    public void valid_whenOpenEqualsClose() {
        Timestamp t = ts(1_234_567);
        Event e = new Event("Hack Night", null, null, t, t);
        assertNull("Expected valid Event when open == close", e.validate());
    }

    @Test
    public void invalid_whenOpenAfterClose() {
        Event e = new Event("Swim Trials", null, null, ts(5_000), ts(4_000));
        String err = e.validate();
        assertNotNull(err);
        assertTrue(err.contains("before") || err.contains("equal"));
    }

    @Test
    public void invalid_whenTitleMissingOrBlank() {
        // Use default ctor to create a blank event and then set only times
        Event e = new Event();
        e.setRegistrationOpen(ts(1_000));
        e.setRegistrationClose(ts(2_000));
        // Title is null -> should fail
        String err1 = e.validate();
        assertNotNull(err1);
        assertTrue(err1.contains("Title"));

        // Set blank title -> should also fail
        e.setTitle("");
        String err2 = e.validate();
        assertNotNull(err2);
        assertTrue(err2.contains("Title"));
    }

    @Test
    public void invalid_whenRegistrationOpenNull() {
        Event e = new Event();
        e.setTitle("Career Fair");
        e.setRegistrationClose(ts(2_000));
        String err = e.validate();
        assertNotNull(err);
        assertTrue(err.contains("open"));
    }

    @Test
    public void invalid_whenRegistrationCloseNull() {
        Event e = new Event();
        e.setTitle("Career Fair");
        e.setRegistrationOpen(ts(1_000));
        String err = e.validate();
        assertNotNull(err);
        assertTrue(err.contains("close"));
    }
}
