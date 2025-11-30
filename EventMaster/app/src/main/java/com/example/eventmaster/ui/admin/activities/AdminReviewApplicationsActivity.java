package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.OrganizerApplicationRepositoryFs;
import com.example.eventmaster.model.OrganizerApplication;
import com.example.eventmaster.ui.admin.dialogs.AdminApplicationDetailDialog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for admins to review pending organizer applications.
 * Shows a list of pending applications that can be approved or rejected.
 */
public class AdminReviewApplicationsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewApplications;
    private LinearLayout layoutEmpty;
    private MaterialToolbar toolbar;
    
    private OrganizerApplicationRepositoryFs repository;
    private ApplicationAdapter adapter;
    private List<OrganizerApplication> applications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review_applications);

        repository = new OrganizerApplicationRepositoryFs();
        applications = new ArrayList<>();

        toolbar = findViewById(R.id.toolbar);
        recyclerViewApplications = findViewById(R.id.recyclerViewApplications);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        toolbar.setNavigationOnClickListener(v -> finish());

        // Set up RecyclerView
        adapter = new ApplicationAdapter(applications, application -> {
            // Show detail dialog when application is clicked
            AdminApplicationDetailDialog dialog = AdminApplicationDetailDialog.newInstance(
                    application,
                    this::onApplicationStatusChanged
            );
            dialog.show(getSupportFragmentManager(), "ApplicationDetailDialog");
        });

        recyclerViewApplications.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewApplications.setAdapter(adapter);

        // Load applications
        loadApplications();
    }

    /**
     * Loads pending applications from Firestore.
     */
    private void loadApplications() {
        repository.getPendingApplications()
                .addOnSuccessListener(apps -> {
                    runOnUiThread(() -> {
                        applications.clear();
                        applications.addAll(apps);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                })
                .addOnFailureListener(error -> {
                    android.util.Log.e("AdminReview", "Failed to load applications", error);
                    runOnUiThread(() -> {
                        updateEmptyState();
                    });
                });
    }

    /**
     * Updates empty state visibility.
     */
    private void updateEmptyState() {
        if (applications.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewApplications.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewApplications.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when application status changes (approved/rejected).
     * Refreshes the list.
     */
    private void onApplicationStatusChanged() {
        loadApplications();
    }

    // ---------- RecyclerView Adapter ----------

    private static class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {
        
        private final List<OrganizerApplication> applications;
        private final OnApplicationClickListener listener;
        
        interface OnApplicationClickListener {
            void onApplicationClick(OrganizerApplication application);
        }
        
        ApplicationAdapter(List<OrganizerApplication> applications, OnApplicationClickListener listener) {
            this.applications = applications;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.admin_item_application_row, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OrganizerApplication app = applications.get(position);
            holder.bind(app);
            holder.itemView.setOnClickListener(v -> listener.onApplicationClick(app));
        }
        
        @Override
        public int getItemCount() {
            return applications.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView textName;
            private final TextView textEmail;
            private final TextView textReason;
            private final TextView textSubmittedDate;
            
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textName = itemView.findViewById(R.id.textName);
                textEmail = itemView.findViewById(R.id.textEmail);
                textReason = itemView.findViewById(R.id.textReason);
                textSubmittedDate = itemView.findViewById(R.id.textSubmittedDate);
            }
            
            void bind(OrganizerApplication app) {
                textName.setText(app.getApplicantName());
                textEmail.setText(app.getApplicantEmail());
                
                // Truncate reason to 2 lines
                String reason = app.getReason();
                if (reason != null && reason.length() > 100) {
                    reason = reason.substring(0, 100) + "...";
                }
                textReason.setText(reason != null ? reason : "No reason provided");
                
                // Format submitted date
                Timestamp submittedAt = app.getSubmittedAt();
                if (submittedAt != null) {
                    Date date = submittedAt.toDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                    textSubmittedDate.setText("Submitted: " + sdf.format(date));
                } else {
                    textSubmittedDate.setText("Submitted: Unknown");
                }
            }
        }
    }
}

