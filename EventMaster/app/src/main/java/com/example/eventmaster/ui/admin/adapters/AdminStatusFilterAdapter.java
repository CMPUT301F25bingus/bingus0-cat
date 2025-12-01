package com.example.eventmaster.ui.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.entrant.model.StatusFilter;

/**
 * Status filter adapter for admin event list.
 * Renders the lifecycle filter chips (All/Open/Closed/Done)
 * and forwards selection events back to the hosting fragment.
 */
public class AdminStatusFilterAdapter extends RecyclerView.Adapter<AdminStatusFilterAdapter.StatusViewHolder> {

    public interface StatusFilterListener {
        void onStatusFilterSelected(@NonNull StatusFilter filter);
    }

    private final StatusFilterListener listener;
    private StatusFilter currentFilter = StatusFilter.ALL;
    private int totalCount;
    private int openCount;
    private int closedCount;
    private int doneCount;

    public AdminStatusFilterAdapter(@NonNull StatusFilterListener listener) {
        this.listener = listener;
    }

    public void setCounts(int total, int open, int closed, int done) {
        this.totalCount = total;
        this.openCount = open;
        this.closedCount = closed;
        this.doneCount = done;
        notifyItemChanged(0);
    }

    public void setCurrentFilter(@NonNull StatusFilter filter) {
        if (currentFilter == filter) return;
        currentFilter = filter;
        notifyItemChanged(0);
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_status_filter, parent, false);
        return new StatusViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        holder.bind(currentFilter, totalCount, openCount, closedCount, doneCount);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    class StatusViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout chipAll;
        final LinearLayout chipOpen;
        final LinearLayout chipClosed;
        final LinearLayout chipDone;
        final TextView labelAll;
        final TextView labelOpen;
        final TextView labelClosed;
        final TextView labelDone;
        final TextView countAll;
        final TextView countOpen;
        final TextView countClosed;
        final TextView countDone;
        final View indicatorAll;
        final View indicatorOpen;
        final View indicatorClosed;
        final View indicatorDone;

        StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            chipAll = itemView.findViewById(R.id.status_chip_all);
            chipOpen = itemView.findViewById(R.id.status_chip_open);
            chipClosed = itemView.findViewById(R.id.status_chip_closed);
            chipDone = itemView.findViewById(R.id.status_chip_done);

            labelAll = itemView.findViewById(R.id.status_label_all);
            labelOpen = itemView.findViewById(R.id.status_label_open);
            labelClosed = itemView.findViewById(R.id.status_label_closed);
            labelDone = itemView.findViewById(R.id.status_label_done);

            countAll = itemView.findViewById(R.id.status_count_all);
            countOpen = itemView.findViewById(R.id.status_count_open);
            countClosed = itemView.findViewById(R.id.status_count_closed);
            countDone = itemView.findViewById(R.id.status_count_done);

            indicatorAll = itemView.findViewById(R.id.status_indicator_all);
            indicatorOpen = itemView.findViewById(R.id.status_indicator_open);
            indicatorClosed = itemView.findViewById(R.id.status_indicator_closed);
            indicatorDone = itemView.findViewById(R.id.status_indicator_done);

            chipAll.setOnClickListener(v -> listener.onStatusFilterSelected(StatusFilter.ALL));
            chipOpen.setOnClickListener(v -> listener.onStatusFilterSelected(StatusFilter.OPEN));
            chipClosed.setOnClickListener(v -> listener.onStatusFilterSelected(StatusFilter.CLOSED));
            chipDone.setOnClickListener(v -> listener.onStatusFilterSelected(StatusFilter.DONE));
        }

        void bind(StatusFilter filter, int total, int open, int closed, int done) {
            countAll.setText(String.valueOf(total));
            countOpen.setText(String.valueOf(open));
            countClosed.setText(String.valueOf(closed));
            countDone.setText(String.valueOf(done));

            updateChip(labelAll, countAll, indicatorAll, filter == StatusFilter.ALL);
            updateChip(labelOpen, countOpen, indicatorOpen, filter == StatusFilter.OPEN);
            updateChip(labelClosed, countClosed, indicatorClosed, filter == StatusFilter.CLOSED);
            updateChip(labelDone, countDone, indicatorDone, filter == StatusFilter.DONE);
        }

        private void updateChip(TextView label, TextView count, View indicator, boolean active) {
            int activeColor = ContextCompat.getColor(itemView.getContext(), R.color.teal_dark);
            int inactiveColor = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary_dark);
            label.setTextColor(active ? activeColor : inactiveColor);
            count.setTextColor(active ? activeColor : inactiveColor);
            indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
        }
    }
}

