/**
 * QRManagerFs
 *
 * Role:
 *  - Generates and uploads QR code bitmaps for events to Firebase Storage.
 *
 * Design Pattern:
 *  - Implements Data Access / Utility layer in MVC architecture.
 *
 * Storage Path:
 *  - events/{eventId}/qr.png (PNG; public read, write requires Firebase Auth)
 *
 * Rendering:
 *  - Uses ZXing (QRCodeWriter) to render a square QR bitmap in-memory.
 *
 * Contract:
 *  - generateAndUpload(eventId, payload) → Task<String> with download URL.
 *  - renderLocal(payload, sizePx) → Bitmap for local preview/testing.
 *
 * Outstanding Issues:
 *  - None known.
 */

package com.example.eventmaster.data.firestore;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.example.eventmaster.data.api.QRManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Firebase Storage implementation of {@link QRManager}.
 * Handles QR code generation and upload for event documents.
 */
public class QRManagerFs implements QRManager {

    private final StorageReference root = FirebaseStorage.getInstance().getReference();

    /**
     * Generates a QR bitmap from a payload and uploads it to Firebase Storage.
     *
     * @param eventId Firestore event ID used for storage path naming
     * @param payload string encoded into the QR (e.g., event deep link)
     * @return {@link Task} resolving to the uploaded PNG's public download URL
     */
    @Override
    public Task<String> generateAndUpload(String eventId, String payload) {
        try {
            Bitmap bmp = renderLocal(payload, 768); // ~512–1024px looks good
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            byte[] bytes = out.toByteArray();

            StorageReference ref = root.child("events/" + eventId + "/qr.png");
            return ref.putBytes(bytes)
                    .continueWithTask(t -> ref.getDownloadUrl())
                    .continueWith(t -> {
                        if (!t.isSuccessful()) throw t.getException();
                        return t.getResult().toString();
                    });
        } catch (Exception e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Renders a QR code bitmap locally for a given payload.
     *
     * @param payload text content to encode in the QR
     * @param sizePx output bitmap size in pixels
     * @return generated {@link Bitmap}
     * @throws Exception if QR generation fails
     */
    @Override
    public Bitmap renderLocal(String payload, int sizePx) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        var bitMatrix = writer.encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < sizePx; x++) {
            for (int y = 0; y < sizePx; y++) {
                bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }
}
