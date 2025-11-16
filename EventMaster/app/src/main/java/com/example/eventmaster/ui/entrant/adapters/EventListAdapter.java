package com.example.eventmaster.ui.entrant.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.model.Event;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying a list of events to entrants.
 * Uses item_event_card_entrant.xml for the card layout.
 */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
        void onJoinButtonClick(Event event);
        void onQRCodeClick(Event event); // kept for compatibility; can reuse poster or ignore
    }

    private final OnEventClickListener listener;
    private List<Event> events = new ArrayList<>();

    public EventListAdapter(@NonNull OnEventClickListener listener) {
        this.listener = listener;
    }

    /** Replace current events and refresh list. */
    public void setEvents(@NonNull List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    /** Filter helper. */
    public void filter(@NonNull String query, @NonNull List<Event> allEvents) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        if (q.isEmpty()) {
            setEvents(allEvents);
            return;
        }

        List<Event> filtered = new ArrayList<>();
        for (Event e : allEvents) {
            if (e == null) continue;
            String name = safe(e.getName());
            String desc = safe(e.getDescription());
            String loc  = safe(e.getLocation());
            if (name.toLowerCase().contains(q)
                    || desc.toLowerCase().contains(q)
                    || loc.toLowerCase().contains(q)) {
                filtered.add(e);
            }
        }
        setEvents(filtered);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.entrant_item_event_card, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** ViewHolder matching item_event_card_entrant.xml */
    static class EventViewHolder extends RecyclerView.ViewHolder {

        final ImageView poster;
        final TextView title;
        final TextView location;
        final TextView dates;
        final TextView registerBy;
        final TextView capacity;
        final TextView joined;
        final MaterialButton joinButton;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            poster     = itemView.findViewById(R.id.imgEventPoster);
            title      = itemView.findViewById(R.id.txtEventTitle);
            location   = itemView.findViewById(R.id.txtEventLocation);
            dates      = itemView.findViewById(R.id.txtEventDates);
            registerBy = itemView.findViewById(R.id.register_by_text);
            capacity   = itemView.findViewById(R.id.capacity_text);
            joined     = itemView.findViewById(R.id.joined_text);
            joinButton = itemView.findViewById(R.id.join_button);
        }

        void bind(Event event, OnEventClickListener listener) {
            // ---------- Title ----------
            title.setText(safe(event.getName(), "Unnamed Event"));

            // ---------- Location ----------
            location.setText(safe(event.getLocation(), "Location TBA"));

            // ---------- Dates ----------
            if (event.getRegistrationStartDate() != null && event.getRegistrationEndDate() != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("MMM dd", Locale.getDefault());
                String text = "Reg: " + fmt.format(event.getRegistrationStartDate())
                        + " - " + fmt.format(event.getRegistrationEndDate());
                dates.setText(text);
                registerBy.setText("Register by: " + fmt.format(event.getRegistrationEndDate()));
            } else {
                dates.setText("Dates TBA");
                registerBy.setText("Register by: TBA");
            }

            // ---------- Capacity ----------
            capacity.setText("Cap: " + event.getCapacity());

            // ---------- Joined Count ----------
            joined.setText("Joined: 0"); // TODO: connect to real joined count later

            // ---------- Poster ----------
            String posterUrl = event.getPosterUrl(); // assuming Event model has getPosterUrl()
            if (posterUrl != null && !posterUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(posterUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(poster);
            } else {
                poster.setImageResource(R.drawable.ic_launcher_background);
            }

            // ---------- Clicks ----------
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });

            joinButton.setOnClickListener(v -> {
                if (listener != null) listener.onJoinButtonClick(event);
            });

            // Poster click → open event details or QR
            poster.setOnClickListener(v -> {
                if (listener != null) listener.onQRCodeClick(event);
            });
        }

        private static String safe(String s, String fallback) {
            return (s == null || s.isEmpty()) ? fallback : s;
        }
    }
}
