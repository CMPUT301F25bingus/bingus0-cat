package com.example.eventmaster.ui.organizer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
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
        holder.name.setText("Name: " + entry.getEntrantName());
        holder.email.setText("Email: " + entry.getEmail());
        
        // Show phone or N/A if null
        String phone = entry.getPhone();
        if (phone != null && !phone.isEmpty()) {
            holder.phone.setText("Phone: " + phone);
        } else {
            holder.phone.setText("Phone: N/A");
        }
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
