package com.example.eventmaster.ui.shared.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * Activity for scanning QR codes to view event details.
 * Implements US 01.06.01 - View event details by scanning promotional QR code.
 */
public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private DecoratedBarcodeView barcodeView;
    private ImageView closeButton;
    private boolean isScanning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shared_activity_qr_scanner);

        barcodeView = findViewById(R.id.barcode_scanner);
        closeButton = findViewById(R.id.close_button);

        // Setup close button
        closeButton.setOnClickListener(v -> finish());

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Requests camera permission from the user.
     */
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Starts the QR code scanning process.
     */
    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null && isScanning) {
                    isScanning = false;
                    handleScannedCode(result.getText());
                }
            }
        });
    }

    /**
     * Handles the scanned QR code result.
     * 
     * Expected QR code format: "event_id" or "eventmaster://event/{event_id}"
     * 
     * @param scannedText The text content of the scanned QR code
     */
    private void handleScannedCode(String scannedText) {
        // Pause scanning
        barcodeView.pause();

        // Parse the event ID from scanned text
        String eventId = parseEventId(scannedText);

        if (eventId != null && !eventId.isEmpty()) {
            // Open event details
            openEventDetails(eventId);
        } else {
            // Invalid QR code
            Toast.makeText(this, "Invalid event QR code", Toast.LENGTH_SHORT).show();
            isScanning = true;
            barcodeView.resume();
        }
    }

    /**
     * Parses the event ID from the scanned QR code text.
     * Supports multiple formats:
     * - Direct event ID: "test_event_1"
     * - URI format: "eventmaster://event/test_event_1"
     * - URL format: "https://eventmaster.com/event/test_event_1"
     * 
     * @param scannedText The raw scanned text
     * @return The extracted event ID, or null if invalid
     */
    private String parseEventId(String scannedText) {
        if (scannedText == null || scannedText.isEmpty()) {
            return null;
        }

        // Check if it's a URI format
        if (scannedText.startsWith("eventmaster://event/")) {
            return scannedText.substring("eventmaster://event/".length());
        }
        
        // Check if it's a URL format
        if (scannedText.contains("/event/")) {
            int index = scannedText.lastIndexOf("/event/");
            return scannedText.substring(index + "/event/".length());
        }

        // Otherwise, assume it's a direct event ID
        return scannedText;
    }

    /**
     * Opens the event details activity with the scanned event ID.
     * 
     * @param eventId The event ID to display
     */
    private void openEventDetails(String eventId) {
        Intent intent = new Intent(this, EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, eventId);
        startActivity(intent);
        finish(); // Close scanner after opening details
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null && isScanning) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeView != null) {
            barcodeView.pause();
        }
    }
}



