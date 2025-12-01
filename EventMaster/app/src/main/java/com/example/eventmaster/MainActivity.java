package com.example.eventmaster;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.admin.activities.AdminWelcomeActivity;
import com.example.eventmaster.ui.auth.CreateProfileActivity;
import com.example.eventmaster.ui.auth.SharedLoginActivity;
import com.example.eventmaster.ui.entrant.activities.EntrantWelcomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.example.eventmaster.utils.DeviceUtils;
import com.example.eventmaster.utils.EntrantSessionPrefs;
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

        // 1) If the last session on this device was entrant, try to auto-route via deviceId
        if (EntrantSessionPrefs.wasLastEntrantActive(this)) {
            tryAutoRouteEntrantAndFallback();
            return;
        }

        // 2) Otherwise, use the existing auth + role routing logic
        startNormalRouting();
    }

    /**
     * Original startup logic, extracted from onCreate for reuse.
     * Checks Firebase auth and saved organizer/admin credentials.
     */
    private void startNormalRouting() {
        // First, check if user is already signed in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // User is already authenticated, get profile and route
            AuthHelper.getCurrentUserProfile(new AuthHelper.OnAuthCompleteListener() {
                @Override
                public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                    routeBasedOnRole(profile);
                }

                @Override
                public void onError(Exception error) {
                    // Error getting profile, try auto-login or show role selection
                    attemptAutoLogin();
                }
            });
        } else {
            // No user signed in, check for saved credentials for auto-login
            attemptAutoLogin();
        }
    }

    /**
     * Attempts to route directly to the entrant portal using deviceId.
     * If the entrant profile is missing or incomplete, falls back to normal routing.
     */
    private void tryAutoRouteEntrantAndFallback() {
        String deviceId = DeviceUtils.getDeviceId(this);
        ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

        profileRepo.getEntrantByDeviceIdDoc(deviceId).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                // Failed to load entrant profile → fallback
                startNormalRouting();
                return;
            }

            Profile profile = task.getResult();
            if (profile == null) {
                // No entrant profile yet → fallback (user will see role selection)
                startNormalRouting();
                return;
            }

            boolean hasName = profile.getName() != null && !profile.getName().isEmpty();
            boolean hasEmail = profile.getEmail() != null && !profile.getEmail().isEmpty();

            if (hasName && hasEmail) {
                // Entrant profile is complete → go straight to entrant welcome / portal
                routeEntrant(profile);
            } else {
                // Incomplete entrant profile → send them to profile creation
                Intent intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * Attempts to auto-login using saved "Remember me" credentials.
     * If no credentials are saved, shows role selection.
     */
    private void attemptAutoLogin() {
        if (CredentialStorageHelper.hasSavedCredentials(this)) {
            String savedEmail = CredentialStorageHelper.getSavedEmail(this);
            String savedPassword = CredentialStorageHelper.getSavedPassword(this);

            if (savedEmail != null && savedPassword != null) {
                // Attempt auto-login
                AuthHelper.signInWithEmailAndGetRole(savedEmail, savedPassword, new AuthHelper.OnAuthCompleteListener() {
                    @Override
                    public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                        routeBasedOnRole(profile);
                    }

                    @Override
                    public void onError(Exception error) {
                        // Auto-login failed, clear credentials and show role selection
                        CredentialStorageHelper.clearCredentials(MainActivity.this);
                        showRoleSelection();
                    }
                });
            } else {
                // Credentials exist but couldn't be retrieved, clear and show role selection
                CredentialStorageHelper.clearCredentials(this);
                showRoleSelection();
            }
        } else {
            // No saved credentials, show role selection
            showRoleSelection();
        }
    }

    /**
     * Routes user to appropriate activity based on their role.
     */
    private void routeBasedOnRole(com.example.eventmaster.model.Profile profile) {
        String role = profile.getRole();
        Intent intent = null;

        if ("admin".equals(role)) {
            intent = new Intent(MainActivity.this, AdminWelcomeActivity.class);
        } else if ("organizer".equals(role)) {
            intent = new Intent(MainActivity.this, com.example.eventmaster.ui.organizer.activities.OrganizerManageEventsActivity.class);
        } else if ("entrant".equals(role)) {
            // Entrants use device ID login, so clear any saved email/password credentials
            CredentialStorageHelper.clearCredentials(this);
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
                        intent = new Intent(MainActivity.this, com.example.eventmaster.ui.organizer.activities.OrganizerManageEventsActivity.class);
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

        com.google.android.material.card.MaterialCardView btnLogin = findViewById(R.id.btnLogin);
        com.google.android.material.card.MaterialCardView btnEntrant = findViewById(R.id.btnEntrant);

        // Log in button - navigates to shared login page
        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(this, SharedLoginActivity.class)));

        // Continue as Entrant button - uses device ID as the ONLY identity
        btnEntrant.setOnClickListener(v -> {
            com.google.android.material.card.MaterialCardView btn = (com.google.android.material.card.MaterialCardView) v;
            btn.setEnabled(false);
            btn.setClickable(false);
            btn.setAlpha(0.6f); // Visual feedback

            String deviceId = DeviceUtils.getDeviceId(this);
            ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

            // For entrants: profile doc ID = deviceId
            profileRepo.getEntrantByDeviceIdDoc(deviceId).addOnCompleteListener(task -> {
                if (isFinishing() || isDestroyed()) return;

                Profile existingProfile = null;
                if (task.isSuccessful()) {
                    existingProfile = task.getResult(); // may be null
                } else {
                    android.util.Log.w("MainActivity", "Failed to load entrant profile by deviceId: " +
                            (task.getException() != null ? task.getException().getMessage() : "unknown"));
                }

                if (existingProfile == null) {
                    // First time on this device → create a basic entrant profile
                    Profile p = new Profile();
                    p.setDeviceId(deviceId);
                    p.setRole("entrant");
                    p.setActive(true);
                    p.setBanned(false);

                    profileRepo.upsertEntrantByDeviceId(p).addOnCompleteListener(createTask -> {
                        runOnUiThread(() -> {
                            btn.setEnabled(true);
                            btn.setClickable(true);
                            btn.setAlpha(1.0f);
                        });

                        // New entrant must complete profile details
                        Intent intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Profile profile = existingProfile;

                    runOnUiThread(() -> {
                        btn.setEnabled(true);
                        btn.setClickable(true);
                        btn.setAlpha(1.0f);
                    });

                    // Decide where to go based on completeness
                    Intent intent;
                    if (profile.getName() != null && !profile.getName().isEmpty()
                            && profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                        intent = new Intent(MainActivity.this, EntrantWelcomeActivity.class);
                    } else {
                        intent = new Intent(MainActivity.this, CreateProfileActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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
