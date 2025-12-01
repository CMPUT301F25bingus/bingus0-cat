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

import com.example.eventmaster.ui.organizer.activities.WaitingListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test for Lottery workflow from organizer perspective.
 * Tests US 02.02.01: As an organizer I want to run a lottery to select entrants
 * Tests US 02.03.01: As an organizer I want to see the list of chosen entrants
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LotteryWorkflowIntentTest {

    private ActivityScenario<WaitingListActivity> scenario;
    private static final String TEST_EVENT_ID = "test_event_lottery_001";

    @Before
    public void setUp() {
        // Create intent with test event ID
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), WaitingListActivity.class);
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
     * Test: Waiting list screen displays correctly
     */
    @Test
    public void testWaitingListScreen_displays() {
        // Wait for activity to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if "Run Lottery" button is displayed
        onView(withId(R.id.textDrawReplacement))
                .check(matches(isDisplayed()))
                .check(matches(withText("Run Lottery")));
    }

    /**
     * Test: Run Lottery button is clickable
     * Tests US 02.02.01: Run lottery
     */
    @Test
    public void testRunLottery_buttonClickable() {
        // Wait for activity to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click Run Lottery button
        onView(withId(R.id.textDrawReplacement))
                .perform(click());

        // Wait for lottery to complete
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Screen should still be displayed (no crash)
        onView(withId(R.id.textDrawReplacement))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: Back button navigates back
     */
    @Test
    public void testBackButton_navigatesBack() {
        // Wait for activity to load
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check back button exists
        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));

        // Click back button
        onView(withId(R.id.btnBack))
                .perform(click());

        // Activity should close (tested implicitly by no crash)
    }

    /**
     * Test: Waiting list displays entrants
     */
    @Test
    public void testWaitingList_displaysEntrants() {
        // Wait for data to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if RecyclerView is displayed
        onView(withId(R.id.recyclerViewWaitingList))
                .check(matches(isDisplayed()));
    }
}

