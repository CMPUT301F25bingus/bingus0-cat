package com.example.eventmaster.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * Utility class for generating QR codes from text.
 * Uses the ZXing library to encode text into QR code bitmaps.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code bitmap from the given text.
     *
     * @param text The text to encode in the QR code (e.g., event ID)
     * @param width The desired width of the QR code in pixels
     * @param height The desired height of the QR code in pixels
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String text, int width, int height) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        try {
            // Create QR code writer
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Encode the text into a BitMatrix
            BitMatrix bitMatrix = qrCodeWriter.encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    width,
                    height
            );

            // Convert BitMatrix to Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Set pixel color based on BitMatrix value
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a QR code with default size (512x512 pixels).
     *
     * @param text The text to encode in the QR code
     * @return A Bitmap containing the QR code, or null if generation fails
     */
    public static Bitmap generateQRCode(String text) {
        return generateQRCode(text, 512, 512);
    }
}