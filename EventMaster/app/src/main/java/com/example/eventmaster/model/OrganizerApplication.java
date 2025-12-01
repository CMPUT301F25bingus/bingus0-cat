package com.example.eventmaster.model;

import com.google.firebase.Timestamp;

/**
 * Represents an organizer application submitted by a user.
 * Applications are stored in Firestore and reviewed by admins.
 */
public class OrganizerApplication {
    
    private String applicationId;          // Firestore document ID
    private String applicantEmail;         // Required
    private String applicantName;          // Required
    private String encryptedPassword;      // Encrypted password for account creation
    private String reason;                 // Why they want to be organizer
    private String status;                 // "pending", "approved", "rejected" (default "pending")
    private Timestamp submittedAt;         // When application was submitted
    private Timestamp reviewedAt;          // When admin reviewed it (nullable)
    private String reviewedBy;             // Admin userId who reviewed it (nullable)
    private String rejectionReason;       // Reason if rejected (nullable)
    
    /**
     * No-arg constructor required by Firestore.
     */
    public OrganizerApplication() {
        this.status = "pending";
    }
    
    /**
     * Constructor with required fields.
     */
    public OrganizerApplication(String applicantEmail, String applicantName, 
                               String encryptedPassword, String reason) {
        this();
        this.applicantEmail = applicantEmail;
        this.applicantName = applicantName;
        this.encryptedPassword = encryptedPassword;
        this.reason = reason;
        this.submittedAt = Timestamp.now();
    }
    
    // ---- Getters/Setters ----
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getApplicantEmail() {
        return applicantEmail == null ? "" : applicantEmail;
    }
    
    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }
    
    public String getApplicantName() {
        return applicantName == null ? "" : applicantName;
    }
    
    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }
    
    public String getEncryptedPassword() {
        return encryptedPassword;
    }
    
    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }
    
    public String getReason() {
        return reason == null ? "" : reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getStatus() {
        return status == null ? "pending" : status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Timestamp getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(Timestamp submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public Timestamp getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public String getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    /**
     * Helper method to check if application is pending.
     */
    public boolean isPending() {
        return "pending".equals(status);
    }
    
    /**
     * Helper method to check if application is approved.
     */
    public boolean isApproved() {
        return "approved".equals(status);
    }
    
    /**
     * Helper method to check if application is rejected.
     */
    public boolean isRejected() {
        return "rejected".equals(status);
    }
}

