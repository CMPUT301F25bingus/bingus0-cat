package com.example.eventmaster.ui.admin.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.OrganizerApplicationRepositoryFs;
import com.example.eventmaster.model.OrganizerApplication;
import com.example.eventmaster.utils.AuthHelper;
import com.example.eventmaster.utils.CredentialStorageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dialog showing full application details with approve/reject options.
 */
public class AdminApplicationDetailDialog extends DialogFragment {

    private static final String ARG_APPLICATION = "application";
    
    private OrganizerApplication application;
    private OnStatusChangedListener listener;
    
    private OrganizerApplicationRepositoryFs repository;
    
    public interface OnStatusChangedListener {
        void onStatusChanged();
    }
    
    /**
     * Creates a new instance of the dialog.
     */
    public static AdminApplicationDetailDialog newInstance(
            OrganizerApplication application,
            OnStatusChangedListener listener) {
        AdminApplicationDetailDialog dialog = new AdminApplicationDetailDialog();
        dialog.application = application;
        dialog.listener = listener;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        repository = new OrganizerApplicationRepositoryFs();
        
        // Restore application from arguments if needed
        if (application == null && getArguments() != null) {
            // Could restore from bundle if needed
        }
        
        // Create custom view
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.dialog_admin_application_detail, null);
        
        // Populate view
        TextView textName = view.findViewById(R.id.textName);
        TextView textEmail = view.findViewById(R.id.textEmail);
        TextView textReason = view.findViewById(R.id.textReason);
        TextView textSubmittedDate = view.findViewById(R.id.textSubmittedDate);
        
        MaterialButton btnApprove = view.findViewById(R.id.btnApprove);
        MaterialButton btnReject = view.findViewById(R.id.btnReject);
        
        textName.setText(application.getApplicantName());
        textEmail.setText(application.getApplicantEmail());
        textReason.setText(application.getReason());
        
        // Format submitted date
        Timestamp submittedAt = application.getSubmittedAt();
        if (submittedAt != null) {
            Date date = submittedAt.toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault());
            textSubmittedDate.setText("Submitted: " + sdf.format(date));
        } else {
            textSubmittedDate.setText("Submitted: Unknown");
        }
        
        // Approve button
        btnApprove.setOnClickListener(v -> handleApprove());
        
        // Reject button
        btnReject.setOnClickListener(v -> showRejectDialog());
        
        // Create dialog
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Application Details")
                .setView(view)
                .setNegativeButton("Close", null)
                .create();
    }
    
    /**
     * Handles approval of the application.
     */
    private void handleApprove() {
        // Decrypt password
        String encryptedPassword = application.getEncryptedPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            Toast.makeText(getContext(), "Error: Password not found in application", Toast.LENGTH_LONG).show();
            return;
        }
        
        String decryptedPassword = CredentialStorageHelper.decryptPassword(getContext(), encryptedPassword);
        if (decryptedPassword == null) {
            Toast.makeText(getContext(), "Error: Failed to decrypt password", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Get current admin user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not signed in", Toast.LENGTH_LONG).show();
            return;
        }
        
        String adminUserId = currentUser.getUid();
        
        // Disable buttons
        View view = getDialog().findViewById(R.id.btnApprove);
        if (view != null) {
            view.setEnabled(false);
        }
        view = getDialog().findViewById(R.id.btnReject);
        if (view != null) {
            view.setEnabled(false);
        }
        
        // Create organizer account
        AuthHelper.signUpOrganizer(
                application.getApplicantEmail(),
                decryptedPassword,
                application.getApplicantName(),
                new AuthHelper.OnAuthCompleteListener() {
                    @Override
                    public void onSuccess(com.google.firebase.auth.FirebaseUser user, com.example.eventmaster.model.Profile profile) {
                        // Update application status
                        repository.updateApplicationStatus(
                                application.getApplicationId(),
                                "approved",
                                adminUserId,
                                null
                        ).addOnSuccessListener(aVoid -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Application approved! Account created.", Toast.LENGTH_SHORT).show();
                                    if (listener != null) {
                                        listener.onStatusChanged();
                                    }
                                    dismiss();
                                });
                            }
                        }).addOnFailureListener(error -> {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getContext(), "Account created but failed to update application status", Toast.LENGTH_LONG).show();
                                    android.util.Log.e("AdminDialog", "Failed to update application status", error);
                                    if (listener != null) {
                                        listener.onStatusChanged();
                                    }
                                    dismiss();
                                });
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Re-enable buttons
                                View approveBtn = getDialog().findViewById(R.id.btnApprove);
                                if (approveBtn != null) {
                                    approveBtn.setEnabled(true);
                                }
                                View rejectBtn = getDialog().findViewById(R.id.btnReject);
                                if (rejectBtn != null) {
                                    rejectBtn.setEnabled(true);
                                }
                                
                                String errorMsg = error != null ? error.getMessage() : "Unknown error";
                                if (errorMsg.contains("already in use") || errorMsg.contains("EMAIL_EXISTS")) {
                                    Toast.makeText(getContext(), "Email already registered. Updating application status only.", Toast.LENGTH_LONG).show();
                                    // Still update status even if account exists
                                    repository.updateApplicationStatus(
                                            application.getApplicationId(),
                                            "approved",
                                            adminUserId,
                                            null
                                    ).addOnSuccessListener(aVoid -> {
                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() -> {
                                                if (listener != null) {
                                                    listener.onStatusChanged();
                                                }
                                                dismiss();
                                            });
                                        }
                                    });
                                } else {
                                    Toast.makeText(getContext(), "Failed to create account: " + errorMsg, Toast.LENGTH_LONG).show();
                                    android.util.Log.e("AdminDialog", "Failed to create account", error);
                                }
                            });
                        }
                    }
                }
        );
    }
    
    /**
     * Shows dialog to enter rejection reason.
     */
    private void showRejectDialog() {
        // Create dialog with reason input
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Reject Application");
        builder.setMessage("Please provide a reason for rejection (optional):");
        
        // Create input field
        final TextInputEditText input = new TextInputEditText(getContext());
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Rejection reason");
        input.setMinLines(3);
        input.setMaxLines(5);
        
        // Wrap in TextInputLayout
        TextInputLayout inputLayout = new TextInputLayout(getContext(), null,
                com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox);
        inputLayout.setHint("Reason (optional)");
        inputLayout.addView(input);
        inputLayout.setPadding(
                (int) (48 * getResources().getDisplayMetrics().density),
                0,
                (int) (48 * getResources().getDisplayMetrics().density),
                0
        );
        
        builder.setView(inputLayout);
        builder.setPositiveButton("Reject", (dialog, which) -> {
            String reason = input.getText().toString().trim();
            handleReject(reason);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Handles rejection of the application.
     */
    private void handleReject(String rejectionReason) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Not signed in", Toast.LENGTH_LONG).show();
            return;
        }
        
        String adminUserId = currentUser.getUid();
        
        // Disable buttons
        View view = getDialog().findViewById(R.id.btnApprove);
        if (view != null) {
            view.setEnabled(false);
        }
        view = getDialog().findViewById(R.id.btnReject);
        if (view != null) {
            view.setEnabled(false);
        }
        
        // Update application status
        repository.updateApplicationStatus(
                application.getApplicationId(),
                "rejected",
                adminUserId,
                rejectionReason.isEmpty() ? null : rejectionReason
        ).addOnSuccessListener(aVoid -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Application rejected", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onStatusChanged();
                    }
                    dismiss();
                });
            }
        }).addOnFailureListener(error -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to reject application: " + 
                            (error != null ? error.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    android.util.Log.e("AdminDialog", "Failed to reject application", error);
                    
                    // Re-enable buttons
                    View approveBtn = getDialog().findViewById(R.id.btnApprove);
                    if (approveBtn != null) {
                        approveBtn.setEnabled(true);
                    }
                    View rejectBtn = getDialog().findViewById(R.id.btnReject);
                    if (rejectBtn != null) {
                        rejectBtn.setEnabled(true);
                    }
                });
            }
        });
    }
}

