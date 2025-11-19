package com.example.eventmaster.ui.organizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Adapter for binding {@link EntrantRow} objects to RecyclerView list items.
 * This adapter inflates {item_person_row.xml} and binds entrant data
 * (name, email, phone) to each view holder.
 *
 * Used by {@link OrganizerEntrantsListFragment} to display either:
 *      The final enrolled entrants list (US 02.06.03)
 *      The cancelled entrants list (US 02.06.02)
 */
public class EntrantRowAdapter extends RecyclerView.Adapter<EntrantRowAdapter.VH>  {
    private final List<EntrantRow> data = new ArrayList<>();

    /**
     * Updates the adapter with a new list of entrant rows.
     * Clears any existing data and refreshes the UI.
     *
     * @param rows the new list of {@link EntrantRow} items to display;
     *             may be {@code null} or empty.
     */
    public void submit(List<EntrantRow> rows) {
        data.clear();
        if (rows != null) data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.organizer_item_person_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        EntrantRow r = data.get(pos);
        h.name.setText("Name: " + r.name);
        h.email.setText("Email: " + r.email);
        h.phone.setText("Phone: " + r.phone);
    }

    @Override public int getItemCount() { return data.size(); }


    /**
     * ViewHolder for displaying a single entrant’s information in the list.
     * Holds references to the TextViews in {@code item_person_row.xml}.
     */
    static class VH extends RecyclerView.ViewHolder {
        final TextView name, email, phone;
        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.rowName);
            email = v.findViewById(R.id.rowEmail);
            phone = v.findViewById(R.id.rowPhone);
        }
    }
}
