package com.example.eventmaster;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * Instrumented test for EventDetailsActivity.
 * Tests US 01.06.01 (View event details) and US 01.06.02 (Join waiting list button).
 * Also tests US 01.05.02 (Accept invitation) and US 01.05.03 (Decline invitation)
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

    private static Intent baseIntent(boolean forceInvited) {
        return new Intent(Intent.ACTION_MAIN)
                .setClassName("com.example.eventmaster",
                        "com.example.eventmaster.ui.entrant.EventDetailsActivity")
                .putExtra(com.example.eventmaster.ui.entrant.activities.EventDetailsActivity.EXTRA_EVENT_ID, "test_event_1")
                .putExtra("TEST_MODE", true)
                .putExtra("TEST_FORCE_INVITED", forceInvited);
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
        onView(withId(R.id.event_name_text)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.event_organizer_text)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.event_description_text)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.join_waiting_list_button)).perform(scrollTo()).check(matches(isDisplayed()));

        scenario.close();
    }

    @Test
    public void testJoinWaitingListButtonExists() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), 
                EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, "test_event_1");

        ActivityScenario<EventDetailsActivity> scenario = ActivityScenario.launch(intent);

        // Verify the join button is present (US 01.06.02)
        onView(withId(R.id.join_waiting_list_button)).perform(scrollTo()).check(matches(isDisplayed()));

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
        onView(withId(R.id.registration_date_text)).check(matches(isDisplayed()));
        onView(withId(R.id.waiting_list_count_text)).check(matches(isDisplayed()));

        scenario.close();
    }

    /** When invited → invitation include is visible, join button hidden. */
    @Test
    public void invitedBranch_showsAcceptDecline_andHidesJoin() {
        ActivityScenario.launch(baseIntent(true));

        onView(withId(R.id.invitation_include))
                .perform(scrollTo())
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.join_waiting_list_button))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));

        // Buttons exist and are clickable
        onView(withId(R.id.btnAccept)).check(matches(isDisplayed()));
        onView(withId(R.id.btnDecline)).check(matches(isDisplayed()));
    }

    /** When NOT invited → join button visible, invitation include hidden. */
    @Test
    public void notInvitedBranch_showsJoin_andHidesInvitationUI() {
        ActivityScenario.launch(baseIntent(false));

        onView(withId(R.id.join_waiting_list_button))
                .perform(scrollTo())
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));

        onView(withId(R.id.invitation_include))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    /** Clicking Accept/Decline in TEST_MODE should not crash and should be clickable. */
    @Test
    public void invitedBranch_acceptAndDecline_areClickable() {
        ActivityScenario.launch(baseIntent(true));

        onView(withId(R.id.btnAccept)).perform(scrollTo(), click());
        onView(withId(R.id.btnDecline)).perform(scrollTo(), click());

        // No assertion on Toast by default (needs custom matcher),
        // but the fact we can click without crash covers the basics.
    }

}



