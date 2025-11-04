package com.example.eventmaster;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.entrant.EventDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented test for EventDetailsActivity.
 * Tests US 01.06.01 (View event details) and US 01.06.02 (Join waiting list button).
 * 
 * NOTE: These tests require test data to be seeded in Firestore first.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class EventDetailsActivityTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testEventDetailsDisplayed() {
        // Create intent with test event ID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), 
                EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, "test_event_1");

        // Launch activity
        ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(intent);

        // Verify UI elements are displayed
        onView(withId(R.id.event_name_text)).check(matches(isDisplayed()));
        onView(withId(R.id.event_organizer_text)).check(matches(isDisplayed()));
        onView(withId(R.id.event_description_text)).check(matches(isDisplayed()));
        onView(withId(R.id.join_waiting_list_button)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testJoinWaitingListButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), 
                EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, "test_event_1");

        ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(intent);

        // Verify the join button is present (US 01.06.02)
        onView(withId(R.id.join_waiting_list_button)).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testEventDetailsLayoutComponents() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), 
                EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, "test_event_1");

        ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(intent);

        // Verify all required components are displayed
        onView(withId(R.id.event_poster_image)).check(matches(isDisplayed()));
        onView(withId(R.id.event_date_text)).check(matches(isDisplayed()));
        onView(withId(R.id.event_location_text)).check(matches(isDisplayed()));
        onView(withId(R.id.event_price_text)).check(matches(isDisplayed()));
        onView(withId(R.id.registration_period_text)).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count_text)).check(matches(isDisplayed()));

        scenario.close();
    }
}



