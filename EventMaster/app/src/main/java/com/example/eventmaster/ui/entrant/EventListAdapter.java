package com.example.eventmaster.ui.entrant;

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
 * RecyclerView adapter for displaying a list of events.
 * Shows event cards with thumbnail, name, join button, and event info.
 */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

    private List<Event> events;
    private final OnEventClickListener listener;

    /**
     * Interface for handling event item clicks.
     */
    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onJoinButtonClick(Event event);
        void onQRCodeClick(Event event);
    }

    public EventListAdapter(OnEventClickListener listener) {
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
                if (event.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    event.getDescription().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    event.getLocation().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) {
                    filteredList.add(event);
                }
            }
        }
        
        setEvents(filteredList);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for event items.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView thumbnail;
        private final ImageView qrCode;
        private final TextView eventName;
        private final MaterialButton joinButton;
        private final TextView registerByText;
        private final TextView capacityText;
        private final TextView joinedText;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.event_thumbnail);
            qrCode = itemView.findViewById(R.id.qr_code_image);
            eventName = itemView.findViewById(R.id.event_name);
            joinButton = itemView.findViewById(R.id.join_button);
            registerByText = itemView.findViewById(R.id.register_by_text);
            capacityText = itemView.findViewById(R.id.capacity_text);
            joinedText = itemView.findViewById(R.id.joined_text);
        }

        public void bind(Event event, OnEventClickListener listener) {
            // Set event name
            eventName.setText(event.getName() != null ? event.getName() : "Unnamed Event");

            // Format registration date with null check
            if (event.getRegistrationEndDate() != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                String registerBy = "Register by: " + dateFormat.format(event.getRegistrationEndDate());
                registerByText.setText(registerBy);
            } else {
                registerByText.setText("Register by: TBA");
            }

            // Set capacity
            capacityText.setText("Cap: " + event.getCapacity());

            // Set joined count (placeholder - will be updated with actual count)
            joinedText.setText("Joined: 0");

            // TODO: Load thumbnail image if posterUrl is available
            // For now, thumbnail shows gray background

            // TODO: Generate or load QR code
            // For now, QR code shows placeholder

            // Set click listeners
            itemView.setOnClickListener(v -> listener.onEventClick(event));
            joinButton.setOnClickListener(v -> listener.onJoinButtonClick(event));
            qrCode.setOnClickListener(v -> listener.onQRCodeClick(event));
        }
    }
}

