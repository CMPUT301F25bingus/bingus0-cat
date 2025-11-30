package com.example.eventmaster.data.firestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.eventmaster.common.FirestoreFields;
import com.example.eventmaster.common.FirestorePaths;
import com.example.eventmaster.model.OrganizerApplication;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore repository for organizer applications.
 * Handles CRUD operations for the organizerApplications collection.
 */
public class OrganizerApplicationRepositoryFs {
    
    private static final String COLL = FirestorePaths.ORGANIZER_APPLICATIONS;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    // ---------- Mapping helpers ----------
    
    private static Map<String, Object> toMap(@NonNull OrganizerApplication app) {
        Map<String, Object> m = new HashMap<>();
        
        putIfNotNull(m, FirestoreFields.APPLICANT_EMAIL, app.getApplicantEmail());
        putIfNotNull(m, FirestoreFields.APPLICANT_NAME, app.getApplicantName());
        putIfNotNull(m, FirestoreFields.ENCRYPTED_PASSWORD, app.getEncryptedPassword());
        putIfNotNull(m, FirestoreFields.REASON, app.getReason());
        putIfNotNull(m, FirestoreFields.APPLICATION_STATUS, app.getStatus());
        
        if (app.getSubmittedAt() != null) {
            m.put(FirestoreFields.SUBMITTED_AT, app.getSubmittedAt());
        }
        
        if (app.getReviewedAt() != null) {
            m.put(FirestoreFields.REVIEWED_AT, app.getReviewedAt());
        }
        
        putIfNotNull(m, FirestoreFields.REVIEWED_BY, app.getReviewedBy());
        putIfNotNull(m, FirestoreFields.REJECTION_REASON, app.getRejectionReason());
        
        return m;
    }
    
    private static void putIfNotNull(Map<String, Object> m, String key, @Nullable Object val) {
        if (val == null) return;
        if (val instanceof String && ((String) val).isEmpty()) return;
        m.put(key, val);
    }
    
    private static OrganizerApplication fromDoc(@NonNull DocumentSnapshot doc) {
        OrganizerApplication app = new OrganizerApplication();
        
        app.setApplicationId(doc.getId());
        app.setApplicantEmail(doc.getString(FirestoreFields.APPLICANT_EMAIL));
        app.setApplicantName(doc.getString(FirestoreFields.APPLICANT_NAME));
        app.setEncryptedPassword(doc.getString(FirestoreFields.ENCRYPTED_PASSWORD));
        app.setReason(doc.getString(FirestoreFields.REASON));
        
        String status = doc.getString(FirestoreFields.APPLICATION_STATUS);
        app.setStatus(status != null ? status : "pending");
        
        Timestamp submittedAt = doc.getTimestamp(FirestoreFields.SUBMITTED_AT);
        if (submittedAt != null) {
            app.setSubmittedAt(submittedAt);
        } else {
            app.setSubmittedAt(Timestamp.now());
        }
        
        Timestamp reviewedAt = doc.getTimestamp(FirestoreFields.REVIEWED_AT);
        app.setReviewedAt(reviewedAt);
        
        app.setReviewedBy(doc.getString(FirestoreFields.REVIEWED_BY));
        app.setRejectionReason(doc.getString(FirestoreFields.REJECTION_REASON));
        
        return app;
    }
    
    // ---------- Create / Update ----------
    
    /**
     * Creates a new organizer application in Firestore.
     * 
     * @param application The application to create
     * @return Task with the document ID of the created application
     */
    public Task<String> createApplication(@NonNull OrganizerApplication application) {
        if (application.getApplicantEmail() == null || application.getApplicantEmail().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Applicant email is required"));
        }
        
        Map<String, Object> data = toMap(application);
        
        // Ensure submittedAt is set
        if (!data.containsKey(FirestoreFields.SUBMITTED_AT)) {
            data.put(FirestoreFields.SUBMITTED_AT, Timestamp.now());
        }
        
        // Ensure status is set
        if (!data.containsKey(FirestoreFields.APPLICATION_STATUS)) {
            data.put(FirestoreFields.APPLICATION_STATUS, "pending");
        }
        
        return db.collection(COLL)
                .add(data)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().getId();
                });
    }
    
    /**
     * Updates the status of an application (approve/reject).
     * 
     * @param applicationId The application document ID
     * @param status The new status ("approved" or "rejected")
     * @param reviewedBy The admin userId who reviewed it
     * @param rejectionReason Optional rejection reason (null if approved)
     * @return Task that completes when update is done
     */
    public Task<Void> updateApplicationStatus(@NonNull String applicationId,
                                            @NonNull String status,
                                            @NonNull String reviewedBy,
                                            @Nullable String rejectionReason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirestoreFields.APPLICATION_STATUS, status);
        updates.put(FirestoreFields.REVIEWED_AT, Timestamp.now());
        updates.put(FirestoreFields.REVIEWED_BY, reviewedBy);
        
        if (rejectionReason != null && !rejectionReason.isEmpty()) {
            updates.put(FirestoreFields.REJECTION_REASON, rejectionReason);
        }
        
        return db.collection(COLL).document(applicationId).update(updates);
    }
    
    // ---------- Reads ----------
    
    /**
     * Gets an application by its document ID.
     * 
     * @param applicationId The application document ID
     * @return Task with the application, or exception if not found
     */
    public Task<OrganizerApplication> getApplicationById(@NonNull String applicationId) {
        return db.collection(COLL).document(applicationId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    DocumentSnapshot doc = task.getResult();
                    if (doc == null || !doc.exists()) {
                        throw new IllegalStateException("Application not found: " + applicationId);
                    }
                    return fromDoc(doc);
                });
    }
    
    /**
     * Gets an application by applicant email.
     * Returns the most recent application if multiple exist.
     * 
     * @param email The applicant email
     * @return Task with the application, or null if not found
     */
    public Task<OrganizerApplication> getApplicationByEmail(@NonNull String email) {
        return db.collection(COLL)
                .whereEqualTo(FirestoreFields.APPLICANT_EMAIL, email)
                // Removed orderBy to avoid index requirement
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    QuerySnapshot snap = task.getResult();
                    if (snap != null && !snap.isEmpty()) {
                        // Sort in memory to get most recent
                        List<OrganizerApplication> apps = new ArrayList<>();
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            apps.add(fromDoc(doc));
                        }
                        // Sort by submittedAt descending (most recent first)
                        apps.sort((a, b) -> {
                            Timestamp aTime = a.getSubmittedAt();
                            Timestamp bTime = b.getSubmittedAt();
                            if (aTime == null && bTime == null) return 0;
                            if (aTime == null) return 1;
                            if (bTime == null) return -1;
                            return bTime.compareTo(aTime); // Descending
                        });
                        return apps.get(0); // Return most recent
                    }
                    return null;
                });
    }
    
    /**
     * Checks if there's already a pending application for the given email.
     * 
     * @param email The applicant email
     * @return Task with true if pending application exists, false otherwise
     */
    public Task<Boolean> hasPendingApplication(@NonNull String email) {
        return db.collection(COLL)
                .whereEqualTo(FirestoreFields.APPLICANT_EMAIL, email)
                .whereEqualTo(FirestoreFields.APPLICATION_STATUS, "pending")
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    QuerySnapshot snap = task.getResult();
                    return snap != null && !snap.isEmpty();
                });
    }
    
    /**
     * Gets all pending applications (for admin review).
     * 
     * @return Task with list of pending applications
     */
    public Task<List<OrganizerApplication>> getPendingApplications() {
        return db.collection(COLL)
                .whereEqualTo(FirestoreFields.APPLICATION_STATUS, "pending")
                // Removed orderBy to avoid index requirement
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<OrganizerApplication> applications = new ArrayList<>();
                    QuerySnapshot snap = task.getResult();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            applications.add(fromDoc(doc));
                        }
                        // Sort in memory by submittedAt ascending (oldest first)
                        applications.sort((a, b) -> {
                            Timestamp aTime = a.getSubmittedAt();
                            Timestamp bTime = b.getSubmittedAt();
                            if (aTime == null && bTime == null) return 0;
                            if (aTime == null) return 1;
                            if (bTime == null) return -1;
                            return aTime.compareTo(bTime); // Ascending
                        });
                    }
                    return applications;
                });
    }
    
    /**
     * Gets all applications (for admin - all statuses).
     * 
     * @return Task with list of all applications
     */
    public Task<List<OrganizerApplication>> getAllApplications() {
        return db.collection(COLL)
                .orderBy(FirestoreFields.SUBMITTED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<OrganizerApplication> applications = new ArrayList<>();
                    QuerySnapshot snap = task.getResult();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            applications.add(fromDoc(doc));
                        }
                    }
                    return applications;
                });
    }
}

