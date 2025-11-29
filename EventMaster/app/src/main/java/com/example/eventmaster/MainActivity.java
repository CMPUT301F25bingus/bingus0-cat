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
import com.example.eventmaster.ui.entrant.activities.EntrantWelcomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Role selection screen and authentication router.
 *
 * Flow:
 * - Landing page → Role selection (always shown first)
 * - Entrant → anonymous login → create profile → entrant home
 * - Organizer → signup/login → check role → organizer home
 * - Admin → login only → admin dashboard
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Always show role selection screen first, regardless of authentication status
        // Users must explicitly choose their role (Entrant/Organizer/Admin)
        showRoleSelection();
    }

    @Override
    protected void onStart() {
        // Don't auto-route on resume - keep showing role selection
        super.onStart();
        // Role selection screen is already shown, no need to check auth
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
                            intent = new Intent(MainActivity.this, EntrantWelcomeActivity.class);
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

        com.google.android.material.card.MaterialCardView btnAdmin = findViewById(R.id.btnAdmin);
        com.google.android.material.card.MaterialCardView btnOrganizer = findViewById(R.id.btnOrganizer);
        com.google.android.material.card.MaterialCardView btnEntrant = findViewById(R.id.btnEntrant);

        btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));

        btnOrganizer.setOnClickListener(v ->
                startActivity(new Intent(this, OrganizerLoginActivity.class)));

        btnEntrant.setOnClickListener(v -> {
            com.google.android.material.card.MaterialCardView btn = (com.google.android.material.card.MaterialCardView) v;
            btn.setEnabled(false);
            btn.setClickable(false);
            btn.setAlpha(0.6f); // Visual feedback

            String deviceId = DeviceUtils.getDeviceId(this);
            ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

            // First, check for existing profile by device ID
            profileRepo.getByDeviceId(deviceId).addOnCompleteListener(deviceProfileTask -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                try {
                    if (deviceProfileTask.isSuccessful()) {
                        Profile existingProfile = deviceProfileTask.getResult();
                        
                        if (existingProfile != null) {
                            // Found existing profile for this device
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            
                            // Check if user is already signed in with this profile's userId
                            if (auth.getCurrentUser() != null && existingProfile.getUserId() != null
                                    && existingProfile.getUserId().equals(auth.getCurrentUser().getUid())) {
                                // Already signed in with correct user, just route
                                runOnUiThread(() -> {
                                    btn.setEnabled(true);
                                    btn.setClickable(true);
                                    btn.setAlpha(1.0f);
                                });
                                routeEntrant(existingProfile);
                            } else {
                                // Sign in anonymously and update profile userId if needed
                                // We already have the existing profile, so we'll update it with the new userId
                                FirebaseAuth.getInstance().signInAnonymously()
                                        .addOnCompleteListener(task -> {
                                            if (isFinishing() || isDestroyed()) {
                                                return;
                                            }

                                            if (task.isSuccessful()) {
                                                com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                                if (user != null && user.getUid() != null
                                                        && !user.getUid().equals(existingProfile.getUserId())) {
                                                    // Update existing profile with new anonymous userId
                                                    existingProfile.setUserId(user.getUid());
                                                    profileRepo.upsert(existingProfile).addOnCompleteListener(updateTask -> {
                                                        runOnUiThread(() -> {
                                                            btn.setEnabled(true);
                                                            btn.setClickable(true);
                                                            btn.setAlpha(1.0f);
                                                        });

                                                        if (updateTask.isSuccessful()) {
                                                            routeEntrant(existingProfile);
                                                        } else {
                                                            // Even if update fails, route with existing profile
                                                            android.util.Log.w("MainActivity", "Failed to update profile userId, routing anyway: " + updateTask.getException());
                                                            routeEntrant(existingProfile);
                                                        }
                                                    });
                                                } else {
                                                    // User ID already matches or user is null
                                                    runOnUiThread(() -> {
                                                        btn.setEnabled(true);
                                                        btn.setClickable(true);
                                                        btn.setAlpha(1.0f);
                                                    });
                                                    routeEntrant(existingProfile);
                                                }
                                            } else {
                                                // Sign in failed
                                                runOnUiThread(() -> {
                                                    btn.setEnabled(true);
                                                    btn.setClickable(true);
                                                    btn.setAlpha(1.0f);
                                                    android.widget.Toast.makeText(MainActivity.this,
                                                            "Failed to sign in: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"),
                                                            android.widget.Toast.LENGTH_LONG).show();
                                                });
                                                android.util.Log.e("MainActivity", "Error signing in anonymously: " + (task.getException() != null ? task.getException().getMessage() : "Unknown"), task.getException());
                                            }
                                        });
                            }
                        } else {
                            // No existing profile for this device - first-time user
                            // Sign in anonymously and create new profile
                            AuthHelper.signInAnonymously(MainActivity.this, new AuthHelper.OnAuthCompleteListener() {
                                @Override
                                public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    runOnUiThread(() -> {
                                        btn.setEnabled(true);
                                        btn.setClickable(true);
                                        btn.setAlpha(1.0f);

                                        // Navigate based on profile completeness
                                        Intent intent;
                                        if (profile != null && profile.getName() != null && !profile.getName().isEmpty()
                                                && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                                            // Profile complete, go to welcome screen
                                            intent = new Intent(MainActivity.this, EntrantWelcomeActivity.class);
                                        } else {
                                            // Profile incomplete, go to create profile
                                            intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                                        }
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                                }

                                @Override
                                public void onError(Exception error) {
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }

                                    runOnUiThread(() -> {
                                        btn.setEnabled(true);
                                        btn.setClickable(true);
                                        btn.setAlpha(1.0f);
                                        android.widget.Toast.makeText(MainActivity.this,
                                                "Failed to sign in: " + (error != null ? error.getMessage() : "Unknown error"),
                                                android.widget.Toast.LENGTH_LONG).show();
                                    });

                                    android.util.Log.e("MainActivity", "Error signing in anonymously: " + (error != null ? error.getMessage() : "Unknown"), error);
                                }
                            });
                        }
                    } else {
                        // Error getting profile by device ID, try normal anonymous login as fallback
                        android.util.Log.w("MainActivity", "Error getting profile by device ID, falling back to anonymous login");
                        AuthHelper.signInAnonymously(MainActivity.this, new AuthHelper.OnAuthCompleteListener() {
                            @Override
                            public void onSuccess(com.google.firebase.auth.FirebaseUser user, Profile profile) {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                runOnUiThread(() -> {
                                    btn.setEnabled(true);
                                    btn.setClickable(true);
                                    btn.setAlpha(1.0f);

                                    // Navigate based on profile completeness
                                    Intent intent;
                                    if (profile != null && profile.getName() != null && !profile.getName().isEmpty()
                                            && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                                        intent = new Intent(MainActivity.this, EntrantWelcomeActivity.class);
                                    } else {
                                        intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                                    }
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }

                            @Override
                            public void onError(Exception error) {
                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                runOnUiThread(() -> {
                                    btn.setEnabled(true);
                                    btn.setClickable(true);
                                    btn.setAlpha(1.0f);
                                    android.widget.Toast.makeText(MainActivity.this,
                                            "Failed to sign in: " + (error != null ? error.getMessage() : "Unknown error"),
                                            android.widget.Toast.LENGTH_LONG).show();
                                });

                                android.util.Log.e("MainActivity", "Error signing in anonymously: " + (error != null ? error.getMessage() : "Unknown"), error);
                            }
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error processing device profile: " + e.getMessage(), e);
                    if (!isFinishing() && !isDestroyed()) {
                        runOnUiThread(() -> {
                            btn.setEnabled(true);
                            btn.setClickable(true);
                            btn.setAlpha(1.0f);
                        });
                    }
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
            intent = new Intent(this, EntrantWelcomeActivity.class);
        } else {
            // Profile incomplete, go to create profile
            intent = new Intent(this, CreateProfileActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
