package com.example.eventmaster.ui.admin.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.admin.adapters.AdminImageListAdapter;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying a list of all images uploaded to Firebase Storage.
 * Allows admin to browse and delete images.
 */
public class AdminImageListFragment extends Fragment implements AdminImageListAdapter.OnImageDeleteListener {

    private RecyclerView recyclerView;
    private ImageView backButton;
    private TextView emptyStateText;
    private AdminImageListAdapter adapter;
    private List<StorageReference> imageRefs = new ArrayList<>();

    public AdminImageListFragment() {
        // Required empty public constructor
    }

    public static AdminImageListFragment newInstance() {
        return new AdminImageListFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.admin_fragment_image_list, container, false);

        // Initialize UI elements
        backButton = view.findViewById(R.id.back_button);
        recyclerView = view.findViewById(R.id.images_recycler_view);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup RecyclerView
        adapter = new AdminImageListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load images
        loadImages();

        return view;
    }

    /**
     * Loads all images from Firebase Storage.
     * Recursively lists all files in the events/ directory.
     */
    private void loadImages() {
        StorageReference rootRef = FirebaseStorage.getInstance().getReference();
        StorageReference eventsRef = rootRef.child("events");

        imageRefs.clear();
        adapter.setImages(new ArrayList<>());

        // List all items in events/ directory
        eventsRef.listAll()
                .addOnSuccessListener(listResult -> {
                    processListResult(listResult);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to load images: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    /**
     * Recursively processes list results to find all image files.
     */
    private void processListResult(ListResult listResult) {
        // Process files in current directory
        for (StorageReference item : listResult.getItems()) {
            String name = item.getName().toLowerCase();
            // Check if it's an image file
            if (name.endsWith(".jpg") || name.endsWith(".jpeg") || 
                name.endsWith(".png") || name.endsWith(".gif")) {
                imageRefs.add(item);
            }
        }

        // Recursively process subdirectories
        for (StorageReference prefix : listResult.getPrefixes()) {
            prefix.listAll()
                    .addOnSuccessListener(this::processListResult)
                    .addOnFailureListener(e -> {
                        // Continue processing other directories even if one fails
                    });
        }

        // Update adapter with all found images
        adapter.setImages(new ArrayList<>(imageRefs));
        updateEmptyState();
    }

    /**
     * Updates the empty state visibility based on image count.
     */
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDeleteImage(StorageReference imageRef) {
        // Show confirmation dialog before deleting
        String imagePath = imageRef.getPath();
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Image?")
                .setMessage("Are you sure you want to delete this image?\n\n" + imagePath)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteImage(imageRef))
                .show();
    }

    /**
     * Deletes an image from Firebase Storage.
     */
    private void deleteImage(StorageReference imageRef) {
        imageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(),
                            "Image deleted successfully",
                            Toast.LENGTH_SHORT).show();
                    // Reload images to refresh the list
                    loadImages();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to delete image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}

