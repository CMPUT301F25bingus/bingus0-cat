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

import com.example.eventmaster.ui.organizer.activities.CancelledEntrantsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent test for CancelledEntrantsActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CancelledEntrantsActivityIntentTest {

    @Test
    public void testLaunchWithIntentAndBasicUI() {

        // Create EXPLICIT Intent
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                CancelledEntrantsActivity.class
        );
        intent.putExtra("eventId", "TEST_EVENT_123");

        // Launch the activity
        ActivityScenario<CancelledEntrantsActivity> scenario =
                ActivityScenario.launch(intent);

        // Verify UI elements
        onView(withId(R.id.cancelledEntrantsTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText("Cancelled Entrants")));

        onView(withId(R.id.cancelled_entrants_recycler_view))
                .check(matches(isDisplayed()));

        onView(withId(R.id.total_selected_count))
                .check(matches(isDisplayed()));

        onView(withId(R.id.textDrawReplacement))
                .check(matches(isDisplayed()))
                .perform(click());
    }
}
