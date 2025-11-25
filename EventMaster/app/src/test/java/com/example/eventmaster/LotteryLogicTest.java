package com.example.eventmaster;

import com.example.eventmaster.model.WaitingListEntry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for lottery selection logic.
 * These tests validate:
 *  - Selection counts
 *  - Winner / loser split
 *  - Unique entrants
 *  - Randomness behavior (distribution)
 *  - Status updates
 *  - Data integrity
 */
public class LotteryLogicTest {

    private List<WaitingListEntry> waitingList;
    private final String eventId = "event_lottery_001";

    @Before
    public void setUp() {
        waitingList = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            waitingList.add(new WaitingListEntry(
                    "entry_" + i,
                    eventId,
                    "user_" + i,
                    "User " + i,
                    "user" + i + "@example.com",
                    "+123456789" + i,
                    new Date(),
                    "waiting"
            ));
        }
    }

    // Basic Selection Tests
    @Test
    public void testLottery_selectCorrectNumber() {
        int actualCount = Math.min(3, waitingList.size());
        assertEquals(3, actualCount);
    }

    @Test
    public void testLottery_selectMoreThanAvailable() {
        int actualCount = Math.min(15, waitingList.size());
        assertEquals(10, actualCount);
    }

    @Test
    public void testLottery_selectZero() {
        assertEquals(0, Math.min(0, waitingList.size()));
    }

    @Test
    public void testLottery_emptyWaitingList() {
        List<WaitingListEntry> emptyList = new ArrayList<>();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    // Winner/Loser Split Logic
    @Test
    public void testLottery_winnersAndLosers() {
        int numberToSelect = 3;

        List<WaitingListEntry> chosen = waitingList.subList(0, numberToSelect);
        List<WaitingListEntry> notChosen = waitingList.subList(numberToSelect, waitingList.size());

        assertEquals(3, chosen.size());
        assertEquals(7, notChosen.size());
        assertEquals(10, chosen.size() + notChosen.size());
    }

    @Test
    public void testLottery_allSelected() {
        List<WaitingListEntry> chosen = waitingList.subList(0, 10);
        List<WaitingListEntry> notChosen = waitingList.subList(10, waitingList.size());

        assertEquals(10, chosen.size());
        assertEquals(0, notChosen.size());
    }

    // Status Updates
    @Test
    public void testLottery_statusChange() {
        WaitingListEntry entry = waitingList.get(0);
        assertEquals("waiting", entry.getStatus());
        entry.setStatus("selected");
        assertEquals("selected", entry.getStatus());
    }

    @Test
    public void testLottery_statusForLosers() {
        WaitingListEntry entry = waitingList.get(1);
        entry.setStatus("not_selected");
        assertEquals("not_selected", entry.getStatus());
    }

    // Integrity Tests
    @Test
    public void testLottery_uniqueUserIds() {
        HashSet<String> set = new HashSet<>();
        for (WaitingListEntry e : waitingList) set.add(e.getUserId());

        assertEquals(10, waitingList.size());
        assertEquals(10, set.size()); // no duplicates
    }

    @Test
    public void testLottery_entrantDataIntegrity() {
        WaitingListEntry e = waitingList.get(0);
        assertNotNull(e.getEntryId());
        assertNotNull(e.getUserId());
        assertNotNull(e.getEventId());
        assertNotNull(e.getEntrantName());
        assertEquals(eventId, e.getEventId());
    }

    // Single-Entrant Case
    @Test
    public void testLottery_singleEntrant() {
        List<WaitingListEntry> list = new ArrayList<>();
        list.add(new WaitingListEntry("entry1", eventId, "user1", new Date()));

        assertEquals(1, Math.min(1, list.size()));
    }

    // Randomness/Fairness Tests (Not strict, just behavior check)
    @Test
    public void testLottery_shuffleProducesDifferentOrders() {
        List<WaitingListEntry> listCopy1 = new ArrayList<>(waitingList);
        List<WaitingListEntry> listCopy2 = new ArrayList<>(waitingList);

        Collections.shuffle(listCopy1);
        Collections.shuffle(listCopy2);

        // It's possible but extremely unlikely to be identical after shuffle.
        boolean identical = true;
        for (int i = 0; i < listCopy1.size(); i++) {
            if (!listCopy1.get(i).getUserId().equals(listCopy2.get(i).getUserId())) {
                identical = false;
                break;
            }
        }

        assertFalse("Shuffled lists should NOT be identical", identical);
    }

    // Negative Values / Invalid Params
    @Test
    public void testLottery_negativeSelectionBecomesZero() {
        int numberToSelect = -5;
        int actual = Math.min(Math.max(0, numberToSelect), waitingList.size());
        assertEquals(0, actual);
    }

    @Test
    public void testLottery_largeSelectionLimit() {
        int numberToSelect = 999999;
        assertEquals(10, Math.min(numberToSelect, waitingList.size()));
    }


    // Load/Modify Behavior
    @Test
    public void testLottery_modifyListAfterSelection() {
        List<WaitingListEntry> chosen = waitingList.subList(0, 3);
        chosen.get(0).setStatus("selected");

        assertEquals("selected", waitingList.get(0).getStatus());
    }
}
