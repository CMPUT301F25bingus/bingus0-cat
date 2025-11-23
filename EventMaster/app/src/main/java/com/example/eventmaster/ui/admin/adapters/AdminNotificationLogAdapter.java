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
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying notifications in admin notification log.
 * Shows detailed information including recipient, sender, event, type, and timestamp.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.NotificationLogViewHolder> {

    private List<Notification> notifications;

    public AdminNotificationLogAdapter() {
        this.notifications = new ArrayList<>();
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
        holder.bind(notification);
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

        public void bind(Notification notification) {
            // Type
            String typeStr = notification.getType() != null ? 
                    notification.getType().name() : "GENERAL";
            typeText.setText("Type: " + typeStr);

            // Title and message
            titleText.setText(notification.getTitle() != null ? notification.getTitle() : "No title");
            messageText.setText(notification.getMessage() != null ? notification.getMessage() : "No message");

            // Recipient
            String recipient = notification.getRecipientUserId() != null ? 
                    notification.getRecipientUserId() : "Unknown";
            recipientText.setText("To: " + recipient);

            // Sender
            String sender = notification.getSenderUserId() != null ? 
                    notification.getSenderUserId() : "system";
            senderText.setText("From: " + sender);

            // Event ID
            String eventId = notification.getEventId() != null ? 
                    notification.getEventId() : "N/A";
            eventIdText.setText("Event: " + eventId);

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

