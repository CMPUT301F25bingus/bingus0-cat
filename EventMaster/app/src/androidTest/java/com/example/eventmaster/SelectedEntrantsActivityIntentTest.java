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
import com.example.eventmaster.ui.organizer.activities.SelectedEntrantsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI/Intent test for SelectedEntrantsActivity.
 *
 * Verifies:
 * - Activity launches with eventId
 * - All core UI elements are visible
 * - Buttons are clickable
 * - RecyclerView loads (even if list empty)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SelectedEntrantsActivityIntentTest {

    /**
     * Launch the activity using a properly-targeted intent.
     */
    private ActivityScenario<SelectedEntrantsActivity> launchWithEventId() {
        Intent intent = new Intent(
                ApplicationProvider.getApplicationContext(),
                SelectedEntrantsActivity.class
        );
        intent.putExtra("eventId", "TEST_EVENT_999");

        return ActivityScenario.launch(intent);
    }

    @Test
    public void testLaunchActivityAndBasicUIVisible() {

        launchWithEventId();

        // Toolbar back button
        onView(withId(R.id.back_button_container))
                .check(matches(isDisplayed()));

        // RecyclerView
        onView(withId(R.id.selected_entrants_recycler_view))
                .check(matches(isDisplayed()));

        // Total selected count label
        onView(withId(R.id.total_selected_count))
                .check(matches(isDisplayed()));

        // Send Notification button
        onView(withId(R.id.send_notification_button))
                .check(matches(isDisplayed()));

        // Export CSV button
        onView(withId(R.id.export_csv_button))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSendNotificationButtonClickable() {
        launchWithEventId();

        onView(withId(R.id.send_notification_button))
                .perform(click());
        // No assertion needed — click must not crash
    }

    @Test
    public void testExportCsvButtonClickable() {
        launchWithEventId();

        onView(withId(R.id.export_csv_button))
                .perform(click());
        // Again — click must not crash the UI
    }

    @Test
    public void testBackButtonClickable() {
        launchWithEventId();

        onView(withId(R.id.back_button_container))
                .perform(click());
    }
}
