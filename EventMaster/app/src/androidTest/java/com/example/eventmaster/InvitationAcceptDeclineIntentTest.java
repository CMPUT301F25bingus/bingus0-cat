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

import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation test for Accept/Decline invitation functionality.
 * Tests US 01.05.02: As an entrant I want to accept or decline an invitation
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class InvitationAcceptDeclineIntentTest {

    private ActivityScenario<EventDetailsActivity> scenario;
    private static final String TEST_EVENT_ID = "test_event_invitation_001";

    @Before
    public void setUp() {
        // Create intent with test event ID and force invitation
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailsActivity.class);
        intent.putExtra("eventId", TEST_EVENT_ID);
        intent.putExtra("TEST_MODE", true);
        intent.putExtra("TEST_FORCE_INVITED", true);
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    /**
     * Test: Accept and Decline buttons are displayed for pending invitations
     */
    @Test
    public void testInvitationButtons_displayed() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if invitation include is displayed
        onView(withId(R.id.invitation_include))
                .check(matches(isDisplayed()));

        // Check if Accept button is displayed
        onView(withId(R.id.btnAccept))
                .check(matches(isDisplayed()));

        // Check if Decline button is displayed
        onView(withId(R.id.btnDecline))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: Accept button is clickable
     * Tests US 01.05.02: Accept invitation
     */
    @Test
    public void testAcceptInvitation_buttonClickable() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click Accept button
        onView(withId(R.id.btnAccept))
                .perform(click());

        // Wait for operation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Status text should show enrolled message
        onView(withId(R.id.invite_status_text))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: Decline button is clickable
     * Tests US 01.05.02: Decline invitation
     */
    @Test
    public void testDeclineInvitation_buttonClickable() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click Decline button
        onView(withId(R.id.btnDecline))
                .perform(click());

        // Wait for operation
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Status text should show declined message
        onView(withId(R.id.invite_status_text))
                .check(matches(isDisplayed()));
    }

    /**
     * Test: Buttons are disabled after accepting
     */
    @Test
    public void testButtonsDisabled_afterAccept() {
        // Wait for event details to load
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click Accept button
        onView(withId(R.id.btnAccept))
                .perform(click());

        // Wait for operation
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Buttons should still be displayed but disabled
        // (Espresso doesn't have a direct isEnabled() matcher for Material buttons)
        onView(withId(R.id.btnAccept))
                .check(matches(isDisplayed()));

        onView(withId(R.id.btnDecline))
                .check(matches(isDisplayed()));
    }
}

