package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.EventDetailsActivity;
import com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Launches OrganizerCreateEventActivity, fills the form, publishes,
 * and verifies navigation to EventDetailsActivity. This writes a REAL event doc
 * to Firestore (rules must allow request.auth != null).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerCreateEventActivityTest {

    @Before
    public void setUp() throws Exception {
        // 1) Ensure we're signed in so Firestore writes are allowed
        CountDownLatch authLatch = new CountDownLatch(1);
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(t -> authLatch.countDown());
        // wait up to 10s for sign-in
        authLatch.await(10, TimeUnit.SECONDS);

        // 2) Initialize Espresso-Intents and launch the screen under test
        Intents.init();
        ActivityScenario.launch(OrganizerCreateEventActivity.class);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void createEvent_Publishes_AndNavigatesToDetails() {
        // Prepare times: open = now + 5 min, close = open + 60 min (same day)
        Calendar now = Calendar.getInstance();
        Calendar open = (Calendar) now.clone();
        open.add(Calendar.MINUTE, 5);

        Calendar close = (Calendar) open.clone();
        close.add(Calendar.MINUTE, 60);

        // 1) Fill required text fields
        onView(withId(R.id.editTitle))
                .perform(replaceText("Test Event (Espresso)"), closeSoftKeyboard());
        onView(withId(R.id.editLocation))
                .perform(replaceText("Campus Hall"), closeSoftKeyboard());
        // description optional; fill if you like:
        onView(withId(R.id.editDescription))
                .perform(replaceText("Automated UI test"), closeSoftKeyboard());

        // 2) Set registration OPEN
        onView(withId(R.id.tvRegOpen)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        open.get(Calendar.YEAR),
                        open.get(Calendar.MONTH) + 1,
                        open.get(Calendar.DAY_OF_MONTH)
                ));
        onView(withId(android.R.id.button1)).perform(click()); // OK date
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
                .perform(PickerActions.setTime(
                        open.get(Calendar.HOUR_OF_DAY),
                        open.get(Calendar.MINUTE)
                ));
        onView(withId(android.R.id.button1)).perform(click()); // OK time

        // 3) Set registration CLOSE
        onView(withId(R.id.tvRegClose)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(
                        close.get(Calendar.YEAR),
                        close.get(Calendar.MONTH) + 1,
                        close.get(Calendar.DAY_OF_MONTH)
                ));
        onView(withId(android.R.id.button1)).perform(click()); // OK date
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
                .perform(PickerActions.setTime(
                        close.get(Calendar.HOUR_OF_DAY),
                        close.get(Calendar.MINUTE)
                ));
        onView(withId(android.R.id.button1)).perform(click()); // OK time

        // 4) Button should be enabled now
        onView(withId(R.id.btnPublish)).check(matches(isEnabled()));

        // 5) Click Publish
        onView(withId(R.id.btnPublish)).perform(click());

        // 6) Verify we navigate to EventDetailsActivity (happens only after Firestore success)
        Intents.intended(IntentMatchers.hasComponent(
                EventDetailsActivity.class.getName()));
    }
}
