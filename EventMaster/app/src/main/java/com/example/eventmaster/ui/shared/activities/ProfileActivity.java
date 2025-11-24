package com.example.eventmaster.ui.shared.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
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
                .setMessage("This removes your profile from the system.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) ->
                        profileRepo.delete(
                                currentId,
                                v -> handleLogoutAndNavigateHome(),
                                err -> android.widget.Toast.makeText(this,
                                        "Delete failed: " + err.getMessage(),
                                        android.widget.Toast.LENGTH_LONG).show()))
                .show();
    }

    /**
     * Handles logout: signs out from Firebase Auth and navigates to role selection screen.
     */
    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out?")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, which) -> handleLogoutAndNavigateHome())
                .show();
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
