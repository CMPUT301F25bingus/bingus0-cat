package com.example.eventmaster.ui.entrant.invitations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Invitation;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple adapter for the Entrant's pending invitations list.
 * Exposes Accept / Decline callbacks to the hosting Fragment.
 */
public class InvitationListAdapter extends RecyclerView.Adapter<InvitationListAdapter.VH> {

    public interface Click { void onClick(Invitation inv); }

    private final Click onAccept;
    private final Click onDecline;
    private final List<Invitation> data = new ArrayList<>();

    public InvitationListAdapter(Click onAccept, Click onDecline) {
        this.onAccept = onAccept;
        this.onDecline = onDecline;
    }

    public void submitList(List<Invitation> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_invitation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Invitation inv = data.get(position);

        // Title/subtitle — adjust to your Event model later if you want more info
        h.title.setText("Invitation • Event " + inv.getEventId());
        h.subtitle.setText("For entrant: " + inv.getEntrantId());

        h.btnAccept.setOnClickListener(v -> onAccept.onClick(inv));
        h.btnDecline.setOnClickListener(v -> onDecline.onClick(inv));
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Button btnAccept, btnDecline;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.invTitle);
            subtitle = v.findViewById(R.id.invSubtitle);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}