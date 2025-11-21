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
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        Map<String, Object> e = events.get(position);

        // TITLE
        h.txtTitle.setText(String.valueOf(e.get("title")));

        // LOCATION
        String loc = String.valueOf(e.get("location"));
        h.txtLocation.setText("📍 " + loc);

        // DATE RANGE
        h.txtDates.setText(formatDateRange(e.get("regStart"), e.get("regEnd")));

        // CAPACITY
        Object capObj = e.get("capacity");
        if (capObj != null) {
            h.txtCapacity.setText("👥 Capacity: " + capObj.toString());
        }

        // GEOLOCATION REQUIRED FLAG
        Object geoObj = e.get("geolocationRequired");
        if (geoObj instanceof Boolean && (Boolean) geoObj) {
            h.txtGeoRequired.setVisibility(View.VISIBLE);
        } else {
            h.txtGeoRequired.setVisibility(View.GONE);
        }

        // POSTER LOADING
        Object posterUrl = e.get("posterUrl");
        if (posterUrl != null) {
            Glide.with(h.itemView.getContext())
                    .load(String.valueOf(posterUrl))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(h.imgPoster);
        } else {
            h.imgPoster.setImageResource(R.drawable.ic_launcher_background);
        }

        // CLICK HANDLER
        h.itemView.setOnClickListener(v -> {
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

    // ===== Helper Methods =====

    private String extractEventId(Map<String, Object> e) {
        Object id = e.get("id");
        if (id == null) id = e.get("eventId");
        if (id == null) id = e.get("docId");
        return id == null ? null : String.valueOf(id);
    }

    private String formatDateRange(Object start, Object end) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        if (start instanceof Timestamp && end instanceof Timestamp) {
            try {
                String s = sdf.format(((Timestamp) start).toDate());
                String e2 = sdf.format(((Timestamp) end).toDate());
                return "📅 " + s + " → " + e2;
            } catch (Exception ignored) {}
        }
        return "📅 Registration: TBA";
    }

    // ===== ViewHolder =====
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle, txtLocation, txtDates, txtCapacity, txtGeoRequired;
        ImageView imgPoster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTitle       = itemView.findViewById(R.id.txtEventTitle);
            txtLocation    = itemView.findViewById(R.id.txtEventLocation);
            txtDates       = itemView.findViewById(R.id.txtEventDates);
            txtCapacity    = itemView.findViewById(R.id.txtEventCapacity);
            txtGeoRequired = itemView.findViewById(R.id.txtGeoRequired);
            imgPoster      = itemView.findViewById(R.id.imgEventPoster);
        }
    }
}
