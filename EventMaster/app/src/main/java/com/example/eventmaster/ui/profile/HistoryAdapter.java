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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public static class Row {
        public final Registration reg;
        public final String eventTitle;
        public Row(Registration reg, String eventTitle) { this.reg = reg; this.eventTitle = eventTitle; }
    }

    private final List<Row> data = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public void replace(List<Row> rows) {
        data.clear();
        if (rows != null) data.addAll(rows);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_history_row, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Row row = data.get(pos);
        h.tvEventTitle.setText(row.eventTitle == null ? "(Untitled event)" : row.eventTitle);

        RegistrationStatus st = row.reg.getStatus();
        h.tvStatus.setText(st.name());
        // simple styling by status
        if (st == RegistrationStatus.ACTIVE) {
            h.tvStatus.setBackgroundColor(0xFFE6F7EE); // light green
            h.tvStatus.setTextColor(0xFF0F8A4B);
        } else {
            h.tvStatus.setBackgroundColor(0xFFFDEEEE); // light red
            h.tvStatus.setTextColor(0xFFD5483A);
        }

        h.tvJoined.setText("Joined: " + fmt.format(new Date(row.reg.getCreatedAtUtc())));
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEventTitle, tvStatus, tvJoined;
        VH(@NonNull View v) {
            super(v);
            tvEventTitle = v.findViewById(R.id.tvEventTitle);
            tvStatus     = v.findViewById(R.id.tvStatus);
            tvJoined     = v.findViewById(R.id.tvJoined);
        }
    }
}
