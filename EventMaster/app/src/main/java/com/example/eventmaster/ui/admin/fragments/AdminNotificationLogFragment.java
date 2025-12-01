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
import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.data.firestore.EventReadServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.ui.admin.adapters.AdminNotificationLogAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragment displaying a log of all notifications sent in the system.
 * Shows notifications sorted by most recent first.
 */
public class AdminNotificationLogFragment extends Fragment {

    private static final String TAG = "AdminNotificationLog";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    private static final String ARG_ORGANIZER_ID = "organizerId";
    private static final String ARG_EVENT_ID = "eventId";

    private RecyclerView recyclerView;
    private ImageView backButton;
    private TextView emptyStateText;
    private AdminNotificationLogAdapter adapter;
    private List<Notification> allNotifications = new ArrayList<>();
    private String organizerId;
    private String eventId;
    private EventReadService eventReadService;

    public AdminNotificationLogFragment() {
        // Required empty public constructor
    }

    public static AdminNotificationLogFragment newInstance(String organizerId, String eventId) {
        AdminNotificationLogFragment fragment = new AdminNotificationLogFragment();
        Bundle args = new Bundle();
        if (organizerId != null) {
            args.putString(ARG_ORGANIZER_ID, organizerId);
        }
        if (eventId != null) {
            args.putString(ARG_EVENT_ID, eventId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            organizerId = getArguments().getString(ARG_ORGANIZER_ID);
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
        eventReadService = new EventReadServiceFs();
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

        // Load notifications (filtered by eventId, organizerId, or all)
        if (eventId != null && !eventId.isEmpty()) {
            loadNotificationsForEvent();
        } else if (organizerId != null && !organizerId.isEmpty()) {
            loadNotificationsForOrganizer();
        } else {
            loadAllNotifications();
        }

        return view;
    }

    /**
     * Loads notifications for a specific organizer by first getting their events,
     * then filtering notifications by those event IDs.
     */
    private void loadNotificationsForOrganizer() {
        Log.d(TAG, "Loading notifications for organizer: " + organizerId);
        
        // First, get all events for this organizer
        eventReadService.listByOrganizer(organizerId)
                .addOnSuccessListener(events -> {
                    if (events == null || events.isEmpty()) {
                        Log.d(TAG, "No events found for organizer, showing empty list");
                        adapter.setNotifications(new ArrayList<>());
                        updateEmptyState();
                        return;
                    }
                    
                    // Collect event IDs
                    Set<String> eventIds = new HashSet<>();
                    for (Event event : events) {
                        if (event.getEventId() != null) {
                            eventIds.add(event.getEventId());
                        }
                    }
                    
                    Log.d(TAG, "Found " + eventIds.size() + " events for organizer");
                    
                    // Now load notifications for these events
                    loadNotificationsForEvents(eventIds);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events for organizer", e);
                    Toast.makeText(requireContext(),
                            "Failed to load organizer events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    adapter.setNotifications(new ArrayList<>());
                    updateEmptyState();
                });
    }

    /**
     * Loads notifications for a set of event IDs.
     * Handles Firestore's whereIn limit of 10 items by batching queries if needed.
     */
    private void loadNotificationsForEvents(Set<String> eventIds) {
        if (eventIds.isEmpty()) {
            adapter.setNotifications(new ArrayList<>());
            updateEmptyState();
            return;
        }
        
        List<String> eventIdList = new ArrayList<>(eventIds);
        allNotifications.clear();
        
        // Firestore whereIn has a limit of 10 items, so we need to batch if there are more
        final int BATCH_SIZE = 10;
        final int totalBatches = (eventIdList.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        final AtomicInteger completedBatches = new AtomicInteger(0);
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        for (int i = 0; i < eventIdList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, eventIdList.size());
            List<String> batch = eventIdList.subList(i, end);
            
            db.collection(COLLECTION_NOTIFICATIONS)
                    .whereIn("eventId", batch)
                    .orderBy("sentAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
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
                        
                        int completed = completedBatches.incrementAndGet();
                        if (completed == totalBatches) {
                            // All batches completed, sort and update UI
                            allNotifications.sort((a, b) -> {
                                Date dateA = a.getSentAt();
                                Date dateB = b.getSentAt();
                                if (dateA == null && dateB == null) return 0;
                                if (dateA == null) return 1;
                                if (dateB == null) return -1;
                                return dateB.compareTo(dateA); // Descending
                            });
                            
                            adapter.setNotifications(allNotifications);
                            updateEmptyState();
                            Log.d(TAG, "Loaded " + allNotifications.size() + " notifications for organizer");
                        }
                    })
                    .addOnFailureListener(e -> {
                        // If orderBy fails, try without orderBy
                        Log.w(TAG, "Query with orderBy failed for batch, trying without orderBy: " + e.getMessage());
                        db.collection(COLLECTION_NOTIFICATIONS)
                                .whereIn("eventId", batch)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (QueryDocumentSnapshot doc : querySnapshot) {
                                        try {
                                            Notification notification = parseNotification(doc);
                                            if (notification != null) {
                                                allNotifications.add(notification);
                                            }
                                        } catch (Exception ex) {
                                            Log.e(TAG, "Error parsing notification document: " + doc.getId(), ex);
                                        }
                                    }
                                    
                                    int completed = completedBatches.incrementAndGet();
                                    if (completed == totalBatches) {
                                        // All batches completed, sort and update UI
                                        allNotifications.sort((a, b) -> {
                                            Date dateA = a.getSentAt();
                                            Date dateB = b.getSentAt();
                                            if (dateA == null && dateB == null) return 0;
                                            if (dateA == null) return 1;
                                            if (dateB == null) return -1;
                                            return dateB.compareTo(dateA); // Descending
                                        });
                                        
                                        adapter.setNotifications(allNotifications);
                                        updateEmptyState();
                                        Log.d(TAG, "Loaded " + allNotifications.size() + " notifications for organizer (without orderBy)");
                                    }
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed to load notifications batch for organizer", ex);
                                    int completed = completedBatches.incrementAndGet();
                                    if (completed == totalBatches) {
                                        // Even if some batches failed, update UI with what we have
                                        allNotifications.sort((a, b) -> {
                                            Date dateA = a.getSentAt();
                                            Date dateB = b.getSentAt();
                                            if (dateA == null && dateB == null) return 0;
                                            if (dateA == null) return 1;
                                            if (dateB == null) return -1;
                                            return dateB.compareTo(dateA); // Descending
                                        });
                                        
                                        adapter.setNotifications(allNotifications);
                                        updateEmptyState();
                                        if (allNotifications.isEmpty()) {
                                            Toast.makeText(requireContext(),
                                                    "Failed to load notifications: " + ex.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    });
        }
    }

    /**
     * Loads notifications for a specific event.
     */
    private void loadNotificationsForEvent() {
        Log.d(TAG, "Loading notifications for event: " + eventId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("eventId", eventId)
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
                    
                    Log.d(TAG, "Loaded " + allNotifications.size() + " notifications for event");
                })
                .addOnFailureListener(e -> {
                    // If orderBy fails, try without orderBy
                    Log.w(TAG, "Query with orderBy failed, trying without orderBy: " + e.getMessage());
                    db.collection(COLLECTION_NOTIFICATIONS)
                            .whereEqualTo("eventId", eventId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                allNotifications.clear();
                                
                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    try {
                                        Notification notification = parseNotification(doc);
                                        if (notification != null) {
                                            allNotifications.add(notification);
                                        }
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error parsing notification document: " + doc.getId(), ex);
                                    }
                                }
                                
                                // Sort manually by sentAt
                                allNotifications.sort((a, b) -> {
                                    Date dateA = a.getSentAt();
                                    Date dateB = b.getSentAt();
                                    if (dateA == null && dateB == null) return 0;
                                    if (dateA == null) return 1;
                                    if (dateB == null) return -1;
                                    return dateB.compareTo(dateA); // Descending
                                });

                                adapter.setNotifications(allNotifications);
                                updateEmptyState();
                                
                                Log.d(TAG, "Loaded " + allNotifications.size() + " notifications for event (without orderBy)");
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Failed to load notifications for event", ex);
                                Toast.makeText(requireContext(),
                                        "Failed to load notifications: " + ex.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                updateEmptyState();
                            });
                });
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

