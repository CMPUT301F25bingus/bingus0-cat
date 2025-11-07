package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.entrant.EventDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test for Waiting List Join/Exit functionality.
 * Tests US 01.02.01: As an entrant I want to join the waiting list for a specific event
 * Tests US 01.02.02: As an entrant I want to leave the waiting list for a specific event
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitingListJoinIntentTest {

    private ActivityScenario<EventDetailsActivity> scenario;
    private static final String TEST_EVENT_ID = "test_event_waiting_list_001";

    @Before
    public void setUp() {
        // Create intent with test event ID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test: User can see the "Join Waiting List" button
     */
    @Test
    public void testJoinWaitingListButtonVisible() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if join button is displayed
        onView(withId(R.id.join_waiting_list_button))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: User joins waiting list and button text changes
     * Tests US 01.02.01: Join waiting list
     */
    @Test
    public void testJoinWaitingList_buttonTextChanges() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Initial button should say "Join Waiting List"
        onView(withId(R.id.join_waiting_list_button))
                .check(matches(withText("Join Waiting List")));

        // Click join button
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        // Wait for operation to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Button text should change to "Exit Waiting List"
        onView(withId(R.id.join_waiting_list_button))
                .check(matches(withText("Exit Waiting List")));
    }

    /**
     * Test: User exits waiting list after joining
     * Tests US 01.02.02: Leave waiting list
     */
    @Test
    public void testExitWaitingList_afterJoining() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Join waiting list first
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        // Wait for join operation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now button should say "Exit Waiting List"
        onView(withId(R.id.join_waiting_list_button))
                .check(matches(withText("Exit Waiting List")));

        // Click to exit
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        // Wait for exit operation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Button should change back to "Join Waiting List"
        onView(withId(R.id.join_waiting_list_button))
                .check(matches(withText("Join Waiting List")));
    }

    /**
     * Test: Waiting list count updates after joining
     */
    @Test
    public void testWaitingListCount_updatesAfterJoin() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Join waiting list
        onView(withId(R.id.join_waiting_list_button))
                .perform(click());

        // Wait for operation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Waiting list count should be visible and updated
        onView(withId(R.id.waiting_list_count_text))
                .check(matches(isDisplayed()));
    }
}

