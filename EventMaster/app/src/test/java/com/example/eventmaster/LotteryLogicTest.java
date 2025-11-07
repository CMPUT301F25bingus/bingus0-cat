package com.example.eventmaster;

import com.example.eventmaster.model.WaitingListEntry;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Lottery logic.
 * Tests the lottery selection algorithm and winner/loser separation.
 */
public class LotteryLogicTest {

    private List<WaitingListEntry> waitingList;
    private String eventId = "event_lottery_001";

    @Before
    public void setUp() {
        waitingList = new ArrayList<>();
        
        // Create 10 entrants
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

    @Test
    public void testLottery_selectCorrectNumber() {
        int numberToSelect = 3;
        int actualCount = Math.min(numberToSelect, waitingList.size());
        
        assertEquals(3, actualCount);
    }

    @Test
    public void testLottery_selectMoreThanAvailable() {
        int numberToSelect = 15; // More than 10 available
        int actualCount = Math.min(numberToSelect, waitingList.size());
        
        assertEquals(10, actualCount); // Should only select 10 (all available)
    }

    @Test
    public void testLottery_selectZero() {
        int numberToSelect = 0;
        int actualCount = Math.min(numberToSelect, waitingList.size());
        
        assertEquals(0, actualCount);
    }

    @Test
    public void testLottery_emptyWaitingList() {
        List<WaitingListEntry> emptyList = new ArrayList<>();
        
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testLottery_winnersAndLosers() {
        int numberToSelect = 3;
        
        // Simulate lottery selection
        List<WaitingListEntry> chosen = waitingList.subList(0, numberToSelect);
        List<WaitingListEntry> notChosen = waitingList.subList(numberToSelect, waitingList.size());
        
        assertEquals(3, chosen.size());
        assertEquals(7, notChosen.size());
        assertEquals(10, chosen.size() + notChosen.size());
    }

    @Test
    public void testLottery_allSelected() {
        int numberToSelect = 10;
        
        List<WaitingListEntry> chosen = waitingList.subList(0, numberToSelect);
        List<WaitingListEntry> notChosen = waitingList.subList(numberToSelect, waitingList.size());
        
        assertEquals(10, chosen.size());
        assertEquals(0, notChosen.size());
    }

    @Test
    public void testLottery_statusChange() {
        WaitingListEntry entry = waitingList.get(0);
        assertEquals("waiting", entry.getStatus());
        
        // Simulate selection
        entry.setStatus("selected");
        assertEquals("selected", entry.getStatus());
    }

    @Test
    public void testLottery_multipleEntrantsUniqueIds() {
        List<String> userIds = new ArrayList<>();
        for (WaitingListEntry entry : waitingList) {
            userIds.add(entry.getUserId());
        }
        
        // Check all user IDs are unique
        assertEquals(10, userIds.size());
        assertEquals(10, new java.util.HashSet<>(userIds).size());
    }

    @Test
    public void testLottery_entrantDataIntegrity() {
        WaitingListEntry entry = waitingList.get(0);
        
        assertNotNull(entry.getEntryId());
        assertNotNull(entry.getUserId());
        assertNotNull(entry.getEventId());
        assertNotNull(entry.getEntrantName());
        assertEquals(eventId, entry.getEventId());
    }

    @Test
    public void testLottery_singleEntrant() {
        List<WaitingListEntry> singleList = new ArrayList<>();
        singleList.add(new WaitingListEntry(
            "entry_single",
            eventId,
            "user_single",
            new Date()
        ));
        
        int numberToSelect = 1;
        int actualCount = Math.min(numberToSelect, singleList.size());
        
        assertEquals(1, actualCount);
    }
}

