package com.example.eventmaster.data.api;

import android.graphics.Bitmap;

import com.google.android.gms.tasks.Task;

public interface QRManager {
    /** Generates a QR for the given payload, uploads to Storage, returns public URL. */
    Task<String> generateAndUpload(String eventId, String payload);

    /** Exposed for preview in UI (no upload). */
    Bitmap renderLocal(String payload, int sizePx) throws Exception;
}
