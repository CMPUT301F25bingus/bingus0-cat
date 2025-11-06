package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.action.ViewActions;

import com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity;
import com.example.eventmaster.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ✅ Intent/UI tests for OrganizerCreateEventActivity
 * Covers:
 *  - US 02.04.01: Upload event poster
 *  - US 02.01.01: Create event + generate QR
 *  - US 02.01.04: Set registration period
 */
@RunWith(AndroidJUnit4.class)
public class OrganizerCreateEventIntentTest {

    @Before
    public void setup() {
        ActivityScenario.launch(
                new Intent(Intent.ACTION_MAIN)
                        .setClassName("com.example.eventmaster",
                                "com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity")
        );
    }

    /** ✅ US 02.01.01 - Verify all form elements are visible */
    @Test
    public void testUIElementsVisible() {
        onView(withId(R.id.editTitle)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.editDescription)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.editLocation)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btnPickPoster)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvRegOpen)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.tvRegClose)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.cbGenerateQr)).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.btnPublish)).perform(scrollTo()).check(matches(isDisplayed()));

    }

    /** ✅ Validation test - should show error if title missing */
    @Test
    public void testPublishWithoutTitle_ShowsError() {
        onView(withId(R.id.btnPublish)).perform(scrollTo(), click());
        onView(withId(R.id.editTitle)).check(matches(hasErrorText("Required")));
    }

    /** ✅ US 02.01.04 - Ensure date buttons exist and are clickable */
    @Test
    public void testDateButtonsClickable() {
        onView(withId(R.id.tvRegOpen)).perform(scrollTo()).check(matches(isClickable()));
        onView(withId(R.id.tvRegClose)).perform(scrollTo()).check(matches(isClickable()));
    }

    /** ✅ US 02.04.01 - Upload Poster button + preview visible */
    @Test
    public void testUploadPosterUIVisible() {
        onView(withId(R.id.btnPickPoster))
                .check(matches(withText("Upload event poster")))
                .perform(scrollTo());
        onView(withId(R.id.imgPosterPreview)).check(matches(isDisplayed()));
    }

    /** ✅ US 02.01.01 - Full form entry + publish flow */
    @Test
    public void testCreateEventFlow_BasicInputs() {
        onView(withId(R.id.editTitle))
                .perform(scrollTo(), typeText("Campus Hackathon"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.editDescription))
                .perform(scrollTo(), typeText("A student-run coding competition."), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.editLocation))
                .perform(scrollTo(), typeText("MacEwan University"), ViewActions.closeSoftKeyboard());

        // Simulate clicking/scrolling to date fields
        onView(withId(R.id.tvRegOpen)).perform(scrollTo());
        onView(withId(R.id.tvRegClose)).perform(scrollTo());

        // Toggle QR checkbox and click Publish
        onView(withId(R.id.cbGenerateQr)).perform(scrollTo(), click());
        onView(withId(R.id.btnPublish)).perform(scrollTo(), click());

    }

    /** ✅ US 02.04.01 - Simulate poster selection and verify preview updates */
    @Test
    public void testPosterSelection_MockUriUpdatesPreview() {
        ActivityScenario<OrganizerCreateEventActivity> scenario =
                ActivityScenario.launch(OrganizerCreateEventActivity.class);

        scenario.onActivity(activity -> {
            // Create a mock drawable URI from app resources
            android.net.Uri mockUri = android.net.Uri.parse(
                    "android.resource://com.example.eventmaster/" + R.drawable.ic_launcher_foreground);

            // Simulate selecting the poster
            activity.testSetPosterPreview(mockUri);

            // Verify the ImageView updated its drawable
            android.widget.ImageView preview = activity.findViewById(R.id.imgPosterPreview);
            android.graphics.drawable.Drawable drawable = preview.getDrawable();

            org.junit.Assert.assertNotNull(
                    "Poster preview should display selected mock image", drawable);
        });
    }

}
