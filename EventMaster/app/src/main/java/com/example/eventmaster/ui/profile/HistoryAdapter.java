package com.example.eventmaster.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.model.RegistrationStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders the "Event history" rows on Profile screen.
 * Expects layout item_profile_history_row with ids: tvTitle, tvStatusChip, tvDate.
 * Uses chip drawables: chip_history_active, chip_history_cancelled, chip_history_not_selected.
 */
class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    static class Row {
        final Registration reg;
        final String eventTitle;
        Row(Registration r, String title){ this.reg = r; this.eventTitle = title; }
    }

    private final List<Row> data = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    void replace(List<Row> rows){
        data.clear();
        if (rows != null) data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_history_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Row row = data.get(position);

        String title = (row.eventTitle == null || row.eventTitle.trim().isEmpty())
                ? "—" : row.eventTitle;
        h.tvTitle.setText(title);

        RegistrationStatus st = row.reg.getStatus();
        if (st == RegistrationStatus.ACTIVE) {
            h.tvStatusChip.setText("Selected");
            h.tvStatusChip.setBackgroundResource(R.drawable.chip_history_active);
        } else if (st == RegistrationStatus.CANCELLED_BY_ORGANIZER
                || st == RegistrationStatus.CANCELLED_BY_ENTRANT) {
            h.tvStatusChip.setText("Cancelled");
            h.tvStatusChip.setBackgroundResource(R.drawable.chip_history_cancelled);
        } else if (st == RegistrationStatus.NOT_SELECTED) {
            h.tvStatusChip.setText("Not Selected");
            h.tvStatusChip.setBackgroundResource(R.drawable.chip_history_not_selected);
        }

        String dateText = "Date: " + fmt.format(row.reg.getCreatedAtUtc());
        h.tvDate.setText(dateText);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvStatusChip, tvDate;
        VH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvStatusChip = v.findViewById(R.id.tvStatusChip);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
