// app/src/main/java/com/example/eventmaster/ui/admin/profiles/EntrantAdapter.java
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

/**
 * RecyclerView adapter for rendering Entrant profiles in the Admin "Browse profiles" screen.
 *
 * Responsibilities:
 *  - Binds name/email/phone and shows a "Remove" action per row.
 *  - Exposes callbacks for row open and remove so the Activity can decide behaviors.
 *
 * Layout contract (res/layout/item_admin_entrant_row.xml):
 *  - @id/imgAvatar  (ImageView) optional avatar
 *  - @id/tvName     (TextView)
 *  - @id/tvEmail    (TextView)
 *  - @id/tvPhone    (TextView)
 *  - @id/btnRemove  (TextView or Button acting as a delete control)
 */
public class EntrantAdapter extends RecyclerView.Adapter<EntrantAdapter.VH> {

    public interface OnRowOpen { void onOpen(Profile p); }
    public interface OnRowRemove { void onRemove(int position, Profile p); }

    private final List<Profile> data = new ArrayList<>();
    private final OnRowOpen onOpen;
    private final OnRowRemove onRemove;

    public EntrantAdapter(List<Profile> initial, OnRowOpen onOpen, OnRowRemove onRemove) {
        if (initial != null) data.addAll(initial);
        this.onOpen = onOpen;
        this.onRemove = onRemove;
    }

    /** Replace entire dataset (used by Firestore listener). */
    public void replace(List<Profile> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    /** Optional helpers (used by the old seeded version). */
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
        h.btnRemove.setOnClickListener(v -> {
            if (onRemove != null) onRemove.onRemove(h.getBindingAdapterPosition(), p);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvEmail, tvPhone, btnRemove;
        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatar);
            tvName    = v.findViewById(R.id.tvName);
            tvEmail   = v.findViewById(R.id.tvEmail);
            tvPhone   = v.findViewById(R.id.tvPhone);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }

    private String ns(String s) { return s == null || s.trim().isEmpty() ? "—" : s; }
}
