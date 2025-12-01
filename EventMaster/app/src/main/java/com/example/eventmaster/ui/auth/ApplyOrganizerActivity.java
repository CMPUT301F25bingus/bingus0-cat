package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.OrganizerApplicationRepositoryFs;
import com.example.eventmaster.model.OrganizerApplication;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;

/**
 * Activity for users to apply to become an organizer.
 * Collects name, email, password, and reason, then stores application in Firestore.
 */
public class ApplyOrganizerActivity extends AppCompatActivity {

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputLayout reasonLayout;
    
    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private TextInputEditText reasonEditText;
    
    private MaterialButton btnSubmit;
    private TextView textError;
    
    private OrganizerApplicationRepositoryFs repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_organizer);

        repository = new OrganizerApplicationRepositoryFs();
        
        // Set up toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
        
        // Initialize UI elements
        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        reasonLayout = findViewById(R.id.reasonLayout);
        
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        reasonEditText = findViewById(R.id.reasonEditText);
        
        btnSubmit = findViewById(R.id.btnSubmit);
        textError = findViewById(R.id.textError);

        btnSubmit.setOnClickListener(v -> handleSubmit());
    }

    /**
     * Handles form submission.
     */
    private void handleSubmit() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();
        String reason = reasonEditText.getText().toString().trim();

        // Validate input
        if (!validateInput(name, email, password, confirmPassword, reason)) {
            return;
        }

        // Clear previous errors
        hideError();

        // Disable button and show loading state
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Checking...");

        // Check for existing pending application
        repository.hasPendingApplication(email)
                .addOnSuccessListener(hasPending -> {
                    if (hasPending) {
                        runOnUiThread(() -> {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Submit Application");
                            showError("You already have a pending application with this email. Please wait for it to be reviewed.");
                        });
                    } else {
                        // No pending application, proceed with submission
                        proceedWithSubmission(name, email, password, reason);
                    }
                })
                .addOnFailureListener(error -> {
                    runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Application");
                        showError("Failed to check existing applications. Please try again.");
                        android.util.Log.e("ApplyOrganizer", "Failed to check for duplicates", error);
                    });
                });
    }

    /**
     * Proceeds with application submission after duplicate check passes.
     */
    private void proceedWithSubmission(String name, String email, String password, String reason) {
        btnSubmit.setText("Submitting...");

        // Encrypt password
        String encryptedPassword = CredentialStorageHelper.encryptPassword(this, password);
        if (encryptedPassword == null) {
            showError("Failed to encrypt password. Please try again.");
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Application");
            return;
        }

        // Create application object
        OrganizerApplication application = new OrganizerApplication(
                email,
                name,
                encryptedPassword,
                reason
        );
        application.setSubmittedAt(Timestamp.now());

        // Save to Firestore
        repository.createApplication(application)
                .addOnSuccessListener(applicationId -> {
                    runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Application");
                        Toast.makeText(this, "Application submitted successfully!", Toast.LENGTH_SHORT).show();
                        navigateToSuccessScreen();
                    });
                })
                .addOnFailureListener(error -> {
                    runOnUiThread(() -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit Application");
                        handleSubmitError(error);
                        android.util.Log.e("ApplyOrganizer", "Failed to submit application", error);
                    });
                });
    }

    /**
     * Validates all form inputs.
     */
    private boolean validateInput(String name, String email, String password, 
                                 String confirmPassword, String reason) {
        boolean isValid = true;

        // Validate name
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name is required");
            isValid = false;
        } else {
            nameLayout.setError(null);
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        // Validate reason
        if (TextUtils.isEmpty(reason)) {
            reasonLayout.setError("Please provide a reason");
            isValid = false;
        } else if (reason.length() < 10) {
            reasonLayout.setError("Please provide a more detailed reason (at least 10 characters)");
            isValid = false;
        } else {
            reasonLayout.setError(null);
        }

        return isValid;
    }

    /**
     * Handles submission errors.
     */
    private void handleSubmitError(Exception error) {
        String errorMessage = error != null ? error.getMessage() : "Failed to submit application";
        
        if (errorMessage.contains("already exists") || errorMessage.contains("duplicate")) {
            showError("An application with this email already exists.");
        } else if (errorMessage.contains("Network")) {
            showError("Connection error. Please check your internet and try again.");
        } else {
            showError("Error: " + errorMessage);
        }
    }

    /**
     * Shows an error message.
     */
    private void showError(String message) {
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the error message.
     */
    private void hideError() {
        textError.setVisibility(View.GONE);
        textError.setText("");
    }

    /**
     * Navigates to success screen.
     */
    private void navigateToSuccessScreen() {
        Intent intent = new Intent(this, ApplicationSubmittedActivity.class);
        intent.putExtra("applicantEmail", emailEditText.getText().toString().trim());
        startActivity(intent);
        finish();
    }
}

