package com.example.eventmaster.ui.shared.adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Card-style adapter for entrant event history.
 * Displays event title, event date, status chip, and join date with optional poster.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public static class HistoryItem {
        public String eventId;
        public String title;
        public long eventDateMs;   // 0 if unknown
        public long joinedDateMs;  // 0 if unknown
        public String status;      // WAITING | SELECTED | NOT_SELECTED | CANCELLED | ENDED
        public String statusLabel; // human readable label
        public String posterUrl;
        public boolean ended;
    }

    private final List<HistoryItem> data = new ArrayList<>();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat joinedFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final OnHistoryClickListener listener;

    public interface OnHistoryClickListener {
        void onHistoryClick(@NonNull HistoryItem item);
    }

    public HistoryAdapter(OnHistoryClickListener listener) {
        this.listener = listener;
    }

    public void replace(List<HistoryItem> items) {
        data.clear();
        if (items != null) data.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shared_item_history_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        HistoryItem item = data.get(position);
        Context ctx = h.itemView.getContext();

        h.title.setText(item.title != null && !item.title.isEmpty() ? item.title : "Unnamed Event");

        if (item.eventDateMs > 0) {
            h.eventDate.setText(dateFmt.format(new Date(item.eventDateMs)));
            h.eventDate.setVisibility(View.VISIBLE);
        } else {
            h.eventDate.setVisibility(View.GONE);
        }

        // Status chip
        h.statusChip.setText(item.statusLabel != null ? item.statusLabel : item.status);
        int bgRes = R.drawable.history_chip_waiting;
        switch (item.status) {
            case "SELECTED":
                bgRes = R.drawable.history_chip_selected;
                break;
            case "NOT_SELECTED":
            case "CANCELLED":
                bgRes = R.drawable.history_chip_cancelled;
                break;
            case "ENDED":
                bgRes = R.drawable.history_chip_ended;
                break;
            default:
                bgRes = R.drawable.history_chip_waiting;
        }
        h.statusChip.setBackgroundResource(bgRes);

        if (item.joinedDateMs > 0) {
            h.joinedDate.setText("Joined " + joinedFmt.format(new Date(item.joinedDateMs)));
            h.joinedDate.setVisibility(View.VISIBLE);
        } else {
            h.joinedDate.setVisibility(View.GONE);
        }

        // Poster
        if (item.posterUrl != null && !item.posterUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(item.posterUrl)
                    .placeholder(new ColorDrawable(0xFFE0F2F1))
                    .centerCrop()
                    .into(h.poster);
        } else {
            h.poster.setImageDrawable(new ColorDrawable(0xFFE0F2F1));
        }

        // Subtle opacity if ended
        h.card.setAlpha(item.ended ? 0.82f : 1f);

        // Click to open details
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onHistoryClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        ImageView poster;
        TextView title;
        TextView eventDate;
        TextView statusChip;
        TextView joinedDate;

        VH(@NonNull View v) {
            super(v);
            card = (CardView) v;
            poster = v.findViewById(R.id.history_poster);
            title = v.findViewById(R.id.history_title);
            eventDate = v.findViewById(R.id.history_event_date);
            statusChip = v.findViewById(R.id.history_status_chip);
            joinedDate = v.findViewById(R.id.history_joined_date);
        }
    }
}
