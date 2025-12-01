package com.example.eventmaster.ui.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter that renders the list of events owned by an organizer
 * on the AdminProfileDetail screen.
 *
 * Layout contract (res/layout/organizer_item_event_row.xml):
 *  - @id/tvTitle  : TextView for the event title
 *  - @id/tvDateLocation  : TextView for date and location
 *  - @id/tvStatus  : TextView for status badge
 */
public class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.VH> {

    private final List<Event> data = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
    private OnEventClickListener clickListener;

    public void replace(List<Event> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_event_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = data.get(pos);
        
        // Set event title
        h.tvTitle.setText(e.getName() != null && !e.getName().isEmpty() ? e.getName() : "—");
        
        // Format date and location
        String dateLocationText = formatDateLocation(e);
        h.tvDateLocation.setText(dateLocationText);
        
        // Determine if event is Ongoing or Completed
        boolean isCompleted = isEventCompleted(e);
        String status = isCompleted ? "Completed" : "Ongoing";
        h.tvStatus.setText(status);
        
        // Set status badge background color
        int bgResId = isCompleted ? R.drawable.bg_status_completed : R.drawable.bg_status_ongoing;
        h.tvStatus.setBackgroundResource(bgResId);
        
        // Set click listener on the entire item view
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null && e.getId() != null) {
                clickListener.onEventClick(e.getId());
            }
        });
    }

    /**
     * Determines if an event is completed based on its event date.
     * An event is considered completed if its event date is in the past.
     * If no event date is available, checks registration end date.
     */
    private boolean isEventCompleted(Event event) {
        Date eventDate = null;
        
        // Try to get event date first
        if (event.getEventDate() != null) {
            eventDate = event.getEventDate();
        } 
        // Fallback to registration end date if event date is not available
        else if (event.getRegistrationEndDate() != null) {
            eventDate = event.getRegistrationEndDate();
        }
        
        // If no date is available, consider it ongoing
        if (eventDate == null) {
            return false;
        }
        
        // Compare with current date
        Date now = new Date();
        return eventDate.before(now);
    }

    private String formatDateLocation(Event event) {
        StringBuilder sb = new StringBuilder();
        
        // Add date
        if (event.getEventDate() != null) {
            sb.append(dateFormat.format(event.getEventDate()));
        } else if (event.getRegistrationStartDate() != null) {
            sb.append(dateFormat.format(event.getRegistrationStartDate()));
        }
        
        // Add location
        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" • ");
            }
            sb.append(event.getLocation());
        }
        
        return sb.length() > 0 ? sb.toString() : "—";
    }

    @Override public int getItemCount() { return data.size(); }

    public interface OnEventClickListener {
        void onEventClick(String eventId);
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvDateLocation;
        final TextView tvStatus;
        
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDateLocation = v.findViewById(R.id.tvDateLocation);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}
