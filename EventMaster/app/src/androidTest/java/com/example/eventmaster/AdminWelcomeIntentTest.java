package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmaster.ui.admin.AdminBrowseActivity;
import com.example.eventmaster.ui.admin.AdminWelcomeActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminWelcomeIntentTest {

    @Before public void setUp() { Intents.init(); }
    @After  public void tearDown() { Intents.release(); }

    @Test
    public void continueButton_opensAdminBrowse() {
        // Launch the screen under test
        try (ActivityScenario<AdminWelcomeActivity> ignored =
                     ActivityScenario.launch(new Intent(Intent.ACTION_MAIN)
                             .setClassName(
                                     "com.example.eventmaster",
                                     AdminWelcomeActivity.class.getName()))) {

            // Tap "Continue"
            onView(withId(R.id.btnContinue)).perform(click());

            // Verify we navigated to AdminBrowseActivity
            intended(hasComponent(AdminBrowseActivity.class.getName()));
        }
    }
}
