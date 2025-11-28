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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
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
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment displaying a list of all events for admin to browse and delete.
 * Implements US 03.04.01 - Admin can browse events.
 * Admins can only delete events, not edit or manage them.
 */
public class AdminEventListFragment extends Fragment
        implements AdminEventListAdapter.OnAdminEventClickListener {

    private static final String TAG = "AdminEventListFragment";
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

    // Full list of events from repo
    private List<Event> allEvents = new ArrayList<>();

    // Filter state (simplified: sort + price + availability)
    private List<Event> filteredEvents = new ArrayList<>();
    private String sortOrder = "newest";   // "newest" or "oldest"
    private Double maxPrice = null;        // null = no limit
    private boolean onlyAvailableSpots = false;

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
     * Search is applied on top of the filteredEvents list.
     */
    private void initSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op by design
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                List<Event> eventsToSearch = filteredEvents.isEmpty()
                        ? allEvents
                        : filteredEvents;

                if (query.isEmpty()) {
                    adapter.setEvents(eventsToSearch);
                } else {
                    adapter.filter(query, eventsToSearch);
                }
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
                allEvents = events != null ? events : new ArrayList<>();
                // Initialize filteredEvents with the full list
                filteredEvents = new ArrayList<>(allEvents);

                // Apply current filters (sort, price, availability)
                applyFilters();

                // Also apply any active search query on top
                CharSequence query = searchEditText != null ? searchEditText.getText() : null;
                if (query != null && query.length() > 0) {
                    adapter.filter(query.toString(), filteredEvents);
                } else {
                    adapter.setEvents(filteredEvents);
                }

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
     * Shows filter dialog with options for sorting, price, and availability.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_filter_events_admin, null);

        // Get UI elements from dialog
        RadioGroup radioGroupSort = dialogView.findViewById(R.id.radioGroupSort);
        RadioButton radioSortNewest = dialogView.findViewById(R.id.radioSortNewest);
        RadioButton radioSortOldest = dialogView.findViewById(R.id.radioSortOldest);
        SeekBar seekBarPrice = dialogView.findViewById(R.id.seekBarPrice);
        TextView textPriceValue = dialogView.findViewById(R.id.textPriceValue);
        MaterialCheckBox checkAvailableSpots = dialogView.findViewById(R.id.checkAvailableSpots);
        MaterialButton btnClearFilters = dialogView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        // If your dialog layout still has location / event type views,
        // they’ll just be unused here and can be ignored or hidden in XML.

        // Price ranges: 0=$0, 1=$50, 2=$100, 3=$150, 4=$200+
        final double[] priceRanges = {0, 50, 100, 150, 200};

        // Set current filter values
        if ("newest".equals(sortOrder)) {
            radioSortNewest.setChecked(true);
        } else {
            radioSortOldest.setChecked(true);
        }

        // Set price seekbar based on current maxPrice
        if (maxPrice != null) {
            int priceIndex = 4; // Default to $200+
            for (int i = 0; i < priceRanges.length; i++) {
                if (maxPrice <= priceRanges[i]) {
                    priceIndex = i;
                    break;
                }
            }
            seekBarPrice.setProgress(priceIndex);
        } else {
            seekBarPrice.setProgress(4); // $200+ (no limit)
        }
        updatePriceText(textPriceValue, seekBarPrice.getProgress(), priceRanges);

        checkAvailableSpots.setChecked(onlyAvailableSpots);

        // SeekBar listener to update price text
        seekBarPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePriceText(textPriceValue, progress, priceRanges);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Create dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        // Apply filters button
        btnApplyFilters.setOnClickListener(v -> {
            // Get sort order
            int selectedRadioId = radioGroupSort.getCheckedRadioButtonId();
            if (selectedRadioId == R.id.radioSortNewest) {
                sortOrder = "newest";
            } else {
                sortOrder = "oldest";
            }

            // Get price filter
            int priceProgress = seekBarPrice.getProgress();
            if (priceProgress < 4) {
                maxPrice = priceRanges[priceProgress];
            } else {
                maxPrice = null; // No limit
            }

            // Get availability filter
            onlyAvailableSpots = checkAvailableSpots.isChecked();

            // Apply filters
            applyFilters();

            // Clear search when filters are applied
            searchEditText.setText("");

            // Show filtered list
            adapter.setEvents(filteredEvents);
            updateEmptyState();

            dialog.dismiss();
        });

        // Clear filters button
        btnClearFilters.setOnClickListener(v -> {
            sortOrder = "newest";
            maxPrice = null;
            onlyAvailableSpots = false;

            // Reset dialog UI
            radioSortNewest.setChecked(true);
            seekBarPrice.setProgress(4);
            updatePriceText(textPriceValue, 4, priceRanges);
            checkAvailableSpots.setChecked(false);

            // Apply cleared filters
            applyFilters();

            // Clear search
            searchEditText.setText("");

            adapter.setEvents(filteredEvents);
            updateEmptyState();

            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Updates the price text display based on seekbar progress.
     */
    private void updatePriceText(TextView textView, int progress, double[] priceRanges) {
        if (progress >= priceRanges.length) {
            textView.setText("Max Price: $200+ (No limit)");
        } else if (progress == 0) {
            textView.setText("Max Price: Free events only ($0)");
        } else {
            textView.setText(String.format("Max Price: $%.0f", priceRanges[progress]));
        }
    }

    /**
     * Applies active filters to the event list.
     * (Sort by date, filter by price, filter by available spots)
     */
    private void applyFilters() {
        filteredEvents = new ArrayList<>(allEvents);

        // Filter by price
        if (maxPrice != null) {
            filteredEvents.removeIf(event -> event.getPrice() > maxPrice);
        }

        // Filter by available spots
        if (onlyAvailableSpots) {
            filteredEvents.removeIf(event -> event.getCapacity() <= 0);
        }

        // Sort by registration start date
        filteredEvents.sort((e1, e2) -> {
            Date date1 = e1.getRegistrationStartDate();
            Date date2 = e2.getRegistrationStartDate();

            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;

            int comparison = date1.compareTo(date2);
            return "newest".equals(sortOrder) ? -comparison : comparison;
        });
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
                    // Reload events to refresh the list (will reapply filters)
                    loadEvents();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        requireContext(),
                        TOAST_DELETE_FAILED_PREFIX + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show());
    }
}
