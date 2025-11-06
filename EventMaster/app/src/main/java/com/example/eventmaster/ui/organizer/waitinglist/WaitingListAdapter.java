package com.example.eventmaster.ui.organizer.waitinglist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.WaitingListEntry;

import java.util.List;

public class WaitingListAdapter extends RecyclerView.Adapter<WaitingListAdapter.ViewHolder> {

    private List<WaitingListEntry> entrants;

    public WaitingListAdapter(List<WaitingListEntry> entrants) {
        this.entrants = entrants;
    }

    public void updateList(List<WaitingListEntry> newEntrants) {
        this.entrants = newEntrants;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_list_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitingListEntry entrant = entrants.get(position);
        
        // Set name
        holder.textName.setText("Name: " + entrant.getEntrantName());
        
        // Set email
        holder.textEmail.setText("Email: " + entrant.getEmail());
        
        // Set phone (handle null)
        String phone = entrant.getPhone();
        if (phone != null && !phone.isEmpty()) {
            holder.textPhone.setText("Phone: " + phone);
        } else {
            holder.textPhone.setText("Phone: N/A");
        }
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

