package com.example.eventmaster.ui.organizer.adapters;

import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EventAdapter - Simplified version showing only event name and waiting list count
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull String eventId);
    }

    private final List<Map<String, Object>> events;
    private OnEventClickListener clickListener;
    private final Map<String, Integer> waitingListCounts = new HashMap<>();

    public EventAdapter(List<Map<String, Object>> events) {
        this.events = events;
    }

    public void setOnEventClickListener(OnEventClickListener l) {
        this.clickListener = l;
    }

    /**
     * Sets waiting list counts for events.
     */
    public void setWaitingListCounts(Map<String, Integer> counts) {
        waitingListCounts.clear();
        if (counts != null) {
            waitingListCounts.putAll(counts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_event_card_simple, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Map<String, Object> e = events.get(position);
        String eventId = extractEventId(e);
        String title = String.valueOf(e.get("title") != null ? e.get("title") : "Unnamed Event");

        // Set title
        h.eventTitle.setText(title);

        // Set date
        Object eventDateObj = e.get("eventDate");
        if (eventDateObj instanceof Timestamp) {
            Timestamp eventDate = (Timestamp) eventDateObj;
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            h.eventDate.setText(dateFormat.format(eventDate.toDate()));
            h.eventDate.setVisibility(View.VISIBLE);
        } else {
            // Fallback to registration end date
            Object regEndObj = e.get("regEnd");
            if (regEndObj instanceof Timestamp) {
                Timestamp regEnd = (Timestamp) regEndObj;
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                h.eventDate.setText(dateFormat.format(regEnd.toDate()));
                h.eventDate.setVisibility(View.VISIBLE);
            } else {
                h.eventDate.setVisibility(View.GONE);
            }
        }

        // Set status badge (Live if event hasn't ended)
        long now = System.currentTimeMillis();
        boolean isLive = false;
        Object eventDateObj2 = e.get("eventDate");
        if (eventDateObj2 instanceof Timestamp) {
            Timestamp eventDate = (Timestamp) eventDateObj2;
            isLive = eventDate.toDate().getTime() >= now;
        } else {
            Object regEndObj = e.get("regEnd");
            if (regEndObj instanceof Timestamp) {
                Timestamp regEnd = (Timestamp) regEndObj;
                isLive = regEnd.toDate().getTime() >= now;
            }
        }
        
        if (isLive) {
            h.statusBadge.setVisibility(View.VISIBLE);
            h.statusBadge.setText("Live");
        } else {
            h.statusBadge.setVisibility(View.GONE);
        }

        // Set waiting list count
        int count = waitingListCounts.getOrDefault(eventId, 0);
        h.waitingList.setText(count + " waitlisted");

        // Load poster
        int placeholderColor = ContextCompat.getColor(h.itemView.getContext(), R.color.org_card_tint);
        Object posterUrl = e.get("posterUrl");
        if (posterUrl != null && !posterUrl.toString().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(posterUrl.toString())
                    .placeholder(new ColorDrawable(placeholderColor))
                    .centerCrop()
                    .into(h.poster);
        } else {
            h.poster.setImageDrawable(new ColorDrawable(placeholderColor));
        }

        // Click handler
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null && eventId != null && !eventId.isEmpty()) {
                clickListener.onEventClick(eventId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private String extractEventId(Map<String, Object> e) {
        Object id = e.get("id");
        if (id == null) id = e.get("eventId");
        if (id == null) id = e.get("docId");
        return id == null ? null : String.valueOf(id);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle;
        TextView eventDate;
        TextView waitingList;
        TextView statusBadge;
        ImageView poster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            waitingList = itemView.findViewById(R.id.event_waiting_list);
            statusBadge = itemView.findViewById(R.id.event_status_badge);
            poster = itemView.findViewById(R.id.event_poster);
        }
    }
}
