package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.entrant.activities.EntrantWelcomeActivity;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Profile creation screen for entrants after anonymous login.
 * Allows entrants to set their name, email, and phone number.
 */
public class CreateProfileActivity extends AppCompatActivity {
    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private MaterialButton saveButton;
    private ProfileRepositoryFs profileRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_profile);

        profileRepo = new ProfileRepositoryFs();

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        phoneLayout = findViewById(R.id.phoneLayout);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        saveButton = findViewById(R.id.btnSave);

        saveButton.setOnClickListener(v -> handleSaveProfile());
    }

    private void handleSaveProfile() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (!validateInput(name, email, phone)) {
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated. Please try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        String deviceId = DeviceUtils.getDeviceId(this);

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        // Get existing profile and update it
        profileRepo.get(uid).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Profile profile = task.getResult();
                profile.setName(name);
                profile.setEmail(email);
                profile.setDeviceId(deviceId); // Ensure device ID is set
                if (!TextUtils.isEmpty(phone)) {
                    profile.setPhoneNumber(phone);
                }

                profileRepo.upsert(profile).addOnCompleteListener(updateTask -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(CreateProfileActivity.this, "Profile saved!", Toast.LENGTH_SHORT).show();
                        navigateToEntrantHome();
                    } else {
                        Toast.makeText(CreateProfileActivity.this, 
                                "Failed to save profile: " + (updateTask.getException() != null 
                                        ? updateTask.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // Profile doesn't exist, create new one
                Profile profile = new Profile();
                profile.setUserId(uid);
                profile.setDeviceId(deviceId);
                profile.setName(name);
                profile.setEmail(email);
                if (!TextUtils.isEmpty(phone)) {
                    profile.setPhoneNumber(phone);
                }
                profile.setRole("entrant");
                profile.setActive(true);
                profile.setBanned(false);

                profileRepo.upsert(profile).addOnCompleteListener(createTask -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Save");
                    if (createTask.isSuccessful()) {
                        Toast.makeText(CreateProfileActivity.this, "Profile created!", Toast.LENGTH_SHORT).show();
                        navigateToEntrantHome();
                    } else {
                        Toast.makeText(CreateProfileActivity.this, 
                                "Failed to create profile: " + (createTask.getException() != null 
                                        ? createTask.getException().getMessage() : "Unknown error"), 
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private boolean validateInput(String name, String email, String phone) {
        boolean isValid = true;

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name is required");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // Phone is optional, but if provided, validate format
        if (!TextUtils.isEmpty(phone)) {
            // Basic phone validation (can be enhanced)
            if (phone.length() < 10) {
                phoneLayout.setError("Phone number is too short");
                isValid = false;
            } else {
                phoneLayout.setError(null);
            }
        } else {
            phoneLayout.setError(null);
        }

        return isValid;
    }

    private void navigateToEntrantHome() {
        Intent intent = new Intent(this, EntrantWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

