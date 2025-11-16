package com.example.eventmaster.ui.organizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EventAdapter
 *
 * - RecyclerView adapter for the organizer's event list.
 * - Binds title, location, dates, and poster; exposes item clicks via OnEventClickListener.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull String eventId);
    }

    private final List<Map<String, Object>> events;
    private OnEventClickListener clickListener;

    public EventAdapter(List<Map<String, Object>> events) {
        this.events = events;
    }

    public void setOnEventClickListener(OnEventClickListener l) {
        this.clickListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_event_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> e = events.get(position);

        holder.txtTitle.setText(String.valueOf(e.get("title")));
        holder.txtLocation.setText(String.valueOf(e.get("location")));
        holder.txtDate.setText(formatDateRange(e.get("regStart"), e.get("regEnd")));

        Object posterUrl = e.get("posterUrl");
        if (posterUrl != null) {
            Glide.with(holder.itemView.getContext())
                    .load(String.valueOf(posterUrl))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener == null) return;
            String eventId = extractEventId(e);
            if (eventId != null && !eventId.isEmpty()) {
                clickListener.onEventClick(eventId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /** Accepts common id keys: "id", "eventId", "docId". */
    private String extractEventId(Map<String, Object> e) {
        Object id = e.get("id");
        if (id == null) id = e.get("eventId");
        if (id == null) id = e.get("docId");
        return id == null ? null : String.valueOf(id);
    }

    /** Formats "From MMM d, yyyy to MMM d, yyyy" if both ends present; otherwise shows fallback text. */
    private String formatDateRange(Object start, Object end) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        if (start instanceof Timestamp && end instanceof Timestamp) {
            try {
                return "From " + sdf.format(((Timestamp) start).toDate())
                        + " to " + sdf.format(((Timestamp) end).toDate());
            } catch (Exception ignored) {}
        }
        return "Dates unavailable";
    }

    /** ViewHolder cache. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtTitle, txtLocation, txtDate;
        final ImageView imgPoster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle    = itemView.findViewById(R.id.txtEventTitle);
            txtLocation = itemView.findViewById(R.id.txtEventLocation);
            txtDate     = itemView.findViewById(R.id.txtEventDates);
            imgPoster   = itemView.findViewById(R.id.imgEventPoster);
        }
    }
}
