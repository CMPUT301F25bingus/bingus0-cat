package com.example.eventmaster;

import static org.junit.Assert.*;
import android.graphics.Bitmap;

import com.example.eventmaster.ui.organizer.activities.OrganizerCreateEventActivity;
import com.example.eventmaster.utils.QRCodeGenerator;
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
        Bitmap qr = QRCodeGenerator.generateQRCode("eventmaster://event/123");
        assertNotNull("QR bitmap should not be null", qr);
        assertTrue(qr.getWidth() > 0);
        assertTrue(qr.getHeight() > 0);
    }

    @Test
    public void testGenerateQRCode_bitmapSquare() {
        Bitmap qr = QRCodeGenerator.generateQRCode("test");
        assertEquals("QR should be square", qr.getWidth(), qr.getHeight());
    }
}
