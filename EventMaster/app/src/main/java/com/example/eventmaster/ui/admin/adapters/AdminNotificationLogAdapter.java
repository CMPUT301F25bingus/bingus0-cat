package com.example.eventmaster.ui.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.utils.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for displaying notifications in admin notification log.
 * Shows detailed information including recipient, sender, event, type, and timestamp.
 * Uses resolved names for better readability.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.NotificationLogViewHolder> {

    private List<Notification> notifications;
    private Map<String, String> userNameMap;  // userId/deviceId -> display name
    private Map<String, String> eventNameMap; // eventId -> event name

    public AdminNotificationLogAdapter() {
        this.notifications = new ArrayList<>();
        this.userNameMap = new HashMap<>();
        this.eventNameMap = new HashMap<>();
    }

    /**
     * Updates the list of notifications and refreshes the RecyclerView.
     *
     * @param newNotifications The new list of notifications
     */
    public void setNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications != null ? newNotifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Sets the resolved user names map (userId/deviceId -> display name).
     */
    public void setUserNameMap(Map<String, String> userNameMap) {
        this.userNameMap = userNameMap != null ? userNameMap : new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     * Sets the resolved event names map (eventId -> event name).
     */
    public void setEventNameMap(Map<String, String> eventNameMap) {
        this.eventNameMap = eventNameMap != null ? eventNameMap : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_notification_log, parent, false);
        return new NotificationLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationLogViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, userNameMap, eventNameMap);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder for individual notification log items.
     */
    static class NotificationLogViewHolder extends RecyclerView.ViewHolder {

        private final TextView typeText;
        private final TextView titleText;
        private final TextView messageText;
        private final TextView recipientText;
        private final TextView senderText;
        private final TextView eventIdText;
        private final TextView timestampText;
        private final TextView readStatusText;

        public NotificationLogViewHolder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.notification_type);
            titleText = itemView.findViewById(R.id.notification_title);
            messageText = itemView.findViewById(R.id.notification_message);
            recipientText = itemView.findViewById(R.id.notification_recipient);
            senderText = itemView.findViewById(R.id.notification_sender);
            eventIdText = itemView.findViewById(R.id.notification_event_id);
            timestampText = itemView.findViewById(R.id.notification_timestamp);
            readStatusText = itemView.findViewById(R.id.notification_read_status);
        }

        public void bind(Notification notification, Map<String, String> userNameMap, Map<String, String> eventNameMap) {
            // Type
            String typeStr = notification.getType() != null ? 
                    notification.getType().name() : "GENERAL";
            typeText.setText("Type: " + typeStr);

            // Title and message
            titleText.setText(notification.getTitle() != null ? notification.getTitle() : "No title");
            messageText.setText(notification.getMessage() != null ? notification.getMessage() : "No message");

            // Recipient - use resolved name or fallback to ID
            String recipientId = notification.getRecipientUserId();
            String recipientDisplay;
            if (recipientId == null || recipientId.isEmpty()) {
                recipientDisplay = "Unknown";
            } else if (userNameMap.containsKey(recipientId)) {
                String recipientName = userNameMap.get(recipientId);
                recipientDisplay = recipientName != null ? recipientName : recipientId;
            } else {
                recipientDisplay = recipientId;
            }
            recipientText.setText("To: " + recipientDisplay);

            // Sender - use resolved name or fallback to ID
            String senderId = notification.getSenderUserId();
            String senderDisplay;
            if (senderId == null || "system".equals(senderId)) {
                senderDisplay = "System";
            } else if (userNameMap.containsKey(senderId)) {
                String senderName = userNameMap.get(senderId);
                senderDisplay = senderName != null ? senderName : senderId;
            } else {
                senderDisplay = senderId;
            }
            senderText.setText("From: " + senderDisplay);

            // Event - use resolved name or fallback to ID
            String eventId = notification.getEventId();
            String eventDisplay;
            if (eventId == null || eventId.isEmpty()) {
                eventDisplay = "N/A";
            } else if (eventNameMap.containsKey(eventId)) {
                String eventName = eventNameMap.get(eventId);
                eventDisplay = eventName != null ? eventName : eventId;
            } else {
                eventDisplay = eventId;
            }
            eventIdText.setText("Event: " + eventDisplay);

            // Timestamp
            if (notification.getSentAt() != null) {
                String relativeTime = TimeUtils.getRelativeTimeString(notification.getSentAt());
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                String absoluteTime = dateFormat.format(notification.getSentAt());
                timestampText.setText(relativeTime + " (" + absoluteTime + ")");
            } else {
                timestampText.setText("Unknown time");
            }

            // Read status
            if (notification.isRead()) {
                readStatusText.setText("✓ Read");
                readStatusText.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                readStatusText.setText("○ Unread");
                readStatusText.setTextColor(itemView.getContext().getColor(android.R.color.holo_blue_dark));
            }
        }
    }
}

