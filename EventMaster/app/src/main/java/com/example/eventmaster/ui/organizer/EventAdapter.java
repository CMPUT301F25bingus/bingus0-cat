package com.example.eventmaster.ui.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;

import java.util.List;
import java.util.Map;

/**
 * EventAdapter
 *
 * Role:
 *  - RecyclerView adapter for displaying event cards in the organizer view.
 *  - Binds Firestore event data (title, location, dates, poster) to UI components.
 *
 * Design Pattern:
 *  - Adapter pattern; bridges event data and RecyclerView layout.
 *
 * Outstanding Issues:
 *  - Future: add support for edit/delete event buttons.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final List<Map<String, Object>> events;

    /**
     * Creates an EventAdapter with the given list of event data.
     *
     * @param events list of maps containing event fields (title, location, etc.)
     */
    public EventAdapter(List<Map<String, Object>> events) {
        this.events = events;
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

        Object regStart = e.get("regStart");
        Object regEnd = e.get("regEnd");

        // Format registration dates
        String formattedDates = "Dates unavailable";
        if (regStart != null && regEnd != null) {
            try {
                com.google.firebase.Timestamp start = (com.google.firebase.Timestamp) regStart;
                com.google.firebase.Timestamp end = (com.google.firebase.Timestamp) regEnd;
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
                formattedDates = "From " + sdf.format(start.toDate()) + " to " + sdf.format(end.toDate());
            } catch (Exception ex) {
                formattedDates = "Invalid date";
            }
        }
        holder.txtDate.setText(formattedDates);

        // Load event poster image using Glide
        Object posterUrl = e.get("posterUrl");
        if (posterUrl != null) {
            Glide.with(holder.itemView.getContext())
                    .load(posterUrl.toString())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * ViewHolder for caching UI components of each event card.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtLocation, txtDate;
        ImageView imgPoster;
        Button btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtEventTitle);
            txtLocation = itemView.findViewById(R.id.txtEventLocation);
            txtDate = itemView.findViewById(R.id.txtEventDates);
            imgPoster = itemView.findViewById(R.id.imgEventPoster);
        }
    }
}
