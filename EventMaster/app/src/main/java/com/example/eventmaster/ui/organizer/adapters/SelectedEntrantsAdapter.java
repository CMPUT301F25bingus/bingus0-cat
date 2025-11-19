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

/**
 * RecyclerView adapter for displaying selected entrants in a list.
 * Each item shows profile picture, name, email, and phone number.
 * 
 * Used in SelectedEntrantsActivity to display lottery winners.
 */
public class SelectedEntrantsAdapter extends RecyclerView.Adapter<SelectedEntrantsAdapter.EntrantViewHolder> {

    private List<Profile> selectedEntrants;
    private OnEntrantClickListener clickListener;

    /**
     * Interface for handling entrant item click events.
     */
    public interface OnEntrantClickListener {
        void onEntrantClick(Profile profile);
    }

    /**
     * Creates a new adapter with an empty list.
     */
    public SelectedEntrantsAdapter() {
        this.selectedEntrants = new ArrayList<>();
    }

    /**
     * Creates a new adapter with initial data.
     * 
     * @param selectedEntrants List of selected entrant profiles
     */
    public SelectedEntrantsAdapter(List<Profile> selectedEntrants) {
        this.selectedEntrants = selectedEntrants != null ? selectedEntrants : new ArrayList<>();
    }

    /**
     * Sets the click listener for entrant items.
     * 
     * @param listener The click listener
     */
    public void setOnEntrantClickListener(OnEntrantClickListener listener) {
        this.clickListener = listener;
    }

    /**
     * Updates the list of selected entrants and refreshes the view.
     * 
     * @param newEntrants New list of selected entrants
     */
    public void updateEntrants(List<Profile> newEntrants) {
        this.selectedEntrants = newEntrants != null ? newEntrants : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Returns the current list of selected entrants.
     * 
     * @return List of profiles
     */
    public List<Profile> getSelectedEntrants() {
        return selectedEntrants;
    }

    @NonNull
    @Override
    public EntrantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_item_selected_entrant, parent, false);
        return new EntrantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntrantViewHolder holder, int position) {
        Profile profile = selectedEntrants.get(position);
        holder.bind(profile, clickListener);
    }

    @Override
    public int getItemCount() {
        return selectedEntrants.size();
    }

    /**
     * ViewHolder for individual entrant items.
     */
    static class EntrantViewHolder extends RecyclerView.ViewHolder {

        private final ImageView profileImage;
        private final TextView nameText;
        private final TextView emailText;
        private final TextView phoneText;

        /**
         * Creates a new ViewHolder.
         * 
         * @param itemView The item view
         */
        public EntrantViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            nameText = itemView.findViewById(R.id.entrant_name);
            emailText = itemView.findViewById(R.id.entrant_email);
            phoneText = itemView.findViewById(R.id.entrant_phone);
        }

        /**
         * Binds profile data to the view.
         * 
         * @param profile The profile to display
         * @param clickListener Click listener for the item
         */
        public void bind(Profile profile, OnEntrantClickListener clickListener) {
            // Set name
            String nameLabel = "Name: " + (profile.getName() != null ? profile.getName() : "Unknown");
            nameText.setText(nameLabel);

            // Set email
            String emailLabel = "Email: " + (profile.getEmail() != null ? profile.getEmail() : "No email");
            emailText.setText(emailLabel);

            // Set phone (optional field)
            if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) {
                String phoneLabel = "Phone: " + profile.getPhoneNumber();
                phoneText.setText(phoneLabel);
                phoneText.setVisibility(View.VISIBLE);
            } else {
                phoneText.setVisibility(View.GONE);
            }

            // TODO: Load profile image from URL using an image loading library (e.g., Glide, Picasso)
            // For now, using default image
            // Example with Glide (when implemented):
            // if (profile.getProfileImageUrl() != null) {
            //     Glide.with(itemView.getContext())
            //         .load(profile.getProfileImageUrl())
            //         .placeholder(R.drawable.ic_launcher_foreground)
            //         .into(profileImage);
            // }

            // Handle item clicks
            if (clickListener != null) {
                itemView.setOnClickListener(v -> clickListener.onEntrantClick(profile));
            }
        }
    }
}

