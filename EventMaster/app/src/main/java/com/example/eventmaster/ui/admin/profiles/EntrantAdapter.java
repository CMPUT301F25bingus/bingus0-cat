package com.example.eventmaster.ui.admin.profiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;

import java.util.List;

public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {

    public interface OnRowOpen { void onOpen(Profile p); }
    public interface OnRowRemove { void onRemove(int position, Profile p); }

    private final List<Profile> data;
    private final OnRowOpen onOpen;
    private final OnRowRemove onRemove;

    public EntrantAdapter(List<Profile> data, OnRowOpen onOpen, OnRowRemove onRemove) {
        this.data = data;
        this.onOpen = onOpen;
        this.onRemove = onRemove;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_entrant_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Profile p = data.get(pos);
        h.tvName.setText("Name: " + ns(p.getName()));
        h.tvEmail.setText("Email: " + ns(p.getEmail()));
        h.tvPhone.setText("Phone: " + ns(p.getPhone()));

        h.itemView.setOnClickListener(v -> { if (onOpen != null) onOpen.onOpen(p); });
        h.btnRemove.setOnClickListener(v -> { if (onRemove != null) onRemove.onRemove(h.getBindingAdapterPosition(), p); });
    }

    @Override public int getItemCount() { return data.size(); }

    public void addAll(List<Profile> more) {
        int start = data.size();
        data.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    public void removeAt(int position) {
        if (position < 0 || position >= data.size()) return;
        data.remove(position);
        notifyItemRemoved(position);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvPhone, btnRemove;
        VH(@NonNull View v) {
            super(v);
            tvName    = v.findViewById(R.id.tvName);
            tvEmail   = v.findViewById(R.id.tvEmail);
            tvPhone   = v.findViewById(R.id.tvPhone);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }

    private String ns(String s) { return s == null ? "—" : s; }
}
