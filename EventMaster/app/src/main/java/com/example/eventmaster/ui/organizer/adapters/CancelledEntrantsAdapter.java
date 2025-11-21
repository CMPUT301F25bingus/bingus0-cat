package com.example.eventmaster.ui.organizer.adapters;

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

public class CancelledEntrantsAdapter extends RecyclerView.Adapter<CancelledEntrantsAdapter.ViewHolder> {

    /**
     * Internal lightweight pair object to keep Profile + Status together.
     * This solves async mismatch problems without modifying Profile class.
     */
    private static class CancelledRow {
        Profile profile;
        String status;
        CancelledRow(Profile p, String s) {
            profile = p;
            status = s;
        }
    }

    // Internal unified list
    private final List<CancelledRow> rows = new ArrayList<>();

    /**
     * Update the list with profile+status pairs.
     */
    public void updateCancelledEntrants(List<Profile> profiles, List<String> statuses) {
        rows.clear();

        int size = Math.min(
                profiles != null ? profiles.size() : 0,
                statuses != null ? statuses.size() : 0
        );

        for (int i = 0; i < size; i++) {
            rows.add(new CancelledRow(profiles.get(i), statuses.get(i)));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_cancelled_entrant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CancelledRow row = rows.get(position);
        holder.bind(row.profile, row.status);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    /**
     * ViewHolder for cancelled entrant cards.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView profileImage;
        private final TextView nameText;
        private final TextView emailText;
        private final TextView phoneText;
        private final TextView cancelledByText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            profileImage = itemView.findViewById(R.id.profile_image);
            nameText = itemView.findViewById(R.id.entrant_name);
            emailText = itemView.findViewById(R.id.entrant_email);
            phoneText = itemView.findViewById(R.id.entrant_phone);
            cancelledByText = itemView.findViewById(R.id.entrant_cancelled_by);
        }

        void bind(Profile p, String status) {

            // Name
            nameText.setText("Name: " +
                    (p.getName() != null ? p.getName() : "Unknown"));

            // Email
            emailText.setText("Email: " +
                    (p.getEmail() != null ? p.getEmail() : "N/A"));

            // Phone
            if (p.getPhoneNumber() != null && !p.getPhoneNumber().isEmpty()) {
                phoneText.setVisibility(View.VISIBLE);
                phoneText.setText("Phone: " + p.getPhoneNumber());
            } else {
                phoneText.setVisibility(View.GONE);
            }

            // Cancellation reason derived ONLY from registration status
            if (status != null) {
                switch (status) {
                    case "CANCELLED_BY_ENTRANT":
                        cancelledByText.setText("Cancelled by: Entrant");
                        break;
                    case "CANCELLED_BY_ORGANIZER":
                        cancelledByText.setText("Cancelled by: Organizer");
                        break;
                    default:
                        cancelledByText.setText("Cancelled by: Unknown");
                }
                cancelledByText.setVisibility(View.VISIBLE);
            } else {
                cancelledByText.setVisibility(View.GONE);
            }

            // Default profile image (replace later if needed)
            profileImage.setImageResource(R.drawable.profile_circle);
        }
    }
}
