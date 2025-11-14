package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.OrganizerHomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login and signup screen for organizers.
 * Allows switching between login and signup modes.
 */
public class OrganizerLoginActivity extends AppCompatActivity {
    private boolean isSignUpMode = false;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton primaryButton;
    private MaterialButton switchModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        primaryButton = findViewById(R.id.btnPrimary);
        switchModeButton = findViewById(R.id.btnSwitchMode);

        updateUI();

        primaryButton.setOnClickListener(v -> {
            if (isSignUpMode) {
                handleSignUp();
            } else {
                handleSignIn();
            }
        });

        switchModeButton.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            updateUI();
        });
    }

    private void updateUI() {
        if (isSignUpMode) {
            primaryButton.setText("Sign Up");
            switchModeButton.setText("Already have an account? Sign In");
        } else {
            primaryButton.setText("Sign In");
            switchModeButton.setText("Don't have an account? Sign Up");
        }
        // Clear fields
        emailEditText.setText("");
        passwordEditText.setText("");
        emailLayout.setError(null);
        passwordLayout.setError(null);
    }

    private void handleSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        primaryButton.setEnabled(false);
        primaryButton.setText("Creating account...");

        AuthHelper.signUpOrganizer(email, password, new AuthHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                primaryButton.setEnabled(true);
                primaryButton.setText("Sign Up");
                Toast.makeText(OrganizerLoginActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                navigateToOrganizerHome();
            }

            @Override
            public void onError(Exception error) {
                primaryButton.setEnabled(true);
                primaryButton.setText("Sign Up");
                
                String errorCode = null;
                String errorMessage = error != null ? error.getMessage() : "Failed to create account";
                
                // Get Firebase error code if available
                if (error instanceof FirebaseAuthException) {
                    errorCode = ((FirebaseAuthException) error).getErrorCode();
                }
                
                // Check error code first (more reliable)
                if (errorCode != null) {
                    if ("ERROR_EMAIL_ALREADY_IN_USE".equals(errorCode) || "email-already-in-use".equals(errorCode)) {
                        emailLayout.setError("This email is already registered");
                        return;
                    } else if ("ERROR_OPERATION_NOT_ALLOWED".equals(errorCode) || "operation-not-allowed".equals(errorCode)) {
                        Toast.makeText(OrganizerLoginActivity.this, 
                                "Email/Password authentication is not enabled!\n\n" +
                                "Please enable it in Firebase Console:\n" +
                                "1. Go to Authentication\n" +
                                "2. Click 'Sign-in method' tab\n" +
                                "3. Enable 'Email/Password'\n" +
                                "4. Click Save", 
                                Toast.LENGTH_LONG).show();
                        return;
                    } else if ("ERROR_WEAK_PASSWORD".equals(errorCode) || "weak-password".equals(errorCode)) {
                        passwordLayout.setError("Password is too weak");
                        return;
                    } else if ("ERROR_INVALID_EMAIL".equals(errorCode) || "invalid-email".equals(errorCode)) {
                        emailLayout.setError("Invalid email format");
                        return;
                    }
                }
                
                // Fallback to message checking
                if (errorMessage != null) {
                    if (errorMessage.contains("email-already-in-use") || errorMessage.contains("EMAIL_EXISTS")) {
                        emailLayout.setError("This email is already registered");
                    } else if (errorMessage.contains("operation-not-allowed") || errorMessage.contains("OPERATION_NOT_ALLOWED")) {
                        Toast.makeText(OrganizerLoginActivity.this, 
                                "Email/Password authentication is not enabled!\n\n" +
                                "Please enable it in Firebase Console:\n" +
                                "1. Go to Authentication\n" +
                                "2. Click 'Sign-in method' tab\n" +
                                "3. Enable 'Email/Password'\n" +
                                "4. Click Save", 
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OrganizerLoginActivity.this, 
                                "Error Code: " + (errorCode != null ? errorCode : "unknown") + "\n" +
                                "Error: " + errorMessage, 
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(OrganizerLoginActivity.this, "Failed to create account", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleSignIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        primaryButton.setEnabled(false);
        primaryButton.setText("Signing in...");

        AuthHelper.signInWithEmail(email, password, "organizer", new AuthHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                primaryButton.setEnabled(true);
                primaryButton.setText("Sign In");
                Toast.makeText(OrganizerLoginActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                navigateToOrganizerHome();
            }

            @Override
            public void onError(Exception error) {
                primaryButton.setEnabled(true);
                primaryButton.setText("Sign In");
                String errorMessage = error != null ? error.getMessage() : "Failed to sign in";
                if (errorMessage != null) {
                    if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
                        passwordLayout.setError("Incorrect password");
                    } else if (errorMessage.contains("user-not-found")) {
                        emailLayout.setError("No account found with this email");
                    } else if (errorMessage.contains("role mismatch")) {
                        Toast.makeText(OrganizerLoginActivity.this, "This account is not an organizer", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(OrganizerLoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

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

    private void navigateToOrganizerHome() {
        Intent intent = new Intent(this, OrganizerHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

