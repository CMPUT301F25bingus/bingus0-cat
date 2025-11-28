package com.example.eventmaster.ui.shared.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.NotificationService;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.NotificationServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProfileActivity
 * ----------------
 * Displays entrant profile info (name, email, phone, banned flag)
 * and shows event history based on registrations.
 *
 * Features:
 *  - Load user info from Firestore
 *  - Edit / delete profile
 *  - Show list of registered events (title + date)
 *  - Toolbar back arrow navigation to Event List
 */
public class ProfileActivity extends AppCompatActivity {

    // === Data repositories ===
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    private final EventRepository eventRepository = new EventRepositoryFs();
    private final NotificationService notificationService = new NotificationServiceFs();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    // Temporary fallback ID; replaced by FirebaseAuth UID if logged in
    private String currentId = "demoUser123";

    // === UI elements ===
    private TextView tvHeroName, tvHeroEmail, tvBanned;
    private TextInputLayout layoutName, layoutEmail, layoutPhone;
    private TextInputEditText inputName, inputEmail, inputPhone, inputDeviceId;
    private MaterialButton btnEdit, btnCancelEdit, btnDelete, btnLogout;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigationView;
    private Profile currentProfile;
    private boolean isEditing = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shared_activity_profile);

        // 🔹 Setup Toolbar with back arrow
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            // Make it act as the app bar
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
        }

        // 🔹 Use real UID if Firebase Auth user exists
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 🔹 Bind layout views
        tvHeroName = findViewById(R.id.tvHeroName);
        tvHeroEmail = findViewById(R.id.tvHeroEmail);
        tvBanned = findViewById(R.id.tvBanned);
        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPhone = findViewById(R.id.layoutPhone);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputDeviceId = findViewById(R.id.inputDeviceId);
        btnEdit  = findViewById(R.id.btnEdit);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnDelete= findViewById(R.id.btnDelete);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setFieldsEditable(false);
        if (inputDeviceId != null) {
            inputDeviceId.setEnabled(false);
            inputDeviceId.setFocusable(false);
        }

        btnEdit.setOnClickListener(v -> {
            if (isEditing) {
                saveProfileEdits();
            } else {
                enterEditMode();
            }
        });
        btnCancelEdit.setOnClickListener(v -> exitEditMode(true));

        // 🔹 Delete button → confirm, then delete profile
        btnDelete.setOnClickListener(v -> confirmDelete());

        // 🔹 Logout button → sign out and go to role selection
        btnLogout.setOnClickListener(v -> handleLogout());

        // Setup bottom navigation (for entrants)
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        // Show bottom nav for entrants (always show for now, can be made conditional later)
        bottomNavigationView.setVisibility(android.view.View.VISIBLE);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EventListActivity.class));
                return true;
            } else if (itemId == R.id.nav_history) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EntrantHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_alerts) {
                finish();
                startActivity(new Intent(this, com.example.eventmaster.ui.entrant.activities.EntrantNotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Already on Profile screen
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile every time user returns
        loadProfile();
    }

    // === PROFILE SECTION ===

    /**
     * Load entrant profile from Firestore using ProfileRepositoryFs.
     * Updates name, email, phone, and banned chip.
     */
    private void loadProfile() {
        profileRepo.get(currentId, p -> {
            currentProfile = p;
            if (isEditing) {
                exitEditMode(false);
            }
            applyProfile(p);
        }, e -> {
            if (tvHeroName != null) {
                tvHeroName.setText("Error");
            }
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Show confirmation dialog before permanently deleting profile.
     * If confirmed → deletes and closes activity.
     */
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete profile?")
                .setMessage("This removes your profile and all associated data from the system.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    // Get deviceId before deleting to also delete any profiles with same deviceId
                    String deviceId = currentProfile != null ? currentProfile.getDeviceId() : null;
                    
                    // First, delete all user-related data (registrations, waiting list, notifications)
                    deleteAllUserData(currentId, deviceId, () -> {
                        // Then delete the profile by userId
                        profileRepo.delete(
                                currentId,
                                v -> {
                                    // Also delete any other profiles with the same deviceId to prevent restoration
                                    if (deviceId != null && !deviceId.isEmpty()) {
                                        deleteAllProfilesByDeviceId(deviceId, () -> handleLogoutAndNavigateHome());
                                    } else {
                                        handleLogoutAndNavigateHome();
                                    }
                                },
                                err -> android.widget.Toast.makeText(this,
                                        "Delete failed: " + err.getMessage(),
                                        android.widget.Toast.LENGTH_LONG).show());
                    });
                })
                .show();
    }

    /**
     * Deletes all user-related data: registrations, waiting list entries, chosen list entries, and notifications.
     */
    private void deleteAllUserData(String userId, String deviceId, Runnable onComplete) {
        android.util.Log.d("ProfileActivity", "Starting deletion of all user data for userId: " + userId);
        
        // Step 1: Delete all notifications
        deleteAllNotifications(userId, deviceId, () -> {
            // Step 2: Delete all event-related data (registrations, waiting list, chosen list)
            deleteAllEventData(userId, onComplete);
        });
    }

    /**
     * Deletes all notifications for the user.
     */
    private void deleteAllNotifications(String userId, String deviceId, Runnable onComplete) {
        android.util.Log.d("ProfileActivity", "Deleting notifications for userId: " + userId);
        
        // Delete notifications by userId
        notificationService.deleteAllNotificationsForUser(
                userId,
                () -> {
                    android.util.Log.d("ProfileActivity", "Successfully deleted notifications for userId: " + userId);
                    // Also delete by deviceId if different
                    if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(userId)) {
                        notificationService.deleteAllNotificationsForUser(
                                deviceId,
                                () -> {
                                    android.util.Log.d("ProfileActivity", "Successfully deleted notifications for deviceId: " + deviceId);
                                    onComplete.run();
                                },
                                error -> {
                                    android.util.Log.w("ProfileActivity", "Failed to delete notifications by deviceId: " + error);
                                    // Continue even if this fails
                                    onComplete.run();
                                });
                    } else {
                        onComplete.run();
                    }
                },
                error -> {
                    android.util.Log.w("ProfileActivity", "Failed to delete notifications by userId: " + error);
                    // Continue even if this fails
                    onComplete.run();
                });
    }

    /**
     * Deletes all event-related data: registrations, waiting list entries, and chosen list entries.
     * Deletes by both userId and deviceId to ensure all data is removed.
     */
    private void deleteAllEventData(String userId, Runnable onComplete) {
        // Get deviceId from current profile
        String deviceId = currentProfile != null ? currentProfile.getDeviceId() : null;
        
        android.util.Log.d("ProfileActivity", "Deleting event data for userId: " + userId + ", deviceId: " + deviceId);
        
        // Get all events
        eventRepository.getAllEvents()
                .addOnSuccessListener(events -> {
                    if (events == null || events.isEmpty()) {
                        android.util.Log.d("ProfileActivity", "No events found, skipping event data deletion");
                        onComplete.run();
                        return;
                    }
                    
                    android.util.Log.d("ProfileActivity", "Found " + events.size() + " events, deleting user data from each");
                    
                    // Delete user data from each event
                    int totalEvents = events.size();
                    final int[] completedEvents = {0};
                    
                    if (totalEvents == 0) {
                        onComplete.run();
                        return;
                    }
                    
                    for (Event event : events) {
                        String eventId = event.getId();
                        if (eventId == null || eventId.isEmpty()) {
                            completedEvents[0]++;
                            if (completedEvents[0] >= totalEvents) {
                                android.util.Log.d("ProfileActivity", "Completed deleting event data");
                                onComplete.run();
                            }
                            continue;
                        }
                        
                        // Delete by both userId and deviceId (if different)
                        deleteEventUserData(eventId, userId, deviceId, () -> {
                            completedEvents[0]++;
                            if (completedEvents[0] >= totalEvents) {
                                android.util.Log.d("ProfileActivity", "Completed deleting event data from all events");
                                onComplete.run();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProfileActivity", "Failed to get events for deletion: " + e.getMessage());
                    // Continue even if this fails
                    onComplete.run();
                });
    }

    /**
     * Deletes user data from a specific event: registration, waiting list entry, and chosen list entry.
     * Deletes by both userId and deviceId to ensure all data is removed.
     */
    private void deleteEventUserData(String eventId, String userId, String deviceId, Runnable onComplete) {
        List<com.google.android.gms.tasks.Task<?>> deleteTasks = new ArrayList<>();
        
        // Delete by userId
        deleteTasks.add(db.collection("events").document(eventId)
                .collection("registrations").document(userId).delete());
        deleteTasks.add(db.collection("events").document(eventId)
                .collection("waiting_list").document(userId).delete());
        deleteTasks.add(db.collection("events").document(eventId)
                .collection("chosen_list").document(userId).delete());
        
        // Also delete by deviceId if it's different from userId
        if (deviceId != null && !deviceId.isEmpty() && !deviceId.equals(userId)) {
            deleteTasks.add(db.collection("events").document(eventId)
                    .collection("registrations").document(deviceId).delete());
            deleteTasks.add(db.collection("events").document(eventId)
                    .collection("waiting_list").document(deviceId).delete());
            deleteTasks.add(db.collection("events").document(eventId)
                    .collection("chosen_list").document(deviceId).delete());
        }
        
        // Wait for all deletions to complete
        com.google.android.gms.tasks.Tasks.whenAllComplete(deleteTasks)
                .addOnCompleteListener(task -> {
                    android.util.Log.d("ProfileActivity", "Completed deleting user data from event: " + eventId);
                    onComplete.run();
                });
    }

    /**
     * Deletes all profiles with the given deviceId to prevent profile restoration after deletion.
     * Uses a safety counter to prevent infinite recursion.
     */
    private void deleteAllProfilesByDeviceId(String deviceId, Runnable onComplete) {
        deleteAllProfilesByDeviceId(deviceId, onComplete, 0);
    }

    /**
     * Helper method with recursion safety counter.
     */
    private void deleteAllProfilesByDeviceId(String deviceId, Runnable onComplete, int iteration) {
        // Safety check: prevent infinite recursion (max 10 iterations)
        if (iteration >= 10) {
            android.util.Log.w("ProfileActivity", "Reached max iterations for deleting profiles by deviceId");
            onComplete.run();
            return;
        }

        // Query all profiles with this deviceId
        profileRepo.getByDeviceId(deviceId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Profile profile = task.getResult();
                        // Delete this profile (it might have a different userId)
                        profileRepo.delete(
                                profile.getUserId(),
                                v -> {
                                    // Recursively check for more profiles with the same deviceId
                                    deleteAllProfilesByDeviceId(deviceId, onComplete, iteration + 1);
                                },
                                err -> {
                                    // Continue even if deletion fails
                                    android.util.Log.w("ProfileActivity", "Failed to delete profile by deviceId: " + err.getMessage());
                                    deleteAllProfilesByDeviceId(deviceId, onComplete, iteration + 1);
                                });
                    } else {
                        // No more profiles with this deviceId, we're done
                        onComplete.run();
                    }
                });
    }

    /**
     * Handles logout: signs out from Firebase Auth and navigates to role selection screen.
     */
    private void handleLogout() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Log Out?")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, which) -> handleLogoutAndNavigateHome())
                .create();
        
        // Change button colors from purple to teal
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(ProfileActivity.this, R.color.teal_dark)
                );
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(ProfileActivity.this, R.color.teal_dark)
                );
            }
        });
        
        dialog.show();
    }

    /**
     * Centralized sign-out and navigation logic used by logout & delete flows.
     */
    private void handleLogoutAndNavigateHome() {
        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, com.example.eventmaster.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // === Utility ===

    /**
     * Returns safe text (fallback if null or blank).
     */
    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }

    private void applyProfile(Profile profile) {
        if (profile == null) return;
        String heroName = ns(profile.getName());
        String heroEmail = ns(profile.getEmail());
        String rawName = profile.getName() != null ? profile.getName() : "";
        String rawEmail = profile.getEmail() != null ? profile.getEmail() : "";
        String rawPhone = profile.getPhone() != null ? profile.getPhone() : "";
        String device = profile.getDeviceId();
        if (device == null || device.trim().isEmpty()) {
            device = currentId;
        }

        if (tvHeroName != null) tvHeroName.setText(heroName);
        if (tvHeroEmail != null) tvHeroEmail.setText(heroEmail);
        if (inputName != null) inputName.setText(rawName);
        if (inputEmail != null) inputEmail.setText(rawEmail);
        if (inputPhone != null) inputPhone.setText(rawPhone);
        if (inputDeviceId != null) inputDeviceId.setText(device);

        boolean banned = profile.getBanned();
        if (tvBanned != null) {
            tvBanned.setVisibility(banned ? View.VISIBLE : View.GONE);
            tvBanned.setText(banned ? "BANNED" : "");
        }
    }

    private void setFieldsEditable(boolean editable) {
        TextInputLayout[] layouts = {layoutName, layoutEmail, layoutPhone};
        TextInputEditText[] fields = {inputName, inputEmail, inputPhone};
        for (TextInputLayout layout : layouts) {
            if (layout != null) layout.setEnabled(editable);
        }
        for (TextInputEditText field : fields) {
            if (field == null) continue;
            field.setEnabled(editable);
            field.setFocusable(editable);
            field.setFocusableInTouchMode(editable);
            field.setCursorVisible(editable);
        }
    }

    private void enterEditMode() {
        isEditing = true;
        btnEdit.setText("Save");
        btnEdit.setIcon(null);
        if (btnCancelEdit != null) {
            btnCancelEdit.setVisibility(View.VISIBLE);
        }
        setFieldsEditable(true);
        if (inputName != null) {
            inputName.requestFocus();
            inputName.setSelection(inputName.getText() != null ? inputName.getText().length() : 0);
        }
    }

    private void exitEditMode(boolean resetFields) {
        isEditing = false;
        btnEdit.setText("Edit");
        btnEdit.setIconResource(R.drawable.ic_edit_24);
        if (btnCancelEdit != null) {
            btnCancelEdit.setVisibility(View.GONE);
        }
        setFieldsEditable(false);
        if (resetFields && currentProfile != null) {
            applyProfile(currentProfile);
        }
    }

    private void saveProfileEdits() {
        if (inputName == null || inputEmail == null || currentId == null) return;
        String name = textOrEmpty(inputName);
        String email = textOrEmpty(inputEmail);
        String phone = textOrEmpty(inputPhone);

        if (name.isEmpty()) {
            inputName.setError("Required");
            inputName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            inputEmail.setError("Required");
            inputEmail.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phoneNumber", phone);

        btnEdit.setEnabled(false);
        profileRepo.update(currentId, updates)
                .addOnSuccessListener(v -> {
                    btnEdit.setEnabled(true);
                    if (currentProfile == null) {
                        currentProfile = new Profile();
                        currentProfile.setUserId(currentId);
                    }
                    currentProfile.setName(name);
                    currentProfile.setEmail(email);
                    currentProfile.setPhone(phone);
                    applyProfile(currentProfile);
                    exitEditMode(false);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(err -> {
                    btnEdit.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String textOrEmpty(TextInputEditText editText) {
        return editText != null && editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }
}
