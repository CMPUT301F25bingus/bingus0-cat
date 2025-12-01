package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.organizer.activities.WaitingListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test for WaitingListActivity.
 *
 * Verifies:
 * - Activity launches using an Intent
 * - All key UI components exist
 * - "Run Lottery" triggers correctly
 * - RecyclerView loads (even if empty)
 * - Back button is clickable
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerViewWaitingListActivityIntentTest {

    /**
     * Helper to launch the activity correctly with an eventId.
     */
    private ActivityScenario<WaitingListActivity> launchScreen() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                WaitingListActivity.class
        );

        intent.putExtra("eventId", "TEST_EVENT_ABC");
        return ActivityScenario.launch(intent);
    }

    @Test
    public void testLaunchesAndUIVisible() {
        launchScreen();

        // Back button toolbar
        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));

        // Title text
        onView(withId(R.id.textTotalCount))
                .check(matches(isDisplayed()));

        // RecyclerView (waiting list)
        onView(withId(R.id.recyclerViewWaitingList))
                .check(matches(isDisplayed()));

        // "Run Lottery" button
        onView(withId(R.id.textDrawReplacement))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()));
    }

    @Test
    public void testRunLotteryButtonClick() {
        launchScreen();

        // User triggers "Run Lottery"
        onView(withId(R.id.textDrawReplacement))
                .perform(click());

        // No crash = success
    }

    @Test
    public void testBackButtonClickable() {
        launchScreen();

        onView(withId(R.id.btnBack))
                .perform(click());

        // No crash = success
    }
}
