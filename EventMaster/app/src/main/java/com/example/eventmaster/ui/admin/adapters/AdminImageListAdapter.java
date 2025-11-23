package com.example.eventmaster.ui.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying images in admin view.
 * Shows image thumbnails with delete functionality.
 */
public class AdminImageListAdapter extends RecyclerView.Adapter<AdminImageListAdapter.ImageViewHolder> {

    private List<StorageReference> images;
    private final OnImageDeleteListener listener;

    /**
     * Interface for handling image delete actions.
     */
    public interface OnImageDeleteListener {
        void onDeleteImage(StorageReference imageRef);
    }

    public AdminImageListAdapter(OnImageDeleteListener listener) {
        this.images = new ArrayList<>();
        this.listener = listener;
    }

    /**
     * Updates the list of images and refreshes the RecyclerView.
     */
    public void setImages(List<StorageReference> newImages) {
        this.images = newImages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.admin_item_image_card, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        StorageReference imageRef = images.get(position);
        holder.bind(imageRef, listener);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * ViewHolder for image items.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView imageThumbnail;
        private final TextView imagePath;
        private final MaterialButton deleteButton;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageThumbnail = itemView.findViewById(R.id.image_thumbnail);
            imagePath = itemView.findViewById(R.id.image_path);
            deleteButton = itemView.findViewById(R.id.delete_image_button);
        }

        public void bind(StorageReference imageRef, OnImageDeleteListener listener) {
            // Set image path
            String path = imageRef.getPath();
            imagePath.setText(path);

            // Load image using Glide
            imageRef.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        Glide.with(itemView.getContext())
                                .load(uri)
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .error(R.drawable.ic_avatar_placeholder)
                                .centerCrop()
                                .into(imageThumbnail);
                    })
                    .addOnFailureListener(e -> {
                        // If download URL fails, show placeholder
                        imageThumbnail.setImageResource(R.drawable.ic_avatar_placeholder);
                    });

            // Set delete button click listener
            deleteButton.setOnClickListener(v -> listener.onDeleteImage(imageRef));
        }
    }
}

