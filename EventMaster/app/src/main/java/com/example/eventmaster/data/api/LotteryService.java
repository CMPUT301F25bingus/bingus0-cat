package com.example.eventmaster.data.api;

import com.google.android.gms.tasks.Task;

public interface LotteryService {
    Task<Void> drawLottery(String eventId, int numberToSelect);
}
