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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private Map<String, Integer> waitingListCounts = new HashMap<>(); // eventId -> count

    public EventListAdapter(@NonNull OnEventClickListener listener) {
        this.listener = listener;
    }

    /** Set waiting list counts map and refresh the display. */
    public void setWaitingListCounts(@NonNull Map<String, Integer> counts) {
        this.waitingListCounts = new HashMap<>(counts);
        notifyDataSetChanged();
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
        holder.bind(events.get(position), listener, waitingListCounts);
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
        final TextView description;
        final TextView badgeStatus;
        final TextView categoryChip;
        final TextView waitingListCount;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            poster           = itemView.findViewById(R.id.imgEventPoster);
            title            = itemView.findViewById(R.id.txtEventTitle);
            location         = itemView.findViewById(R.id.txtEventLocation);
            dates            = itemView.findViewById(R.id.txtEventDates);
            description      = itemView.findViewById(R.id.txtDescription);
            badgeStatus      = itemView.findViewById(R.id.badge_status);
            categoryChip     = itemView.findViewById(R.id.category_chip);
            waitingListCount = itemView.findViewById(R.id.txtWaitingListCount);
        }

        void bind(Event event, OnEventClickListener listener, Map<String, Integer> waitingListCounts) {
            // ---------- Title ----------
            title.setText(safe(event.getName(), "Unnamed Event"));

            // ---------- Description ----------
            String desc = event.getDescription();
            if (desc != null && !desc.trim().isEmpty()) {
                description.setText(desc);
                description.setVisibility(View.VISIBLE);
            } else {
                description.setVisibility(View.GONE);
            }

            // ---------- Location ----------
            location.setText(safe(event.getLocation(), "Location TBA"));

            // ---------- Event Dates (showing actual event date, not registration dates) ----------
            if (event.getEventDate() != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String eventDateText = fmt.format(event.getEventDate());
                
                // If event spans multiple days, try to show range
                if (event.getRegistrationStartDate() != null && event.getRegistrationEndDate() != null) {
                    Date regEnd = event.getRegistrationEndDate();
                    if (regEnd.after(event.getEventDate())) {
                        // Event might span days, show range
                        String endDateText = fmt.format(regEnd);
                        dates.setText(eventDateText + " - " + endDateText);
                    } else {
                        dates.setText(eventDateText);
                    }
                } else {
                    dates.setText(eventDateText);
                }
            } else if (event.getRegistrationStartDate() != null && event.getRegistrationEndDate() != null) {
                // Fallback to registration dates if event date not available
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startText = fmt.format(event.getRegistrationStartDate());
                String endText = fmt.format(event.getRegistrationEndDate());
                dates.setText(startText + " - " + endText);
            } else {
                dates.setText("Dates TBA");
            }

            // ---------- Waiting List Count ----------
            String eventId = event.getId();
            int count = (waitingListCounts != null && eventId != null) 
                    ? waitingListCounts.getOrDefault(eventId, 0) : 0;
            if (count == 1) {
                waitingListCount.setText("1 person on waiting list");
            } else {
                waitingListCount.setText(count + " people on waiting list");
            }

            // ---------- Badge Status ----------
            // TODO: Show badge if user is on waitlist - needs user context
            badgeStatus.setVisibility(View.GONE);

            // ---------- Category Chip ----------
            // TODO: Add category field to Event model or derive from other data
            categoryChip.setVisibility(View.GONE);

            // ---------- Poster ----------
            String posterUrl = event.getPosterUrl();
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

            // Remove poster-specific click handler since entire card is clickable
        }

        private static String safe(String s, String fallback) {
            return (s == null || s.isEmpty()) ? fallback : s;
        }
    }
}
