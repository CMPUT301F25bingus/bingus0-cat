package com.example.eventmaster;

import com.example.eventmaster.model.Profile;
import org.junit.Test;
import static org.junit.Assert.*;


public class ProfileTest {

    @Test
    public void defaultFlags_areSafe() {
        Profile p = new Profile("id123", "Ava", "ava@x.ca", "555");
        assertEquals("entrant", p.getRole());
        assertFalse(p.getBanned());
        assertTrue(p.getActive());
    }

    @Test
    public void gettersSetters_work() {
        Profile p = new Profile();
        p.setId("u1");
        p.setName("Bob");
        p.setEmail("b@x.ca");
        p.setPhone("555");
        p.setRole("organizer");
        p.setBanned(true);
        p.setActive(false);

        assertEquals("u1", p.getId());
        assertEquals("Bob", p.getName());
        assertEquals("b@x.ca", p.getEmail());
        assertEquals("555", p.getPhone());
        assertEquals("organizer", p.getRole());
        assertTrue(p.getBanned());
        assertFalse(p.getActive());
    }
}
