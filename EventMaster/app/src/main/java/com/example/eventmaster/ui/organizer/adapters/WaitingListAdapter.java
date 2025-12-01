package com.example.eventmaster.ui.organizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;

import java.util.List;

/**
 * RecyclerView adapter for displaying users currently on the waiting list.
 * Each row displays the entrant’s name, email, and phone number. The adapter
 * uses profile data when available, falling back to waiting list fields if needed.
 *
 * It supports runtime updates when new entrants join or others are removed.
 */
public class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.ViewHolder> {

    private List<WaitingListEntry> entrants;

    public WaitingListAdapter(List<WaitingListEntry> entrants) {
        this.entrants = entrants;
    }

    /**
     * Replaces the list of waiting list entrants and refreshes the UI.
     *
     * @param newEntrants the updated list of WaitingListEntry objects.
     */
    public void updateList(List<WaitingListEntry> newEntrants) {
        this.entrants = newEntrants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_waiting_list_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitingListEntry entry = entrants.get(position);
        Profile p = entry.getProfile();
        //set name
        String name;
        if (p != null && p.getName() != null && !p.getName().isEmpty()) {
            name = p.getName();
        } else if (entry.getEntrantName() != null) {
            name = entry.getEntrantName();
        } else {
            name = "Unknown";
        }
        holder.textName.setText("Name: " + name);

        //set email
        String email;
        if (p != null && p.getEmail() != null && !p.getEmail().isEmpty()) {
            email = p.getEmail();
        } else if (entry.getEmail() != null) {
            email = entry.getEmail();
        } else {
            email = "N/A";
        }
        holder.textEmail.setText("Email: " + email);

        //set number if exists
        String phone;
        if (p != null && p.getPhoneNumber() != null && !p.getPhoneNumber().isEmpty()) {
            phone = p.getPhoneNumber();
        } else if (entry.getPhone() != null) {
            phone = entry.getPhone();
        } else {
            phone = "N/A";
        }
        holder.textPhone.setText("Phone: " + phone);

    }

    @Override
    public int getItemCount() {
        return entrants.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textEmail;
        TextView textPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textEntrantName);
            textEmail = itemView.findViewById(R.id.textEntrantEmail);
            textPhone = itemView.findViewById(R.id.textEntrantPhone);
        }
    }
}

