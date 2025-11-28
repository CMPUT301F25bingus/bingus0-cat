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
public class AdminEventListFragment extends Fragment
        implements AdminEventListAdapter.OnAdminEventClickListener {

    private static final String TAG = "AdminEventListFragment";
    private static final String TOAST_FILTER_COMING_SOON = "Filter options coming soon!";
    private static final String TOAST_DELETE_SUCCESS = "Event deleted successfully";
    private static final String TOAST_DELETE_FAILED_PREFIX = "Failed to delete event: ";
    private static final String TOAST_LOAD_FAILED_PREFIX = "Failed to load events: ";
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

        // Initialize repository once for the fragment lifecycle
        eventRepository = new EventRepositoryFs();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        final View root = inflater.inflate(
                R.layout.admin_fragment_event_list,
                container,
                false
        );

        // Wire UI references
        initViews(root);

        // Configure UI behavior and data bindings
        initRecyclerView();
        initSearch();
        initBackButton();
        initFilterButton();

        // Initial data load
        loadEvents();

        return root;
    }

    /**
     * Finds and assigns all required views from the layout.
     */
    private void initViews(@NonNull View root) {
        backButton = root.findViewById(R.id.back_button);
        recyclerView = root.findViewById(R.id.events_recycler_view);
        searchEditText = root.findViewById(R.id.search_edit_text);
        filterButton = root.findViewById(R.id.filter_button);
        emptyStateText = root.findViewById(R.id.empty_state_text);
    }

    /**
     * Sets up the RecyclerView with its adapter and layout manager.
     */
    private void initRecyclerView() {
        adapter = new AdminEventListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    /**
     * Sets up the search EditText with a TextWatcher.
     */
    private void initSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op by design
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Keep same behavior: filter adapter and update empty state
                adapter.filter(s.toString(), allEvents);
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op by design
            }
        });
    }

    /**
     * Initializes the back button behavior to delegate back navigation to activity.
     */
    private void initBackButton() {
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    /**
     * Initializes the filter button click listener.
     */
    private void initFilterButton() {
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    /**
     * Loads all events from the repository and updates the list.
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
                Toast.makeText(
                        requireContext(),
                        TOAST_LOAD_FAILED_PREFIX + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                updateEmptyState();
            }
        });
    }

    /**
     * Toggles between the empty state text and the RecyclerView
     * depending on whether there are items to show.
     */
    private void updateEmptyState() {
        final boolean isEmpty = (adapter == null || adapter.getItemCount() == 0);

        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }


    /**
     * Placeholder for future filter functionality.
     * Currently shows a Toast only.
     */
    private void showFilterDialog() {
        Toast.makeText(
                requireContext(),
                TOAST_FILTER_COMING_SOON,
                Toast.LENGTH_SHORT
        ).show();
        // TODO: Implement filter dialog
    }

    @Override
    public void onEventClick(Event event) {
        // Navigate to admin event details (read-only view)
        Intent intent = new Intent(
                requireContext(),
                com.example.eventmaster.ui.admin.activities.AdminEventDetailsActivity.class
        );
        intent.putExtra(
                com.example.eventmaster.ui.admin.activities.AdminEventDetailsActivity.EXTRA_EVENT_ID,
                event.getEventId()
        );
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
     * Deletes an event from the repository and refreshes the list.
     */
    private void deleteEvent(Event event) {
        eventRepository.delete(event.getEventId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(
                            requireContext(),
                            TOAST_DELETE_SUCCESS,
                            Toast.LENGTH_SHORT
                    ).show();
                    // Reload events to refresh the list
                    loadEvents();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        TOAST_DELETE_FAILED_PREFIX + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
