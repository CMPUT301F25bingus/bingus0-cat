package com.example.eventmaster.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Event;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying events in admin view.
 * Shows event cards with admin-specific actions (view entrants, edit, cancel, etc.).
 */
public class AdminEventListAdapter extends RecyclerView.Adapter<AdminEventListAdapter.AdminEventViewHolder> {

    private List<Event> events;
    private final OnAdminEventClickListener listener;

    /**
     * Interface for handling admin event actions.
     */
    public interface OnAdminEventClickListener {
        void onEventClick(Event event);
        void onViewEntrantsClick(Event event);
        void onNotificationsClick(Event event);
        void onEditEventClick(Event event);
        void onCancelEventClick(Event event);
    }

    public AdminEventListAdapter(OnAdminEventClickListener listener) {
        this.events = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Updates the list of events and refreshes the RecyclerView.
     *
     * @param newEvents The new list of events
     */
    public void setEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    /**
     * Filters the event list based on search query.
     *
     * @param query Search query
     * @param allEvents The complete list of events to filter from
     */
    public void filter(String query, List<Event> allEvents) {
        List<Event> filteredList = new ArrayList<>();
        
        if (query.isEmpty()) {
            filteredList = allEvents;
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            for (Event event : allEvents) {
                if (event.getName() != null && event.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    event.getDescription() != null && event.getDescription().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    event.getLocation() != null && event.getLocation().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(event);
                }
            }
        }
        
        setEvents(filteredList);
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event_card, parent, false);
        return new AdminEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for admin event items.
     */
    static class AdminEventViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView thumbnail;
        private final TextView eventName;
        private final TextView eventDateText;
        private final TextView registrationDeadlineText;
        private final TextView joinedCountText;
        private final MaterialButton viewEntrantsButton;
        private final MaterialButton notificationsButton;
        private final MaterialButton editEventButton;
        private final MaterialButton cancelEventButton;

        public AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.event_thumbnail);
            eventName = itemView.findViewById(R.id.event_name);
            eventDateText = itemView.findViewById(R.id.event_date_text);
            registrationDeadlineText = itemView.findViewById(R.id.registration_deadline_text);
            joinedCountText = itemView.findViewById(R.id.joined_count_text);
            viewEntrantsButton = itemView.findViewById(R.id.view_entrants_button);
            notificationsButton = itemView.findViewById(R.id.notifications_button);
            editEventButton = itemView.findViewById(R.id.edit_event_button);
            cancelEventButton = itemView.findViewById(R.id.cancel_event_button);
        }

        public void bind(Event event, OnAdminEventClickListener listener) {
            // Set event name
            eventName.setText(event.getName() != null ? event.getName() : "Unnamed Event");

            // Format event date range
            if (event.getEventDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd - MMM dd, yyyy", Locale.getDefault());
                String dateRange = "Date → " + dateFormat.format(event.getEventDate());
                eventDateText.setText(dateRange);
            } else {
                eventDateText.setText("Date → TBA");
            }

            // Format registration deadline
            if (event.getRegistrationEndDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String deadline = "Registration deadline → " + dateFormat.format(event.getRegistrationEndDate());
                registrationDeadlineText.setText(deadline);
            } else {
                registrationDeadlineText.setText("Registration deadline → TBA");
            }

            // Set joined count (placeholder - will be updated with actual count)
            joinedCountText.setText("Joined → 0");

            // TODO: Load thumbnail image if posterUrl is available

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onEventClick(event));
            viewEntrantsButton.setOnClickListener(v -> listener.onViewEntrantsClick(event));
            notificationsButton.setOnClickListener(v -> listener.onNotificationsClick(event));
            editEventButton.setOnClickListener(v -> listener.onEditEventClick(event));
            cancelEventButton.setOnClickListener(v -> listener.onCancelEventClick(event));
        }
    }
}



