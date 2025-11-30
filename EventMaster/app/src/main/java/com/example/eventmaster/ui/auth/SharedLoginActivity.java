package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.OrganizerApplicationRepositoryFs;
import com.example.eventmaster.model.OrganizerApplication;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.admin.activities.AdminWelcomeActivity;
import com.example.eventmaster.ui.organizer.activities.OrganizerHomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

/**
 * Shared login screen for both admin and organizer.
 * Signs in with email/password, then checks the user's role from their profile
 * and routes them to the appropriate home activity.
 */
public class SharedLoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private CheckBox checkRememberMe;
    private MaterialButton btnLogin;
    private MaterialButton btnApplyOrganizer;
    private MaterialButton btnCheckStatus;
    private TextView textError;
    
    private OrganizerApplicationRepositoryFs applicationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_login);

        applicationRepository = new OrganizerApplicationRepositoryFs();
        
        // Initialize UI elements
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        checkRememberMe = findViewById(R.id.checkRememberMe);
        btnLogin = findViewById(R.id.btnLogin);
        btnApplyOrganizer = findViewById(R.id.btnApplyOrganizer);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);
        textError = findViewById(R.id.textError);

        // Set up button click listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        btnApplyOrganizer.setOnClickListener(v -> handleApplyOrganizer());
        if (btnCheckStatus != null) {
            btnCheckStatus.setOnClickListener(v -> showStatusCheckDialog());
        }
        
        // Check if we should show status dialog immediately (from ApplicationSubmittedActivity)
        String checkStatusEmail = getIntent().getStringExtra("checkStatusEmail");
        if (checkStatusEmail != null && !checkStatusEmail.isEmpty()) {
            // Pre-fill email and show status dialog
            emailEditText.setText(checkStatusEmail);
            showStatusCheckDialog(checkStatusEmail);
        }
    }

    /**
     * Handles the login button click.
     * Signs in with email/password, checks role, and routes accordingly.
     */
    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (!validateInput(email, password)) {
            return;
        }

        // Clear previous errors
        hideError();

        // Disable button and show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");

        // Sign in and get role
        AuthHelper.signInWithEmailAndGetRole(email, password, new AuthHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(FirebaseUser user, Profile profile) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log in");

                    // Check role and route accordingly
                    String role = profile.getRole();
                    if (role == null || role.isEmpty()) {
                        showError("Invalid account type");
                        return;
                    }

                    // Save credentials if "Remember me" is checked
                    if (checkRememberMe.isChecked()) {
                        CredentialStorageHelper.saveCredentials(SharedLoginActivity.this, email, password);
                    }

                    // Route based on role
                    if ("admin".equals(role)) {
                        Toast.makeText(SharedLoginActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                        navigateToAdminHome();
                    } else if ("organizer".equals(role)) {
                        Toast.makeText(SharedLoginActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                        navigateToOrganizerHome();
                    } else {
                        // Entrants can't log in here - they use device ID
                        showError("Invalid account type. Please use 'Continue as Entrant' for entrant accounts.");
                    }
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Log in");
                    handleLoginError(error);
                    android.util.Log.e("SharedLogin", "Sign-in error: " + (error != null ? error.getMessage() : "Unknown"), error);
                });
            }
        });
    }

    /**
     * Handles the "Apply to be Organizer" button click.
     * Navigates to application form.
     */
    private void handleApplyOrganizer() {
        Intent intent = new Intent(this, ApplyOrganizerActivity.class);
        startActivity(intent);
    }
    
    /**
     * Shows dialog to check application status.
     * User enters email, then status is displayed.
     */
    private void showStatusCheckDialog() {
        showStatusCheckDialog(null);
    }
    
    /**
     * Shows dialog to check application status with pre-filled email.
     */
    private void showStatusCheckDialog(String prefillEmail) {
        // Create dialog with email input
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Check Application Status");
        
        // Create input field
        final TextInputEditText input = new TextInputEditText(this);
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Enter your email");
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            input.setText(prefillEmail);
        }
        
        // Wrap in TextInputLayout for better styling
        TextInputLayout inputLayout = new TextInputLayout(this, null, 
                com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        inputLayout.setHint("Email");
        inputLayout.addView(input);
        inputLayout.setPadding(
                (int) (48 * getResources().getDisplayMetrics().density),
                0,
                (int) (48 * getResources().getDisplayMetrics().density),
                0
        );
        
        builder.setView(inputLayout);
        builder.setPositiveButton("Check", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                checkApplicationStatus(email);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Checks application status for given email and shows result in dialog.
     */
    private void checkApplicationStatus(String email) {
        applicationRepository.getApplicationByEmail(email)
                .addOnSuccessListener(application -> {
                    if (application == null) {
                        showStatusResultDialog("No Application Found", 
                                "No application found with email: " + email);
                    } else {
                        String status = application.getStatus();
                        String message;
                        String title;
                        
                        if ("pending".equals(status)) {
                            title = "Application Pending";
                            message = "Your application is currently under review. " +
                                     "We'll notify you once a decision is made.";
                        } else if ("approved".equals(status)) {
                            title = "Application Approved!";
                            message = "Your application has been approved! " +
                                     "You can now log in as an organizer using your email and password.";
                        } else if ("rejected".equals(status)) {
                            title = "Application Rejected";
                            message = "Your application was not approved.";
                            if (application.getRejectionReason() != null && 
                                !application.getRejectionReason().isEmpty()) {
                                message += "\n\nReason: " + application.getRejectionReason();
                            }
                        } else {
                            title = "Application Status";
                            message = "Status: " + status;
                        }
                        
                        showStatusResultDialog(title, message);
                    }
                })
                .addOnFailureListener(error -> {
                    showStatusResultDialog("Error", 
                            "Failed to check application status: " + 
                            (error != null ? error.getMessage() : "Unknown error"));
                    android.util.Log.e("SharedLogin", "Failed to check application status", error);
                });
    }
    
    /**
     * Shows the application status result in a dialog.
     */
    private void showStatusResultDialog(String title, String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Validates email and password input.
     */
    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    /**
     * Handles login errors and displays appropriate error messages.
     */
    private void handleLoginError(Exception error) {
        String errorMessage = error != null ? error.getMessage() : "Failed to sign in";

        if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
            passwordLayout.setError("Incorrect password");
        } else if (errorMessage.contains("user-not-found")) {
            emailLayout.setError("No account found with this email");
        } else if (errorMessage.contains("operation-not-allowed") || errorMessage.contains("OPERATION_NOT_ALLOWED")) {
            showError("Email/Password authentication is not enabled. Please contact support.");
        } else if (errorMessage.contains("Profile not found")) {
            showError("Account not found. Please contact support.");
        } else if (errorMessage.contains("Network")) {
            showError("Connection error. Please check your internet.");
        } else {
            showError("Error: " + errorMessage);
        }
    }

    /**
     * Shows an error message in the error text view.
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
     * Navigates to Admin Welcome Activity.
     */
    private void navigateToAdminHome() {
        Intent intent = new Intent(this, AdminWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Navigates to Organizer Home Activity.
     */
    private void navigateToOrganizerHome() {
        Intent intent = new Intent(this, OrganizerHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

