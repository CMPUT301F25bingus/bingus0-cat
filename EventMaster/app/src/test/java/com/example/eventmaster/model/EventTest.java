package com.example.eventmaster.model;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for the Event model class.
 * Tests event creation, getters, and setters.
 */
public class EventTest {

    private Event event;
    private Date eventDate;
    private Date regStartDate;
    private Date regEndDate;

    @Before
    public void setUp() {
        Calendar cal = Calendar.getInstance();
        
        cal.add(Calendar.DAY_OF_MONTH, 14);
        eventDate = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        regStartDate = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 7);
        regEndDate = cal.getTime();

        event = new Event(
                "event_001",
                "Swimming Lessons",
                "Learn to swim",
                "Community Pool",
                eventDate,
                regStartDate,
                regEndDate,
                "org_001",
                "City Rec Centre",
                20,
                50.00
        );
    }

    @Test
    public void testEventCreation() {
        assertNotNull(event);
        assertEquals("event_001", event.getEventId());
        assertEquals("Swimming Lessons", event.getName());
        assertEquals("Learn to swim", event.getDescription());
        assertEquals("Community Pool", event.getLocation());
        assertEquals(20, event.getCapacity());
        assertEquals(50.00, event.getPrice(), 0.01);
    }

    @Test
    public void testEventOrganizer() {
        assertEquals("org_001", event.getOrganizerId());
        assertEquals("City Rec Centre", event.getOrganizerName());
    }

    @Test
    public void testEventDates() {
        assertEquals(eventDate, event.getEventDate());
        assertEquals(regStartDate, event.getRegistrationStartDate());
        assertEquals(regEndDate, event.getRegistrationEndDate());
    }

    @Test
    public void testSetters() {
        event.setName("Piano Lessons");
        assertEquals("Piano Lessons", event.getName());

        event.setCapacity(30);
        assertEquals(30, event.getCapacity());

        event.setPrice(75.00);
        assertEquals(75.00, event.getPrice(), 0.01);
    }

    @Test
    public void testWaitingListLimit() {
        assertNull(event.getWaitingListLimit()); // Default should be null (unlimited)

        event.setWaitingListLimit(100);
        assertEquals(Integer.valueOf(100), event.getWaitingListLimit());
    }

    @Test
    public void testGeolocationRequired() {
        assertFalse(event.isGeolocationRequired());
        event.setGeolocationRequired(true);
        assertTrue(event.isGeolocationRequired());
    }

    @Test
    public void testPosterUrl() {
        assertNull(event.getPosterUrl()); // Default should be null

        event.setPosterUrl("https://example.com/poster.jpg");
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());
    }

    @Test
    public void testNoArgConstructor() {
        Event emptyEvent = new Event();
        assertNotNull(emptyEvent);
    }

    @Test
    public void testPosterUrlReplacedCorrectly() {
        // Set initial poster URL
        event.setPosterUrl("https://example.com/oldPoster.jpg");
        assertEquals("https://example.com/oldPoster.jpg", event.getPosterUrl());

        // Replace with new URL (US 02.04.02 requirement)
        event.setPosterUrl("https://example.com/newPoster.jpg");
        assertEquals("https://example.com/newPoster.jpg", event.getPosterUrl());

        // Ensure old value was overridden
        assertNotEquals("Old poster should be replaced",
                "https://example.com/oldPoster.jpg",
                event.getPosterUrl());
    }


}




