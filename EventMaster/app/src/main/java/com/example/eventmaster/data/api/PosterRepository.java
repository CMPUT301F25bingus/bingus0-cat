package com.example.eventmaster.data.api;

import android.net.Uri;
import com.google.android.gms.tasks.Task;

public interface PosterRepository {
    /** Uploads poster to Storage at posters/{eventId}.jpg and returns download URL. */
    Task<String> upload(String eventId, Uri localPoster);
}
