package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.activities.AdminWelcomeActivity;
import com.example.eventmaster.utils.AuthHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Login screen for admins.
 * Only allows login (no signup) since admin accounts are manually created.
 */
public class AdminLoginActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.btnLogin);

        loginButton.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        AuthHelper.signInWithEmail(email, password, "admin", new AuthHelper.OnAuthCompleteListener() {
            @Override
            public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                    Toast.makeText(AdminLoginActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                    navigateToAdminHome();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");
                    String errorMessage = error != null ? error.getMessage() : "Failed to sign in";
                    android.util.Log.e("AdminLogin", "Sign-in error: " + errorMessage, error);
                
                    // Try to get the UID before user is signed out
                    com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                    String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    
                    if (errorMessage != null) {
                        if (errorMessage.contains("wrong-password") || errorMessage.contains("invalid-credential")) {
                            passwordLayout.setError("Incorrect password");
                        } else if (errorMessage.contains("user-not-found")) {
                            emailLayout.setError("No account found with this email");
                        } else if (errorMessage.contains("operation-not-allowed") || errorMessage.contains("OPERATION_NOT_ALLOWED")) {
                            Toast.makeText(AdminLoginActivity.this, 
                                    "Email/Password authentication is not enabled in Firebase Console. Please enable it in Authentication > Sign-in method.", 
                                    Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("role mismatch")) {
                            Toast.makeText(AdminLoginActivity.this, "This account is not an admin", Toast.LENGTH_LONG).show();
                        } else if (errorMessage.contains("Profile not found")) {
                            String uidMessage = uid != null ? "\n\nYour UID is: " + uid + "\nUse this EXACT value as the document ID." : "";
                            Toast.makeText(AdminLoginActivity.this, 
                                    "Profile not found in Firestore!" + uidMessage + "\n\n" +
                                    "To fix:\n" +
                                    "1. Firebase Console > Authentication > Users\n" +
                                    "2. Find admin1@test.com and copy the UID\n" +
                                    "3. Firestore > 'profiles' collection\n" +
                                    "4. Add document with UID as document ID\n" +
                                    "5. Add fields:\n" +
                                    "   • userId (string) = UID\n" +
                                    "   • email (string) = admin1@test.com\n" +
                                    "   • role (string) = admin\n" +
                                    "   • active (boolean) = true\n" +
                                    "   • banned (boolean) = false", 
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AdminLoginActivity.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
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
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    private void navigateToAdminHome() {
        Intent intent = new Intent(this, AdminWelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

