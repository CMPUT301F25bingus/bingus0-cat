package com.example.eventmaster;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.eventmaster.ui.organizer.activities.OrganizerManageSpecificEventActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.*;

import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class OrganizerUpdatePosterTest {

    private static final String TEST_EVENT_ID = "test_event_update_poster";

    @Before
    public void setup() {
        Intents.init();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String,Object> data = new HashMap<>();
        data.put("title","Poster Test Event");
        data.put("location","Test");
        data.put("geolocationRequired", false);
        data.put("capacity", 10L);
        data.put("price", 0.0);

        db.collection("events").document(TEST_EVENT_ID).set(data);
    }

    @After
    public void teardown() {
        Intents.release();
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(TEST_EVENT_ID)
                .delete();
    }

    @Test
    public void testEditPoster_opensFilePicker() {
        Intent intent = new Intent(
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                OrganizerManageSpecificEventActivity.class);

        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_ID, TEST_EVENT_ID);
        intent.putExtra(OrganizerManageSpecificEventActivity.EXTRA_EVENT_TITLE, "Poster Test Event");

        try (ActivityScenario<OrganizerManageSpecificEventActivity> scenario =
                     ActivityScenario.launch(intent)) {

            // Fake image result
            Uri fakeImage = Uri.parse("content://test/image.jpg");
            Intent resultData = new Intent();
            resultData.setData(fakeImage);

            Instrumentation.ActivityResult activityResult =
                    new Instrumentation.ActivityResult(0, resultData);

            // Stub out the image picker intent
            Intents.intending(IntentMatchers.hasType("image/*"))
                    .respondWith(activityResult);

            // Scroll to edit button
            onView(withId(R.id.edit_poster_icon))
                    .perform(scrollTo(), click());

            // Verify image picker was triggered
            Intents.intended(IntentMatchers.hasType("image/*"));
        }
    }
}
