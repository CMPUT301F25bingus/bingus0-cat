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

public class ChosenListAdapter extends RecyclerView.Adapter<ChosenListAdapter.ViewHolder> {

    private List<WaitingListEntry> chosenList;

    public ChosenListAdapter(List<WaitingListEntry> chosenList) {
        this.chosenList = chosenList;
    }

    public void updateList(List<WaitingListEntry> newList) {
        this.chosenList = newList;
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
        WaitingListEntry entry = chosenList.get(position);
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
        holder.name.setText("Name: " + name);

        //set email
        String email;
        if (p != null && p.getEmail() != null && !p.getEmail().isEmpty()) {
            email = p.getEmail();
        } else if (entry.getEmail() != null) {
            email = entry.getEmail();
        } else {
            email = "N/A";
        }
        holder.email.setText("Email: " + email);

        //set number if exists
        String phone;
        if (p != null && p.getPhoneNumber() != null && !p.getPhoneNumber().isEmpty()) {
            phone = p.getPhoneNumber();
        } else if (entry.getPhone() != null) {
            phone = entry.getPhone();
        } else {
            phone = "N/A";
        }
        holder.phone.setText("Phone: " + phone);
    }

    @Override
    public int getItemCount() {
        return chosenList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, phone;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textEntrantName);
            email = itemView.findViewById(R.id.textEntrantEmail);
            phone = itemView.findViewById(R.id.textEntrantPhone);
        }
    }
}
