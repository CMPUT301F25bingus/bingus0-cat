package com.example.eventmaster.ui.shared.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.android.material.switchmaterial.SwitchMaterial;

/**
 * Edit Profile screen: lets a user update name, email, phone, and notification preferences.
 * Uses ProfileRepositoryFs (Firestore) to load and upsert the profile.
 * Implements US 01.04.03 - Opt out of receiving notifications.
 */
public class EditProfileActivity extends AppCompatActivity {
    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private String profileId;

    private EditText etName, etEmail, etPhone;
    private SwitchMaterial switchNotifications;
    private Button btnSave;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shared_activity_edit_profile);

        profileId = getIntent().getStringExtra("profileId");
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnSave = findViewById(R.id.btnSave);

        // Load current profile data
        repo.get(profileId, p -> {
            etName.setText(p.getName());
            etEmail.setText(p.getEmail());
            etPhone.setText(p.getPhone());
            // Set notification toggle based on current preference
            switchNotifications.setChecked(p.isNotificationsEnabled());
        }, e -> {
            Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            Profile p = new Profile(
                    profileId,
                    etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etPhone.getText().toString().trim()
            );
            
            // Set notification preference
            p.setNotificationsEnabled(switchNotifications.isChecked());

            repo.upsert(p)
                    .addOnSuccessListener(x -> {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(err -> {
                        Toast.makeText(this, 
                                "Failed to save: " + err.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    });
        });
    }
}
