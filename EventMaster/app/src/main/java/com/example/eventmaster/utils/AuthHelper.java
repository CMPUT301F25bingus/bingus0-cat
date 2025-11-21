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
     */
    public static void signInAnonymously(@NonNull Context context,
                                         @NonNull OnAuthCompleteListener listener) {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            String deviceId = DeviceUtils.getDeviceId(context);

                            profileRepo.get(uid).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful() && profileTask.getResult() != null) {

                                    Profile existingProfile = profileTask.getResult();
                                    if (existingProfile.getDeviceId() == null ||
                                            existingProfile.getDeviceId().isEmpty()) {

                                        existingProfile.setDeviceId(deviceId);
                                        profileRepo.upsert(existingProfile)
                                                .addOnCompleteListener(updateTask ->
                                                        listener.onSuccess(user, existingProfile));
                                    } else {
                                        listener.onSuccess(user, existingProfile);
                                    }

                                } else {
                                    Profile profile = new Profile();
                                    profile.setUserId(uid);
                                    profile.setDeviceId(deviceId);
                                    profile.setRole("entrant");
                                    profile.setActive(true);
                                    profile.setBanned(false);

                                    profileRepo.upsert(profile)
                                            .addOnCompleteListener(createTask -> {
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
     * Signs up a new organizer with name, email, and password.
     */
    public static void signUpOrganizer(@NonNull String email,
                                       @NonNull String password,
                                       @NonNull String organizerName,
                                       @NonNull OnAuthCompleteListener listener) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();

                            Profile profile = new Profile();
                            profile.setUserId(uid);
                            profile.setEmail(email);
                            profile.setName(organizerName);   // ⭐ Name saved here
                            profile.setRole("organizer");
                            profile.setActive(true);
                            profile.setBanned(false);

                            profileRepo.upsert(profile)
                                    .addOnCompleteListener(createTask -> {
                                        if (createTask.isSuccessful()) {
                                            listener.onSuccess(user, profile);
                                        } else {
                                            listener.onError(createTask.getException());
                                        }
                                    });

                        } else {
                            listener.onError(new IllegalStateException("User is null after organizer sign-up"));
                        }

                    } else {
                        listener.onError(task.getException());
                    }
                });
    }


    /**
     * Signs in an existing organizer or admin.
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

                            profileRepo.get(uid).addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful() && profileTask.getResult() != null) {

                                    Profile profile = profileTask.getResult();

                                    if (expectedRole.equals(profile.getRole())) {
                                        listener.onSuccess(user, profile);
                                    } else {
                                        auth.signOut();
                                        listener.onError(new IllegalStateException("role mismatch"));
                                    }

                                } else {
                                    listener.onError(new IllegalStateException("Profile not found"));
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
     * Get currently authenticated user's profile.
     */
    public static void getCurrentUserProfile(@NonNull OnAuthCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            profileRepo.get(user.getUid()).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    listener.onSuccess(user, task.getResult());
                } else {
                    listener.onError(new IllegalStateException("Profile not found"));
                }
            });
        } else {
            listener.onError(new IllegalStateException("No user currently signed in"));
        }
    }

    /**
     * Sign out.
     */
    public static void signOut() {
        auth.signOut();
    }


    public interface OnAuthCompleteListener {
        void onSuccess(@NonNull FirebaseUser user, @NonNull Profile profile);
        void onError(@Nullable Exception error);
    }
}
