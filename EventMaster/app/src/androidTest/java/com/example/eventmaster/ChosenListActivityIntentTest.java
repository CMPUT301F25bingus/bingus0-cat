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

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.activities.ChosenListActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent/UI tests for ChosenListActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ChosenListActivityIntentTest {

    @Test
    public void testChosenListLaunchesAndShowsBasicUI() {

        // Create explicitly targeted Intent
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ChosenListActivity.class
        );
        intent.putExtra("eventId", "TEST_EVENT_001");

        ActivityScenario<ChosenListActivity> scenario =
                ActivityScenario.launch(intent);

        // Top title
        onView(withId(R.id.textTotalChosen))
                .check(matches(isDisplayed()));

        // RecyclerView exists (even if empty)
        onView(withId(R.id.recyclerViewChosenList))
                .check(matches(isDisplayed()));

        // Cancel entrants link visible
        onView(withId(R.id.cancel_entrants_link))
                .check(matches(isDisplayed()));

        // Back button exists
        onView(withId(R.id.btnBack))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCancelEntrantsLinkClickable() {

        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                ChosenListActivity.class
        );
        intent.putExtra("eventId", "TEST_EVENT_002");

        ActivityScenario<ChosenListActivity> scenario =
                ActivityScenario.launch(intent);

        // Click on cancel entrants link
        onView(withId(R.id.cancel_entrants_link))
                .perform(click());
    }
}
