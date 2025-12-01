package com.example.eventmaster.ui.shared.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.eventmaster.MainActivity;
import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Edit Profile screen: lets a user update name, email, phone with a nice design similar to ProfileActivity.
 * Uses ProfileRepositoryFs (Firestore) to load and upsert the profile.
 * Includes logout functionality.
 */
public class EditProfileActivity extends AppCompatActivity {
    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private String profileId;

    // UI elements
    private TextView tvHeroName, tvHeroEmail;
    private ImageView imgAvatar;
    private TextInputLayout layoutName, layoutEmail, layoutPhone;
    private TextInputEditText inputName, inputEmail, inputPhone, inputDeviceId;
    private MaterialButton btnEdit, btnCancelEdit, btnDelete, btnLogout;
    private ImageButton btnAddPicture;
    private boolean isEditing = false;
    private Profile currentProfile;
    private Uri profilePictureUri = null;

    // Image picker launcher
    private final ActivityResultLauncher<String> pickImage =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    profilePictureUri = uri;
                    // Clear any tint before loading image
                    if (imgAvatar != null) {
                        imgAvatar.setColorFilter(null);
                        // Show preview immediately with Glide (circular)
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .into(imgAvatar);
                    }
                    // Upload the image
                    uploadProfilePicture(uri);
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    pickImage.launch("image/*");
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shared_activity_profile);

        profileId = getIntent().getStringExtra("profileId");
        if (profileId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            profileId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Setup back button
        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Bind views
        tvHeroName = findViewById(R.id.tvHeroName);
        tvHeroEmail = findViewById(R.id.tvHeroEmail);
        imgAvatar = findViewById(R.id.imgAvatar);
        layoutName = findViewById(R.id.layoutName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPhone = findViewById(R.id.layoutPhone);
        inputName = findViewById(R.id.inputName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPhone = findViewById(R.id.inputPhone);
        inputDeviceId = findViewById(R.id.inputDeviceId);
        btnEdit = findViewById(R.id.btnEdit);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnLogout = findViewById(R.id.btnLogout);
        btnAddPicture = findViewById(R.id.btnAddPicture);

        // Hide bottom navigation for organizers
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }

        // Hide device ID field for organizers (organizers don't have device IDs)
        TextInputLayout layoutDeviceId = findViewById(R.id.layoutDeviceId);
        if (layoutDeviceId != null) {
            layoutDeviceId.setVisibility(View.GONE);
        }

        setFieldsEditable(false);

        // Load current profile data
        if (profileId != null) {
            repo.get(profileId, p -> {
                currentProfile = p;
                if (tvHeroName != null) tvHeroName.setText(p.getName() != null ? p.getName() : "User");
                if (tvHeroEmail != null) tvHeroEmail.setText(p.getEmail() != null ? p.getEmail() : "");
                if (inputName != null) inputName.setText(p.getName());
                if (inputEmail != null) inputEmail.setText(p.getEmail());
                if (inputPhone != null) inputPhone.setText(p.getPhone());
                // Device ID field is hidden for organizers, so we don't need to set it
                
                // Load profile picture if available
                if (imgAvatar != null) {
                    String imageUrl = p.getProfileImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        // Clear any tint before loading real image
                        imgAvatar.setColorFilter(null);
                        Glide.with(this)
                                .load(imageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .error(R.drawable.ic_avatar_placeholder)
                                .into(imgAvatar);
                    } else {
                        // Use placeholder with tint
                        imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                        imgAvatar.setColorFilter(android.graphics.Color.parseColor("#15837C"));
                    }
                }
            }, e -> {
                Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }

        // Edit button
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                if (isEditing) {
                    saveProfile();
                } else {
                    enterEditMode();
                }
            });
        }

        // Cancel button
        if (btnCancelEdit != null) {
            btnCancelEdit.setOnClickListener(v -> exitEditMode(true));
        }

        // Delete button
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }

        // Logout button
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }

        // Add picture button
        if (btnAddPicture != null) {
            btnAddPicture.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
                } else {
                    requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            });
        }

        // Also make avatar clickable
        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> {
                if (btnAddPicture != null) {
                    btnAddPicture.performClick();
                }
            });
        }
    }

    /**
     * Uploads profile picture to Firebase Storage and updates profile document.
     */
    private void uploadProfilePicture(Uri uri) {
        if (profileId == null || uri == null) {
            Toast.makeText(this, "Cannot upload picture", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading picture...", Toast.LENGTH_SHORT).show();

        try {
            byte[] bytes = readAllBytes(uri);
            StorageReference ref = FirebaseStorage.getInstance()
                    .getReference("profiles/" + profileId + "/profile.jpg");

            ref.putBytes(bytes)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUrl -> {
                        String imageUrl = downloadUrl.toString();
                        // Update profile with image URL
                        if (currentProfile != null) {
                            currentProfile.setProfileImageUrl(imageUrl);
                        }
                        // Update in Firestore
                        FirebaseFirestore.getInstance()
                                .collection("profiles")
                                .document(profileId)
                                .update("profileImageUrl", imageUrl)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                                    // Update currentProfile object
                                    if (currentProfile != null) {
                                        currentProfile.setProfileImageUrl(imageUrl);
                                    }
                                    // Clear any tint before loading real image
                                    if (imgAvatar != null) {
                                        imgAvatar.setColorFilter(null);
                                        // Load image with Glide
                                        Glide.with(this)
                                                .load(imageUrl)
                                                .circleCrop()
                                                .placeholder(R.drawable.ic_avatar_placeholder)
                                                .error(R.drawable.ic_avatar_placeholder)
                                                .into(imgAvatar);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save URL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Reads all bytes from a URI.
     */
    private byte[] readAllBytes(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream for URI: " + uri);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private void setFieldsEditable(boolean editable) {
        if (inputName != null) inputName.setEnabled(editable);
        if (inputEmail != null) inputEmail.setEnabled(editable);
        if (inputPhone != null) inputPhone.setEnabled(editable);
    }

    private void enterEditMode() {
        isEditing = true;
        setFieldsEditable(true);
        if (btnEdit != null) {
            btnEdit.setText("Save");
            btnEdit.setIcon(null);
        }
        if (btnCancelEdit != null) btnCancelEdit.setVisibility(View.VISIBLE);
    }

    private void exitEditMode(boolean resetValues) {
        isEditing = false;
        setFieldsEditable(false);
        if (btnEdit != null) {
            btnEdit.setText("Edit");
            btnEdit.setIconResource(R.drawable.ic_edit_24);
        }
        if (btnCancelEdit != null) btnCancelEdit.setVisibility(View.GONE);

        if (resetValues && currentProfile != null) {
            if (inputName != null) inputName.setText(currentProfile.getName());
            if (inputEmail != null) inputEmail.setText(currentProfile.getEmail());
            if (inputPhone != null) inputPhone.setText(currentProfile.getPhone());
        }
    }

    private void saveProfile() {
        if (profileId == null) {
            Toast.makeText(this, "Profile ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = inputName != null ? inputName.getText().toString().trim() : "";
        String email = inputEmail != null ? inputEmail.getText().toString().trim() : "";
        String phone = inputPhone != null ? inputPhone.getText().toString().trim() : "";

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Profile p = new Profile(profileId, name, email, phone);
        if (currentProfile != null) {
            p.setNotificationsEnabled(currentProfile.isNotificationsEnabled());
            // Preserve profile image URL
            if (currentProfile.getProfileImageUrl() != null) {
                p.setProfileImageUrl(currentProfile.getProfileImageUrl());
            }
        }

        repo.upsert(p)
                .addOnSuccessListener(x -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    currentProfile = p;
                    if (tvHeroName != null) tvHeroName.setText(name);
                    if (tvHeroEmail != null) tvHeroEmail.setText(email);
                    exitEditMode(false);
                    finish();
                })
                .addOnFailureListener(err -> {
                    Toast.makeText(this, 
                            "Failed to save: " + err.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void confirmDelete() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Delete Profile?")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) -> {
                    if (profileId != null) {
                        repo.delete(profileId,
                                x -> {
                                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                                    handleLogoutAndNavigateHome();
                                },
                                err -> {
                                    Toast.makeText(this, 
                                            "Failed to delete: " + err.getMessage(), 
                                            Toast.LENGTH_LONG).show();
                                });
                    }
                })
                .create();
        
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(EditProfileActivity.this, R.color.teal_dark)
                );
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(EditProfileActivity.this, R.color.teal_dark)
                );
            }
        });
        
        dialog.show();
    }

    private void handleLogout() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Log Out?")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log Out", (d, which) -> handleLogoutAndNavigateHome())
                .create();
        
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    ContextCompat.getColor(EditProfileActivity.this, R.color.teal_dark)
                );
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(EditProfileActivity.this, R.color.teal_dark)
                );
            }
        });
        
        dialog.show();
    }

    private void handleLogoutAndNavigateHome() {
        AuthHelper.signOut();
        CredentialStorageHelper.clearCredentials(this);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
