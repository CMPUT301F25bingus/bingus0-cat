package com.example.eventmaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.WaitingListEntry;

import org.junit.Test;

public class WaitingListRepositoryFsLogicTest {

    static class FakeRepo {
        public void addToWaitingList(WaitingListEntry entry, WaitingListRepositoryFs.OnWaitingListOperationListener listener) {
            if (entry.getUserId() == null || entry.getUserId().isEmpty()) {
                listener.onFailure(new Exception("userId is required"));
                return;
            }
            fail("Did not expect to pass validation");
        }

        public void removeFromWaitingList(String entryId, WaitingListRepositoryFs.OnWaitingListOperationListener listener) {
            listener.onFailure(new Exception("Use removeFromWaitingList(eventId, userId) instead"));
        }
    }

    @Test
    public void testAddToWaitingList_nullUserId_fails() {

        FakeRepo repo = new FakeRepo(); // <-- NO FIREBASE CALLS

        WaitingListEntry entry = new WaitingListEntry();
        entry.setEventId("EV1");
        entry.setUserId(null);

        final boolean[] callback = {false};

        repo.addToWaitingList(entry, new WaitingListRepositoryFs.OnWaitingListOperationListener() {
            @Override public void onSuccess() {}

            @Override
            public void onFailure(Exception e) {
                callback[0] = true;
                assertEquals("userId is required", e.getMessage());
            }
        });

        assertTrue(callback[0]);
    }

    @Test
    public void testRemoveFromWaitingList_wrongOverload() {

        FakeRepo repo = new FakeRepo();

        final boolean[] fail = {false};

        repo.removeFromWaitingList("wrong_id", new WaitingListRepositoryFs.OnWaitingListOperationListener() {
            @Override public void onSuccess() {}

            @Override
            public void onFailure(Exception e) {
                fail[0] = true;
                assertEquals("Use removeFromWaitingList(eventId, userId) instead", e.getMessage());
            }
        });

        assertTrue(fail[0]);
    }
}
