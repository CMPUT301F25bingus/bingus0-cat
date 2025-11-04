package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eventmaster.ui.entrant.EntrantNotificationsActivity;
import com.example.eventmaster.ui.organizer.SelectedEntrantsActivity;

/**
 * Main entry point for EventMaster application.
 * Temporary testing screen to navigate to different features.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Setup test buttons
        setupTestButtons();
    }
    
    /**
     * Sets up the test buttons to navigate to different activities.
     * This is for testing/demonstration purposes.
     */
    private void setupTestButtons() {
        // Organizer: Selected Entrants button
        Button organizerButton = findViewById(R.id.test_button_organizer);
        organizerButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectedEntrantsActivity.class);
            startActivity(intent);
        });
        
        // Entrant: Notifications button
        Button entrantButton = findViewById(R.id.test_button_entrant);
        entrantButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EntrantNotificationsActivity.class);
            startActivity(intent);
        });
    }
}