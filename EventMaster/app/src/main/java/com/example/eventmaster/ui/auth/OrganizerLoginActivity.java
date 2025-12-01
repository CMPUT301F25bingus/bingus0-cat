package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.ui.organizer.activities.OrganizerManageEventsActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login and signup screen for organizers.
 */
public class OrganizerLoginActivity extends AppCompatActivity {

    private boolean isSignUpMode = false;

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout nameLayout;

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText nameEditText;

    private MaterialButton primaryButton;
    private MaterialButton switchModeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        nameLayout = findViewById(R.id.nameLayout);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        nameEditText = findViewById(R.id.nameEditText);

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
            nameLayout.setVisibility(View.VISIBLE);
        } else {
            primaryButton.setText("Sign In");
            switchModeButton.setText("Don't have an account? Sign Up");
            nameLayout.setVisibility(View.GONE);
        }

        emailEditText.setText("");
        passwordEditText.setText("");
        if (nameEditText != null) nameEditText.setText("");

        emailLayout.setError(null);
        passwordLayout.setError(null);
        nameLayout.setError(null);
    }


    // ---------------------------------------------------------------------
    // ORGANIZER SIGN-UP
    // ---------------------------------------------------------------------
    private void handleSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String organizerName = nameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(organizerName)) {
            nameLayout.setError("Name is required");
            return;
        }

        if (!validateInput(email, password)) {
            return;
        }

        primaryButton.setEnabled(false);
        primaryButton.setText("Creating account...");

        // Updated AuthHelper call that includes the organizer name
        AuthHelper.signUpOrganizer(email, password, organizerName,
                new AuthHelper.OnAuthCompleteListener() {
                    @Override
                    public void onSuccess(FirebaseUser user, Profile profile) {
                        runOnUiThread(() -> {
                            primaryButton.setEnabled(true);
                            primaryButton.setText("Sign Up");
                            Toast.makeText(OrganizerLoginActivity.this,
                                    "Account created successfully!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToOrganizerHome();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            primaryButton.setEnabled(true);
                            primaryButton.setText("Sign Up");
                            handleSignUpError(error);
                            android.util.Log.e("OrganizerLogin", "Sign-up error: " + (error != null ? error.getMessage() : "Unknown"), error);
                        });
                    }
                });
    }


    // ---------------------------------------------------------------------
    // ORGANIZER SIGN-IN
    // ---------------------------------------------------------------------
    private void handleSignIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        primaryButton.setEnabled(false);
        primaryButton.setText("Signing in...");

        AuthHelper.signInWithEmail(email, password, "organizer",
                new AuthHelper.OnAuthCompleteListener() {
                    @Override
                    public void onSuccess(FirebaseUser user, Profile profile) {
                        runOnUiThread(() -> {
                            primaryButton.setEnabled(true);
                            primaryButton.setText("Sign In");
                            Toast.makeText(OrganizerLoginActivity.this,
                                    "Signed in successfully!",
                                    Toast.LENGTH_SHORT).show();
                            navigateToOrganizerHome();
                        });
                    }

                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            primaryButton.setEnabled(true);
                            primaryButton.setText("Sign In");
                            handleSignInError(error);
                            android.util.Log.e("OrganizerLogin", "Sign-in error: " + (error != null ? error.getMessage() : "Unknown"), error);
                        });
                    }
                });
    }


    // ---------------------------------------------------------------------
    // VALIDATION + ERROR HANDLING
    // ---------------------------------------------------------------------
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


    private void handleSignUpError(Exception error) {
        String errorMessage = error != null ? error.getMessage() : "Failed to create account";

        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();

            if ("ERROR_EMAIL_ALREADY_IN_USE".equals(code)) {
                emailLayout.setError("This email is already registered");
                return;
            }
            if ("ERROR_WEAK_PASSWORD".equals(code)) {
                passwordLayout.setError("Password is too weak");
                return;
            }
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                emailLayout.setError("Invalid email format");
                return;
            }
        }

        Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
    }


    private void handleSignInError(Exception error) {
        String errorMessage = error != null ? error.getMessage() : "Failed to sign in";

        if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
            passwordLayout.setError("Incorrect password");
        } else if (errorMessage.contains("user-not-found")) {
            emailLayout.setError("No account found with this email");
        } else if (errorMessage.contains("role mismatch")) {
            Toast.makeText(this, "This account is not an organizer", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }


    // ---------------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------------
    private void navigateToOrganizerHome() {
        Intent intent = new Intent(OrganizerLoginActivity.this, OrganizerManageEventsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
