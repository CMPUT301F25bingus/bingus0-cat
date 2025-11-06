package com.example.eventmaster;

import static org.junit.Assert.*;
import android.graphics.Bitmap;

import com.example.eventmaster.ui.organizer.OrganizerCreateEventActivity;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class OrganizerCreateEventUnitTest {

    private OrganizerCreateEventActivity activity;

    @Before
    public void setUp() {
        activity = new OrganizerCreateEventActivity();
    }

    @Test
    public void testParseDateToTimestamp_validDate() {
        Timestamp ts = activity.parseDateToTimestamp("2025-11-03 14:30");
        assertNotNull(ts);
    }

    @Test
    public void testParseDateToTimestamp_invalidDate() {
        Timestamp ts = activity.parseDateToTimestamp("invalid");
        assertNull(ts);
    }

    @Test
    public void testCreateQrBitmap_notNull() {
        Bitmap qr = activity.createQrBitmap("eventmaster://event/123", 400);
        assertNotNull(qr);
        assertEquals(400, qr.getWidth());
        assertEquals(400, qr.getHeight());
    }

    @Test
    public void testBitmapToPng_notEmpty() {
        Bitmap bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        byte[] bytes = activity.bitmapToPng(bmp);
        assertTrue(bytes.length > 0);
    }
}
