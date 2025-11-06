package com.example.eventmaster;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.entrant.EventListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented test for EventListActivity.
 * Tests US 01.01.03 (View list of all available events).
 * 
 * NOTE: These tests require test data to be seeded in Firestore first.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventListActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testEventListDisplayed() {
        // Launch activity
        ActivityScenario<EventListActivity> scenario = ActivityScenario.launch(EventListActivity.class);

        // Verify UI elements are displayed
        onView(withId(R.id.search_edit_text)).check(matches(isDisplayed()));
        onView(withId(R.id.filter_button)).check(matches(isDisplayed()));
        onView(withId(R.id.events_recycler_view)).check(matches(isDisplayed()));
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testSearchBarExists() {
        ActivityScenario<EventListActivity> scenario = ActivityScenario.launch(EventListActivity.class);

        // Verify the search bar is present
        onView(withId(R.id.search_edit_text)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testFilterButtonExists() {
        ActivityScenario<EventListActivity> scenario = ActivityScenario.launch(EventListActivity.class);

        // Verify the filter button is present
        onView(withId(R.id.filter_button)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testBottomNavigationExists() {
        ActivityScenario<EventListActivity> scenario = ActivityScenario.launch(EventListActivity.class);

        // Verify bottom navigation is present
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));

        scenario.close();
    }
}



