package com.example.eventmaster;

import static org.junit.Assert.*;
import com.example.eventmaster.model.Event;
import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Calendar;

public class EventTest {
    private Timestamp ts(int y,int m,int d,int h,int min){ Calendar c=Calendar.getInstance(); c.set(y,m-1,d,h,min,0); c.set(Calendar.MILLISECOND,0); return new Timestamp(c.getTime()); }

    @Test public void validate_requiresTitleAndLocation() {
        Event e = new Event("", "d", "", ts(2025,10,27,10,0), ts(2025,10,27,11,0));
        String err = e.validate();
        assertNotNull(err);
    }

    @Test public void validate_openBeforeCloseOk() {
        Event e = new Event("t", "d", "loc", ts(2025,10,27,10,0), ts(2025,10,27,11,0));
        assertNull(e.validate());
    }

    @Test public void validate_rejectsOpenAfterClose() {
        Event e = new Event("t", "d", "loc", ts(2025,10,27,12,0), ts(2025,10,27,11,0));
        assertNotNull(e.validate());
    }
}
