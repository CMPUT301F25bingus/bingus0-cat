package com.example.eventmaster.ui.admin.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.ui.admin.adapters.AdminEventListAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment displaying a list of all events for admin to browse and delete.
 * Implements US 03.04.01 - Admin can browse events.
 * Admins can only delete events, not edit or manage them.
 */
public class AdminEventListFragment extends Fragment implements AdminEventListAdapter.OnAdminEventClickListener {

    private EventRepository eventRepository;
    private AdminEventListAdapter adapter;

    private ImageView backButton;
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private MaterialButton filterButton;
    private TextView emptyStateText;

    private List<Event> allEvents = new ArrayList<>();

    public AdminEventListFragment() {
        // Required empty public constructor
    }

    public static AdminEventListFragment newInstance() {
        return new AdminEventListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize repository
        eventRepository = new EventRepositoryFs();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.admin_fragment_event_list, container, false);

        // Initialize UI elements
        backButton = view.findViewById(R.id.back_button);
        recyclerView = view.findViewById(R.id.events_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        filterButton = view.findViewById(R.id.filter_button);
        emptyStateText = view.findViewById(R.id.empty_state_text);

        // Setup back button
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearch();

        // Setup filter button
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Load events
        loadEvents();

        return view;
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        adapter = new AdminEventListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    /**
     * Sets up the search functionality with text watcher.
     */
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString(), allEvents);
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * Loads all events from Firebase.
     */
    private void loadEvents() {
        eventRepository.getAllEvents(new EventRepository.OnEventListListener() {
            @Override
            public void onSuccess(List<Event> events) {
                allEvents = events;
                adapter.setEvents(events);
                updateEmptyState();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), 
                        "Failed to load events: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    /**
     * Updates the empty state visibility based on event count.
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

    /**
     * Shows filter dialog (placeholder for future implementation).
     */
    private void showFilterDialog() {
        Toast.makeText(requireContext(), "Filter options coming soon!", Toast.LENGTH_SHORT).show();
        // TODO: Implement filter dialog
    }

    @Override
    public void onEventClick(Event event) {
        // Navigate to admin event details (read-only view)
        Intent intent = new Intent(requireContext(), com.example.eventmaster.ui.admin.activities.AdminEventDetailsActivity.class);
        intent.putExtra(com.example.eventmaster.ui.admin.activities.AdminEventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    @Override
    public void onDeleteEventClick(Event event) {
        // Show confirmation dialog before deleting
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Event?")
                .setMessage("Are you sure you want to delete this event?\n\n" + event.getName())
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent(event))
                .show();
    }

    /**
     * Deletes an event from Firebase.
     */
    private void deleteEvent(Event event) {
        eventRepository.delete(event.getEventId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), 
                            "Event deleted successfully", 
                            Toast.LENGTH_SHORT).show();
                    // Reload events to refresh the list
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), 
                            "Failed to delete event: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
}

