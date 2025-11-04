package com.example.eventmaster.ui.organizer.enrollments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;

import java.util.ArrayList;
import java.util.List;

public class EntrantRowAdapter extends RecyclerView.Adapter<EntrantRowAdapter.VH>  {
    private final List<EntrantRow> data = new ArrayList<>();

    public void submit(List<EntrantRow> rows) {
        data.clear();
        if (rows != null) data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        EntrantRow r = data.get(pos);
        h.name.setText("Name: " + r.name);
        h.email.setText("Email: " + r.email);
        h.phone.setText("Phone: " + r.phone);
    }

    @Override public int getItemCount() { return data.size(); }

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
