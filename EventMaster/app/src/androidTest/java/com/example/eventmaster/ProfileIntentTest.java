package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmaster.ui.shared.activities.EditProfileActivity;
import com.example.eventmaster.ui.shared.activities.ProfileActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ProfileIntentTest {

    @Before public void setUp() { Intents.init(); }
    @After  public void tearDown() { Intents.release(); }

    @Test
    public void clickingEdit_opensEditProfileActivity() {
        try (ActivityScenario<ProfileActivity> ignored =
                     ActivityScenario.launch(ProfileActivity.class)) {

            onView(withId(R.id.btnEdit)).perform(click());
            intended(hasComponent(EditProfileActivity.class.getName()));
        }
    }
}
