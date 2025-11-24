package com.example.eventmaster.ui.admin.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.ui.admin.adapters.AdminNotificationLogAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment displaying a log of all notifications sent in the system.
 * Shows notifications sorted by most recent first.
 */
public class AdminNotificationLogFragment extends Fragment {

    private static final String TAG = "AdminNotificationLog";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private RecyclerView recyclerView;
    private ImageView backButton;
    private TextView emptyStateText;
    private AdminNotificationLogAdapter adapter;
    private List<Notification> allNotifications = new ArrayList<>();

    public AdminNotificationLogFragment() {
        // Required empty public constructor
    }

    public static AdminNotificationLogFragment newInstance() {
        return new AdminNotificationLogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.admin_fragment_notification_log, container, false);

        // Initialize UI elements
        backButton = view.findViewById(R.id.back_button);
        recyclerView = view.findViewById(R.id.notifications_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup RecyclerView
        adapter = new AdminNotificationLogAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load all notifications
        loadAllNotifications();

        return view;
    }

    /**
     * Loads all notifications from Firestore, sorted by most recent first.
     */
    private void loadAllNotifications() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLLECTION_NOTIFICATIONS)
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allNotifications.clear();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Notification notification = parseNotification(doc);
                            if (notification != null) {
                                allNotifications.add(notification);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification document: " + doc.getId(), e);
                        }
                    }

                    adapter.setNotifications(allNotifications);
                    updateEmptyState();
                    
                    Log.d(TAG, "Loaded " + allNotifications.size() + " notifications");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load notifications", e);
                    Toast.makeText(requireContext(),
                            "Failed to load notifications: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    /**
     * Parses a Firestore document into a Notification object.
     * Handles legacy field names and missing fields.
     */
    private Notification parseNotification(QueryDocumentSnapshot doc) {
        try {
            Notification notification = new Notification();
            notification.setNotificationId(doc.getId());

            // Event ID
            String eventId = doc.getString("eventId");
            notification.setEventId(eventId);

            // Recipient - try recipientUserId first, then recipientId (legacy)
            String recipientUserId = doc.getString("recipientUserId");
            if (recipientUserId == null || recipientUserId.isEmpty()) {
                recipientUserId = doc.getString("recipientId");
            }
            notification.setRecipientUserId(recipientUserId);

            // Sender
            String senderUserId = doc.getString("senderUserId");
            if (senderUserId == null || senderUserId.isEmpty()) {
                senderUserId = "system";
            }
            notification.setSenderUserId(senderUserId);

            // Type
            String typeStr = doc.getString("type");
            if (typeStr != null) {
                try {
                    notification.setType(Notification.NotificationType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    notification.setType(Notification.NotificationType.GENERAL);
                }
            } else {
                notification.setType(Notification.NotificationType.GENERAL);
            }

            // Title and message
            notification.setTitle(doc.getString("title"));
            notification.setMessage(doc.getString("message"));

            // Sent date - try sentAt first, then createdAt (legacy)
            Date sentAt = null;
            if (doc.getTimestamp("sentAt") != null) {
                sentAt = doc.getTimestamp("sentAt").toDate();
            } else if (doc.getTimestamp("createdAt") != null) {
                sentAt = doc.getTimestamp("createdAt").toDate();
            }
            if (sentAt == null) {
                sentAt = new Date(); // Fallback to current time
            }
            notification.setSentAt(sentAt);

            // Read status
            Boolean isRead = doc.getBoolean("isRead");
            notification.setRead(isRead != null && isRead);

            return notification;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification from document: " + doc.getId(), e);
            return null;
        }
    }

    /**
     * Updates the empty state visibility based on notification count.
     */
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
}

