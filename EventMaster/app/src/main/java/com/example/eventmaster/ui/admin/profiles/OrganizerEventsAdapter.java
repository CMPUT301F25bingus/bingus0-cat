package com.example.eventmaster.ui.admin.profiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that renders the list of events owned by an organizer
 * on the AdminProfileDetail screen.
 *
 * Layout contract (res/layout/item_organizer_event_row.xml):
 *  - @id/tvTitle  : TextView for the event title
 *  - @id/tvVenue  : TextView for a short "Venue — {location}" line (optional)
 */
class OrganizerEventsAdapter extends RecyclerView.Adapter<OrganizerEventsAdapter.VH> {

    private final List<Event> data = new ArrayList<>();

    void replace(List<Event> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Event e = data.get(pos);
        h.tvTitle.setText(e.getTitle() == null ? "—" : e.getTitle());
        h.tvVenue.setText(e.getLocation() == null ? "" : "Venue — " + e.getLocation());
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvVenue;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvVenue = v.findViewById(R.id.tvVenue);
        }
    }
}
