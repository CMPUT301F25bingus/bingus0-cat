package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.admin.activities.AdminWelcomeActivity;
import com.example.eventmaster.ui.auth.AdminLoginActivity;
import com.example.eventmaster.ui.auth.CreateProfileActivity;
import com.example.eventmaster.ui.auth.OrganizerLoginActivity;
import com.example.eventmaster.ui.entrant.EntrantHomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Role selection screen and authentication router.
 * 
 * Flow:
 * - Entrant → anonymous login → create profile → entrant home
 * - Organizer → signup/login → check role → organizer home
 * - Admin → login only → admin dashboard
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already authenticated and route accordingly
        checkAuthAndRoute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check authentication state when activity resumes
        checkAuthAndRoute();
    }

    private void checkAuthAndRoute() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // User is authenticated, check their role and route
            AuthHelper.getCurrentUserProfile(new AuthHelper.OnAuthCompleteListener() {
                @Override
                public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                    String role = profile.getRole();
                    Intent intent = null;
                    
                    if ("admin".equals(role)) {
                        intent = new Intent(MainActivity.this, AdminWelcomeActivity.class);
                    } else if ("organizer".equals(role)) {
                        intent = new Intent(MainActivity.this, com.example.eventmaster.ui.organizer.activities.OrganizerHomeActivity.class);
                    } else if ("entrant".equals(role)) {
                        // Check if profile is complete (has name and email)
                        if (profile.getName() != null && !profile.getName().isEmpty() 
                                && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                            intent = new Intent(MainActivity.this, EntrantHomeActivity.class);
                        } else {
                            // Profile incomplete, go to create profile
                            intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                        }
                    }
                    
                    if (intent != null) {
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Unknown role, show role selection
                        showRoleSelection();
                    }
                }

                @Override
                public void onError(Exception error) {
                    // Error getting profile, show role selection
                    showRoleSelection();
                }
            });
        } else {
            // No user authenticated, show role selection
            showRoleSelection();
        }
    }

    private void showRoleSelection() {
        setContentView(R.layout.temp_activity_main_roles);

        MaterialButton btnAdmin = findViewById(R.id.btnAdmin);
        MaterialButton btnOrganizer = findViewById(R.id.btnOrganizer);
        MaterialButton btnEntrant = findViewById(R.id.btnEntrant);

        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));

        btnOrganizer.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerLoginActivity.class)));

        btnEntrant.setOnClickListener(v -> {
            // Check for existing profile by device ID first
            MaterialButton btn = (MaterialButton) v;
            btn.setEnabled(false);
            btn.setText("Signing in...");
            
            String deviceId = DeviceUtils.getDeviceId(this);
            ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
            
            // First, try to find existing profile for this device
            profileRepo.getByDeviceId(deviceId).addOnCompleteListener(deviceProfileTask -> {
                if (deviceProfileTask.isSuccessful()) {
                    Profile existingProfile = deviceProfileTask.getResult();
                    if (existingProfile != null) {
                        // Found existing profile for this device
                        // Check if user is already signed in with this profile's userId
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null && existingProfile.getUserId().equals(auth.getCurrentUser().getUid())) {
                            // Already signed in with correct user, just route
                            routeEntrant(existingProfile);
                            btn.setEnabled(true);
                            btn.setText("Entrant Portal");
                        } else {
                            // Sign in anonymously and update profile userId if needed
                            AuthHelper.signInAnonymously(this, new AuthHelper.OnAuthCompleteListener() {
                                @Override
                                public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                                    btn.setEnabled(true);
                                    btn.setText("Entrant Portal");
                                    
                                    // Update profile userId if it changed (new anonymous user)
                                    if (!existingProfile.getUserId().equals(user.getUid())) {
                                        existingProfile.setUserId(user.getUid());
                                        profileRepo.upsert(existingProfile).addOnCompleteListener(updateTask -> {
                                            routeEntrant(existingProfile);
                                        });
                                    } else {
                                        routeEntrant(existingProfile);
                                    }
                                }

                                @Override
                                public void onError(Exception error) {
                                    btn.setEnabled(true);
                                    btn.setText("Entrant Portal");
                                    android.widget.Toast.makeText(MainActivity.this, 
                                            "Failed to sign in: " + (error != null ? error.getMessage() : "Unknown error"), 
                                            android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        // No existing profile for this device (this is normal for first-time users)
                        // Create new anonymous user
                        AuthHelper.signInAnonymously(this, new AuthHelper.OnAuthCompleteListener() {
                            @Override
                            public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                                btn.setEnabled(true);
                                btn.setText("Entrant Portal");
                                
                                // Check if profile is complete
                                if (profile.getName() != null && !profile.getName().isEmpty() 
                                        && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                                    // Profile complete, go to home
                                    Intent intent = new Intent(MainActivity.this, EntrantHomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Profile incomplete, go to create profile
                                    Intent intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            }

                            @Override
                            public void onError(Exception error) {
                                btn.setEnabled(true);
                                btn.setText("Entrant Portal");
                                android.widget.Toast.makeText(MainActivity.this, 
                                        "Failed to sign in: " + (error != null ? error.getMessage() : "Unknown error"), 
                                        android.widget.Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } else {
                    // Error getting profile by device ID, try normal anonymous login
                    AuthHelper.signInAnonymously(this, new AuthHelper.OnAuthCompleteListener() {
                        @Override
                        public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                            btn.setEnabled(true);
                            btn.setText("Entrant Portal");
                            
                            // Check if profile is complete
                            if (profile.getName() != null && !profile.getName().isEmpty() 
                                    && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                                // Profile complete, go to home
                                Intent intent = new Intent(MainActivity.this, EntrantHomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                // Profile incomplete, go to create profile
                                Intent intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }

                        @Override
                        public void onError(Exception error) {
                            btn.setEnabled(true);
                            btn.setText("Entrant Portal");
                            android.widget.Toast.makeText(MainActivity.this, 
                                    "Failed to sign in: " + (error != null ? error.getMessage() : "Unknown error"), 
                                    android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        });
    }
    
    private void routeEntrant(Profile profile) {
        Intent intent;
        // Check if profile is complete
        if (profile.getName() != null && !profile.getName().isEmpty() 
                && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
            // Profile complete, go to home
            intent = new Intent(this, EntrantHomeActivity.class);
        } else {
            // Profile incomplete, go to create profile
            intent = new Intent(this, CreateProfileActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
