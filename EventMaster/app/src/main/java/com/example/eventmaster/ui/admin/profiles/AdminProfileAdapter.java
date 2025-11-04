package com.example.eventmaster.ui.admin.profiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;

import java.util.ArrayList;
import java.util.List;

public class AdminProfileAdapter extends RecyclerView.Adapter<AdminProfileAdapter.VH> {

    public interface OnRowClick { void onOpen(Profile p); }

    private final List<Profile> data = new ArrayList<>();
    private final OnRowClick onRowClick;

    public AdminProfileAdapter(List<Profile> initial, OnRowClick onRowClick) {
        if (initial != null) data.addAll(initial);
        this.onRowClick = onRowClick;
    }

    public void replace(List<Profile> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Profile p = data.get(pos);
        h.tvName.setText(ns(p.getName()));
        h.tvEmail.setText(ns(p.getEmail()));
        h.tvPhone.setText(ns(p.getPhone()));

        View.OnClickListener open = v -> { if (onRowClick != null) onRowClick.onOpen(p); };
        h.itemView.setOnClickListener(open);
        h.btnOpen.setOnClickListener(open);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar, btnOpen;
        TextView tvName, tvEmail, tvPhone;
        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatar);
            btnOpen   = v.findViewById(R.id.btnOpen);
            tvName    = v.findViewById(R.id.tvName);
            tvEmail   = v.findViewById(R.id.tvEmail);
            tvPhone   = v.findViewById(R.id.tvPhone);
        }
    }

    private String ns(String s){ return (s == null || s.trim().isEmpty()) ? "—" : s; }
}
