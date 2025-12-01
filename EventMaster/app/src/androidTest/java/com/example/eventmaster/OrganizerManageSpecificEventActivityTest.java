package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.eventmaster.ui.organizer.activities.OrganizerEntrantMapActivity;
import com.example.eventmaster.ui.organizer.activities.OrganizerManageSpecificEventActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

/**
 * Espresso/Intent tests for:
 * - US 02.02.02 View entrant locations on map
 * - US 02.02.03 Enable/disable geolocation requirement
 *
 * We verify:
 *  - When geolocationRequired == true → "View Map" button is visible.
 *  - Clicking it launches OrganizerEntrantMapActivity.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganizerManageSpecificEventActivityTest {

    private static final String TEST_EVENT_ID = "test_event_geo_true";

    /**
     * Polling wait that repeatedly checks view visibility
     * until timeout. Prevents failures due to Firebase async load delay.
     */
    private void waitForView(int viewId, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (true) {
            try {
                onView(withId(viewId)).perform(scrollTo());
                onView(withId(viewId)).check(matches(isDisplayed()));
                return; // SUCCESS
            } catch (Exception e) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    throw e; // TIMEOUT
                }
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
        }
    }

    @Before
    public void setUp() {
        Intents.init();

        // Seed a minimal event document with geolocationRequired = true
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("title", "Geo Test Event");
        data.put("geolocationRequired", true);
        data.put("location", "Test Location");
        data.put("capacity", 5L);
        data.put("price", 0.0);

        db.collection("events").document(TEST_EVENT_ID).set(data);
    }

    @After
    public void tearDown() {
        Intents.release();
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(TEST_EVENT_ID)
                .delete();
    }

    @Test
    public void whenGeolocationEnabled_viewMapButtonVisible_andOpensMapScreen() {

        Intent intent = new Intent(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                OrganizerManageSpecificEventActivity.class
        );
        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_ID, TEST_EVENT_ID);
        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_TITLE, "Geo Test Event");

        try (ActivityScenario<OrganizerManageSpecificEventActivity> scenario =
                     ActivityScenario.launch(intent)) {

            // ⭐ Wait up to 8 seconds for Firestore to load async data
            waitForView(R.id.btnViewMap, 8000);

            // 1) Map button must now be visible (after scrolling)
            onView(withId(R.id.btnViewMap))
                    .perform(scrollTo())
                    .check(matches(isDisplayed()));

            // 2) Click should open OrganizerEntrantMapActivity
            onView(withId(R.id.btnViewMap))
                    .perform(scrollTo(), click());

            intended(hasComponent(OrganizerEntrantMapActivity.class.getName()));
        }
    }
}
