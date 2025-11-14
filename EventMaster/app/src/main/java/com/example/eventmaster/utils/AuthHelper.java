package com.example.eventmaster.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Helper class for authentication operations.
 * Handles anonymous login, email/password authentication, and role verification.
 */
public class AuthHelper {
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();

    /**
     * Performs anonymous authentication for entrants.
     * After successful login, creates a profile in Firestore if it doesn't exist.
     * Saves the device ID to the profile for device-based identification.
     * 
     * @param context Application context (needed to get device ID)
     * @param listener Callback for authentication result
     */
    public static void signInAnonymously(@NonNull Context context, @NonNull OnAuthCompleteListener listener) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            String deviceId = DeviceUtils.getDeviceId(context);
                            
                            // Check if profile exists, if not create one
                            profileRepo.get(uid).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                                    // Profile exists - update device ID if not set
                                    Profile existingProfile = profileTask.getResult();
                                    if (existingProfile.getDeviceId() == null || existingProfile.getDeviceId().isEmpty()) {
                                        existingProfile.setDeviceId(deviceId);
                                        profileRepo.upsert(existingProfile).addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                listener.onSuccess(user, existingProfile);
                                            } else {
                                                // Continue anyway even if device ID update fails
                                                listener.onSuccess(user, existingProfile);
                                            }
                                        });
                                    } else {
                                        listener.onSuccess(user, existingProfile);
                                    }
                                } else {
                                    // Profile doesn't exist or error occurred, create it
                                    Profile profile = new Profile();
                                    profile.setUserId(uid);
                                    profile.setDeviceId(deviceId);
                                    profile.setRole("entrant");
                                    profile.setActive(true);
                                    profile.setBanned(false);
                                    
                                    profileRepo.upsert(profile).addOnCompleteListener(createTask -> {
                                        if (createTask.isSuccessful()) {
                                            listener.onSuccess(user, profile);
                                        } else {
                                            listener.onError(createTask.getException());
                                        }
                                    });
                                }
                            });
                        } else {
                            listener.onError(new IllegalStateException("User is null after anonymous sign-in"));
                        }
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Signs up a new organizer with email and password.
     * Creates a profile with "organizer" role in Firestore.
     */
    public static void signUpOrganizer(@NonNull String email, 
                                       @NonNull String password,
                                       @NonNull OnAuthCompleteListener listener) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            // Create organizer profile
                            Profile profile = new Profile();
                            profile.setUserId(uid);
                            profile.setEmail(email);
                            profile.setRole("organizer");
                            profile.setActive(true);
                            profile.setBanned(false);
                            
                            profileRepo.upsert(profile).addOnCompleteListener(createTask -> {
                                if (createTask.isSuccessful()) {
                                    listener.onSuccess(user, profile);
                                } else {
                                    listener.onError(createTask.getException());
                                }
                            });
                        } else {
                            listener.onError(new IllegalStateException("User is null after sign-up"));
                        }
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Signs in an existing organizer or admin with email and password.
     * Verifies the role in Firestore after successful authentication.
     */
    public static void signInWithEmail(@NonNull String email,
                                       @NonNull String password,
                                       @NonNull String expectedRole,
                                       @NonNull OnAuthCompleteListener listener) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            // Verify role in Firestore
                            profileRepo.get(uid).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful() && profileTask.getResult() != null) {
                                    Profile profile = profileTask.getResult();
                                    String actualRole = profile.getRole();
                                    
                                    if (expectedRole.equals(actualRole)) {
                                        listener.onSuccess(user, profile);
                                    } else {
                                        // Role mismatch - sign out
                                        auth.signOut();
                                        listener.onError(new IllegalStateException(
                                                "User role mismatch. Expected: " + expectedRole + ", Found: " + actualRole));
                                    }
                                } else {
                                    // Profile doesn't exist - provide detailed error
                                    String errorMsg = "Profile not found for user with UID: " + uid;
                                    if (profileTask.getException() != null) {
                                        errorMsg += "\nError: " + profileTask.getException().getMessage();
                                    }
                                    errorMsg += "\n\nPlease verify:\n";
                                    errorMsg += "1. Document ID in Firestore 'profiles' collection = " + uid + "\n";
                                    errorMsg += "2. Document exists and has 'role' field = 'admin'";
                                    auth.signOut();
                                    listener.onError(new IllegalStateException(errorMsg));
                                }
                            });
                        } else {
                            listener.onError(new IllegalStateException("User is null after sign-in"));
                        }
                    } else {
                        listener.onError(task.getException());
                    }
                });
    }

    /**
     * Checks if user is currently authenticated and returns their profile.
     */
    public static void getCurrentUserProfile(@NonNull OnAuthCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            profileRepo.get(uid).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    listener.onSuccess(user, task.getResult());
                } else {
                    listener.onError(task.getException() != null 
                            ? task.getException() 
                            : new IllegalStateException("Profile not found"));
                }
            });
        } else {
            listener.onError(new IllegalStateException("No user currently signed in"));
        }
    }

    /**
     * Signs out the current user.
     */
    public static void signOut() {
        auth.signOut();
    }

    /**
     * Callback interface for authentication operations.
     */
    public interface OnAuthCompleteListener {
        void onSuccess(@NonNull FirebaseUser user, @NonNull Profile profile);
        void onError(@Nullable Exception error);
    }
}

