package com.example.eventmaster.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Profile model class.
 * Tests user profile creation and management.
 */
public class ProfileTest {

    private Profile profile;

    @Before
    public void setUp() {
//        profile = new Profile("user_001", "device_123", "John Doe", "john.doe@example.com");
        profile = Profile.withDeviceId("user_001", "device_123", "John Doe", "john.doe@example.com");

    }

    @Test
    public void testProfileCreation() {
        assertNotNull(profile);
        assertEquals("user_001", profile.getUserId());
        assertEquals("device_123", profile.getDeviceId());
        assertEquals("John Doe", profile.getName());
        assertEquals("john.doe@example.com", profile.getEmail());
    }

    @Test
    public void testDefaultRole() {
        assertEquals("entrant", profile.getRole());
    }

    @Test
    public void testDefaultNotifications() {
        assertTrue(profile.isNotificationsEnabled());
    }

    @Test
    public void testPhoneNumber() {
        assertNull(profile.getPhoneNumber()); // Default should be null

        profile.setPhoneNumber("780-555-1234");
        assertEquals("780-555-1234", profile.getPhoneNumber());
    }

    @Test
    public void testNotificationPreferences() {
        profile.setNotificationsEnabled(false);
        assertFalse(profile.isNotificationsEnabled());

        profile.setNotificationsEnabled(true);
        assertTrue(profile.isNotificationsEnabled());
    }

    @Test
    public void testRoleChange() {
        profile.setRole("organizer");
        assertEquals("organizer", profile.getRole());

        profile.setRole("admin");
        assertEquals("admin", profile.getRole());
    }

    @Test
    public void testSetters() {
        profile.setName("Jane Smith");
        assertEquals("Jane Smith", profile.getName());

        profile.setEmail("jane.smith@example.com");
        assertEquals("jane.smith@example.com", profile.getEmail());

        profile.setUserId("user_002");
        assertEquals("user_002", profile.getUserId());

        profile.setDeviceId("device_456");
        assertEquals("device_456", profile.getDeviceId());
    }

    @Test
    public void testNoArgConstructor() {
        Profile emptyProfile = new Profile();
        assertNotNull(emptyProfile);
    }
}




