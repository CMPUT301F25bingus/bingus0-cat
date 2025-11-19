package com.example.eventmaster.ui.entrant.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Notification;
import com.example.eventmaster.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying notifications in the entrant notifications inbox.
 * Handles different notification types with appropriate colors, icons, and messages.
 * 
 * Implements US 01.04.01 (receive win notification) and US 01.04.02 (receive loss notification).
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener clickListener;

    /**
     * Interface for handling notification item click events.
     */
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    /**
     * Creates a new adapter with an empty list.
     */
    public NotificationsAdapter() {
        this.notifications = new ArrayList<>();
    }

    /**
     * Creates a new adapter with initial data.
     * 
     * @param notifications List of notifications
     */
    public NotificationsAdapter(List<Notification> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    /**
     * Sets the click listener for notification items.
     * 
     * @param listener The click listener
     */
    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Updates the list of notifications and refreshes the view.
     * 
     * @param newNotifications New list of notifications
     */
    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications != null ? newNotifications : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Returns the current list of notifications.
     * 
     * @return List of notifications
     */
    public List<Notification> getNotifications() {
        return notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entrant_item_notification_card, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, clickListener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    /**
     * ViewHolder for individual notification items.
     */
    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final CardView cardView;
        private final TextView iconText;
        private final TextView titleText;
        private final TextView messageText;
        private final TextView timestampText;

        /**
         * Creates a new ViewHolder.
         * 
         * @param itemView The item view
         */
        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            iconText = itemView.findViewById(R.id.notification_icon);
            titleText = itemView.findViewById(R.id.notification_title);
            messageText = itemView.findViewById(R.id.notification_message);
            timestampText = itemView.findViewById(R.id.notification_timestamp);
        }

        /**
         * Binds notification data to the view with appropriate styling.
         * 
         * @param notification The notification to display
         * @param clickListener Click listener for the item
         */
        public void bind(Notification notification, OnNotificationClickListener clickListener) {
            // Set title and message
            titleText.setText(notification.getTitle());
            messageText.setText(notification.getMessage());

            // Set timestamp
            String relativeTime = TimeUtils.getRelativeTimeString(notification.getSentAt());
            timestampText.setText(relativeTime);

            // Style based on notification type
            applyNotificationStyle(notification.getType());

            // Handle item clicks
            if (clickListener != null) {
                itemView.setOnClickListener(v -> clickListener.onNotificationClick(notification));
            }
        }

        /**
         * Applies appropriate styling (background color, icon, text color) based on notification type.
         * 
         * @param type The notification type
         */
        private void applyNotificationStyle(Notification.NotificationType type) {
            int backgroundRes;
            String icon;
            int titleColor;

            switch (type) {
                case LOTTERY_WON:
                    // Green background for winning lottery
                    backgroundRes = R.drawable.notification_background_success;
                    icon = "✓";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
                    break;

                case LOTTERY_LOST:
                    // Red background for losing lottery
                    backgroundRes = R.drawable.notification_background_error;
                    icon = "✗";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
                    break;

                case INVITATION:
                    // Green background for invitations
                    backgroundRes = R.drawable.notification_background_success;
                    icon = "✓";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_green_dark);
                    break;

                case REMINDER:
                    // Purple background for reminders
                    backgroundRes = R.drawable.notification_background_reminder;
                    icon = "🔔";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_blue_dark);
                    break;

                case CANCELLATION:
                    // Red background for cancellations
                    backgroundRes = R.drawable.notification_background_error;
                    icon = "✗";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_dark);
                    break;

                case GENERAL:
                default:
                    // Beige background for general messages
                    backgroundRes = R.drawable.notification_background_general;
                    icon = "💬";
                    titleColor = ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray);
                    break;
            }

            // Apply background
            Drawable background = ContextCompat.getDrawable(itemView.getContext(), backgroundRes);
            cardView.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
            itemView.setBackground(background);

            // Set icon and title color
            iconText.setText(icon);
            titleText.setTextColor(titleColor);
        }
    }
}

