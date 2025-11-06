package com.example.eventmaster.ui.entrant.invitations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.InvitationServiceFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Invitation;

import java.util.ArrayList;
import java.util.List;

public class InvitationListAdapter extends RecyclerView.Adapter<InvitationListAdapter.VH> {

    public interface CurrentUserProvider {
        String getUserId();
    }

    private final List<Invitation> data = new ArrayList<>();
    private final InvitationServiceFs invitationService = new InvitationServiceFs();
    private final RegistrationServiceFs registrationService = new RegistrationServiceFs();
    private final CurrentUserProvider userProvider;

    public InvitationListAdapter(CurrentUserProvider provider) {
        this.userProvider = provider;
    }

    public void submitList(List<Invitation> items) {
        data.clear();
        if (items != null) data.addAll(items);
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
        h.title.setText("Invitation");
        h.subtitle.setText("Event: " + inv.getEventId());

        // enable/disable by status
        boolean pending = "PENDING".equals(String.valueOf(inv.getStatus()));
        h.accept.setEnabled(pending);
        h.decline.setEnabled(pending);

        h.accept.setOnClickListener(v -> {
            h.setBusy(true);
            invitationService.accept(inv.getId(), inv.getEventId(), userProvider.getUserId(),
                    ok -> registrationService.enroll(inv.getEventId(), userProvider.getUserId(),
                            r -> {
                                h.subtitle.setText("Accepted ✅");
                                h.setBusy(false);
                            },
                            err -> {
                                h.subtitle.setText(err.getMessage());
                                h.setBusy(false);
                            }),
                    err -> {
                        h.subtitle.setText(err.getMessage());
                        h.setBusy(false);
                    });
        });

        h.decline.setOnClickListener(v -> {
            h.setBusy(true);
            invitationService.decline(inv.getId(), inv.getEventId(), userProvider.getUserId(),
                    ok -> {
                        h.subtitle.setText("Declined ❌");
                        h.setBusy(false);
                    },
                    err -> {
                        h.subtitle.setText(err.getMessage());
                        h.setBusy(false);
                    });
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        Button accept, decline;
        VH(@NonNull View v) {
            super(v);
            title   = v.findViewById(R.id.invTitle);
            subtitle= v.findViewById(R.id.invSubtitle);
            accept  = v.findViewById(R.id.btnAccept);
            decline = v.findViewById(R.id.btnDecline);
        }
        void setBusy(boolean busy) {
            accept.setEnabled(!busy);
            decline.setEnabled(!busy);
        }
    }
}
