package com.example.eventmaster.ui.admin.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.ui.admin.adapters.AdminEventListAdapter;
import com.example.eventmaster.ui.admin.adapters.AdminStatusFilterAdapter;
import com.example.eventmaster.ui.entrant.model.StatusFilter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fragment displaying a list of all events for admin to browse and delete.
 * Implements US 03.04.01 - Admin can browse events.
 * Admins can only delete events, not edit or manage them.
 */
public class AdminEventListFragment extends Fragment implements AdminEventListAdapter.OnAdminEventClickListener {

    private EventRepository eventRepository;
    private AdminEventListAdapter adapter;
    private AdminStatusFilterAdapter statusFilterAdapter;
    private ConcatAdapter concatAdapter;

    private ImageView backButton;
    private RecyclerView recyclerView;
    private EditText searchEditText;
    private MaterialButton filterButton;
    private TextView emptyStateText;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> filteredEvents = new ArrayList<>();
    private List<Event> statusFilteredEvents = new ArrayList<>();
    private StatusFilter currentStatusFilter = StatusFilter.ALL;
    
    // Filter state variables
    private String sortOrder = "newest"; // "newest" or "oldest"
    private Double maxPrice = null; // null means no price limit
    private String locationFilter = null;
    private List<String> selectedEventTypes = new ArrayList<>(); // List of selected event types for filtering
    
    private static final String TAG = "AdminEventListFragment";
    
    private enum EventLifecycleState {
        OPEN, CLOSED, DONE
    }

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
        statusFilterAdapter = new AdminStatusFilterAdapter(this::selectStatusFilter);
        concatAdapter = new ConcatAdapter(statusFilterAdapter, adapter);
        recyclerView.setAdapter(concatAdapter);
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
                String query = s.toString().trim();
                List<Event> eventsToSearch = statusFilteredEvents.isEmpty()
                        ? filteredEvents
                        : statusFilteredEvents;

                if (query.isEmpty()) {
                    adapter.setEvents(eventsToSearch);
                } else {
                    adapter.filter(query, eventsToSearch);
                }
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
                filteredEvents = new ArrayList<>(allEvents);
                statusFilteredEvents = new ArrayList<>();
                applyFilters();
                updateStatusCounts();
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
     * Always keeps RecyclerView visible so status filter bar stays visible.
     */
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.GONE);
        }
        // Always keep RecyclerView visible so status filter bar stays visible
        recyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * Handles status filter selection.
     */
    private void selectStatusFilter(StatusFilter newFilter) {
        if (currentStatusFilter == newFilter) {
            return;
        }
        currentStatusFilter = newFilter;
        if (statusFilterAdapter != null) {
            statusFilterAdapter.setCurrentFilter(currentStatusFilter);
        }
        applyStatusFilterAndRefresh();
    }

    /**
     * Applies the current status filter and refreshes the event list.
     */
    private void applyStatusFilterAndRefresh() {
        statusFilteredEvents = new ArrayList<>();
        Date now = new Date();
        for (Event event : filteredEvents) {
            EventLifecycleState state = resolveLifecycleState(event, now);
            if (matchesCurrentFilter(state)) {
                statusFilteredEvents.add(event);
            }
        }

        CharSequence query = searchEditText != null ? searchEditText.getText() : null;
        if (query != null && query.length() > 0) {
            adapter.filter(query.toString(), statusFilteredEvents);
        } else {
            adapter.setEvents(statusFilteredEvents);
        }
        updateEmptyState();
    }

    /**
     * Checks if an event matches the current filter.
     */
    private boolean matchesCurrentFilter(EventLifecycleState state) {
        if (currentStatusFilter == StatusFilter.ALL) return true;
        if (currentStatusFilter == StatusFilter.OPEN && state == EventLifecycleState.OPEN) return true;
        if (currentStatusFilter == StatusFilter.CLOSED && state == EventLifecycleState.CLOSED) return true;
        return currentStatusFilter == StatusFilter.DONE && state == EventLifecycleState.DONE;
    }

    /**
     * Resolves the lifecycle state of an event based on dates.
     */
    private EventLifecycleState resolveLifecycleState(Event event, Date referenceDate) {
        if (event == null) return EventLifecycleState.OPEN;

        String status = event.getStatus();
        if (status != null) {
            String normalized = status.trim().toUpperCase();
            if ("DONE".equals(normalized) || "COMPLETED".equals(normalized)) {
                return EventLifecycleState.DONE;
            }
            if ("CLOSED".equals(normalized)) {
                return EventLifecycleState.CLOSED;
            }
        }

        Date eventDate = event.getEventDate();
        if (eventDate != null && !eventDate.after(referenceDate)) {
            return EventLifecycleState.DONE;
        }

        Date regEnd = event.getRegistrationEndDate();
        if (regEnd != null && regEnd.before(referenceDate)) {
            return EventLifecycleState.CLOSED;
        }

        return EventLifecycleState.OPEN;
    }

    /**
     * Updates the status filter counts.
     */
    private void updateStatusCounts() {
        if (statusFilterAdapter == null) return;

        // Count from filteredEvents instead of allEvents to reflect active filters
        List<Event> eventsToCount = (filteredEvents != null && !filteredEvents.isEmpty()) 
            ? filteredEvents 
            : (allEvents != null ? allEvents : new ArrayList<>());

        int total = eventsToCount.size();
        int open = 0;
        int closed = 0;
        int done = 0;
        Date now = new Date();

        for (Event event : eventsToCount) {
            EventLifecycleState state = resolveLifecycleState(event, now);
            switch (state) {
                case DONE:
                    done++;
                    break;
                case CLOSED:
                    closed++;
                    break;
                case OPEN:
                default:
                    open++;
                    break;
            }
        }

        statusFilterAdapter.setCounts(total, open, closed, done);
    }

    /**
     * Shows filter dialog with options for sorting and filtering events.
     * Implements filtering by type, price, location, and sorting by date.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_filter_events, null);

        // Get UI elements from dialog
        RadioGroup radioGroupSort = dialogView.findViewById(R.id.radioGroupSort);
        RadioButton radioSortNewest = dialogView.findViewById(R.id.radioSortNewest);
        RadioButton radioSortOldest = dialogView.findViewById(R.id.radioSortOldest);
        SeekBar seekBarPrice = dialogView.findViewById(R.id.seekBarPrice);
        TextView textPriceValue = dialogView.findViewById(R.id.textPriceValue);
        TextInputEditText editLocation = dialogView.findViewById(R.id.editLocation);
        com.google.android.material.textfield.TextInputEditText autoCompleteEventType = dialogView.findViewById(R.id.autoCompleteEventType);
        androidx.cardview.widget.CardView layoutEventTypes = dialogView.findViewById(R.id.layoutEventTypes);
        MaterialButton btnClearFilters = dialogView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);
        
        // Get all event type checkboxes
        MaterialCheckBox checkTypeSports = dialogView.findViewById(R.id.checkTypeSports);
        MaterialCheckBox checkTypeFood = dialogView.findViewById(R.id.checkTypeFood);
        MaterialCheckBox checkTypeMusic = dialogView.findViewById(R.id.checkTypeMusic);
        MaterialCheckBox checkTypeEducation = dialogView.findViewById(R.id.checkTypeEducation);
        MaterialCheckBox checkTypeWorkshop = dialogView.findViewById(R.id.checkTypeWorkshop);
        MaterialCheckBox checkTypeVolunteer = dialogView.findViewById(R.id.checkTypeVolunteer);
        MaterialCheckBox checkTypeSocial = dialogView.findViewById(R.id.checkTypeSocial);
        MaterialCheckBox checkTypeFitness = dialogView.findViewById(R.id.checkTypeFitness);
        MaterialCheckBox checkTypeFamily = dialogView.findViewById(R.id.checkTypeFamily);
        MaterialCheckBox checkTypeArtsCulture = dialogView.findViewById(R.id.checkTypeArtsCulture);
        MaterialCheckBox checkTypeOther = dialogView.findViewById(R.id.checkTypeOther);

        // Price ranges: 0=$0, 1=$50, 2=$100, 3=$150, 4=$200+
        final double[] priceRanges = {0, 50, 100, 150, 200};

        // Set current filter values
        if (sortOrder.equals("newest")) {
            radioSortNewest.setChecked(true);
        } else {
            radioSortOldest.setChecked(true);
        }

        // Set price seekbar
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

        if (locationFilter != null) {
            editLocation.setText(locationFilter);
        }

        // SeekBar listener to update price text
        seekBarPrice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePriceText(textPriceValue, progress, priceRanges);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Helper method to update field text based on selected checkboxes
        Runnable updateFieldText = () -> {
            List<String> selected = new ArrayList<>();
            if (checkTypeSports.isChecked()) selected.add("Sports");
            if (checkTypeFood.isChecked()) selected.add("Food");
            if (checkTypeMusic.isChecked()) selected.add("Music");
            if (checkTypeEducation.isChecked()) selected.add("Education");
            if (checkTypeWorkshop.isChecked()) selected.add("Workshop");
            if (checkTypeVolunteer.isChecked()) selected.add("Volunteer");
            if (checkTypeSocial.isChecked()) selected.add("Social");
            if (checkTypeFitness.isChecked()) selected.add("Fitness");
            if (checkTypeFamily.isChecked()) selected.add("Family");
            if (checkTypeArtsCulture.isChecked()) selected.add("Arts & Culture");
            if (checkTypeOther.isChecked()) selected.add("Other");
            
            if (selected.isEmpty()) {
                autoCompleteEventType.setText("All types");
            } else if (selected.size() == 1) {
                autoCompleteEventType.setText(selected.get(0));
            } else {
                autoCompleteEventType.setText(selected.size() + " types selected");
            }
        };

        // Restore selected event types to checkboxes
        if (!selectedEventTypes.isEmpty()) {
            layoutEventTypes.setVisibility(View.VISIBLE);
            
            checkTypeSports.setChecked(selectedEventTypes.contains("Sports"));
            checkTypeFood.setChecked(selectedEventTypes.contains("Food"));
            checkTypeMusic.setChecked(selectedEventTypes.contains("Music"));
            checkTypeEducation.setChecked(selectedEventTypes.contains("Education"));
            checkTypeWorkshop.setChecked(selectedEventTypes.contains("Workshop"));
            checkTypeVolunteer.setChecked(selectedEventTypes.contains("Volunteer"));
            checkTypeSocial.setChecked(selectedEventTypes.contains("Social"));
            checkTypeFitness.setChecked(selectedEventTypes.contains("Fitness"));
            checkTypeFamily.setChecked(selectedEventTypes.contains("Family"));
            checkTypeArtsCulture.setChecked(selectedEventTypes.contains("Arts & Culture"));
            checkTypeOther.setChecked(selectedEventTypes.contains("Other"));
        }
        
        // Initialize field text
        updateFieldText.run();

        // Toggle checkbox visibility when clicking the event type field
        autoCompleteEventType.setOnClickListener(v -> {
            if (layoutEventTypes.getVisibility() == View.VISIBLE) {
                layoutEventTypes.setVisibility(View.GONE);
            } else {
                layoutEventTypes.setVisibility(View.VISIBLE);
            }
        });

        // Update field text when checkboxes change
        View.OnClickListener checkboxListener = v -> updateFieldText.run();

        checkTypeSports.setOnClickListener(checkboxListener);
        checkTypeFood.setOnClickListener(checkboxListener);
        checkTypeMusic.setOnClickListener(checkboxListener);
        checkTypeEducation.setOnClickListener(checkboxListener);
        checkTypeWorkshop.setOnClickListener(checkboxListener);
        checkTypeVolunteer.setOnClickListener(checkboxListener);
        checkTypeSocial.setOnClickListener(checkboxListener);
        checkTypeFitness.setOnClickListener(checkboxListener);
        checkTypeFamily.setOnClickListener(checkboxListener);
        checkTypeArtsCulture.setOnClickListener(checkboxListener);
        checkTypeOther.setOnClickListener(checkboxListener);

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

            // Get location filter
            String locationText = editLocation.getText() != null
                ? editLocation.getText().toString().trim()
                : "";
            locationFilter = locationText.isEmpty() ? null : locationText;

            // Get selected event types from checkboxes
            selectedEventTypes.clear();
            if (checkTypeSports.isChecked()) selectedEventTypes.add("Sports");
            if (checkTypeFood.isChecked()) selectedEventTypes.add("Food");
            if (checkTypeMusic.isChecked()) selectedEventTypes.add("Music");
            if (checkTypeEducation.isChecked()) selectedEventTypes.add("Education");
            if (checkTypeWorkshop.isChecked()) selectedEventTypes.add("Workshop");
            if (checkTypeVolunteer.isChecked()) selectedEventTypes.add("Volunteer");
            if (checkTypeSocial.isChecked()) selectedEventTypes.add("Social");
            if (checkTypeFitness.isChecked()) selectedEventTypes.add("Fitness");
            if (checkTypeFamily.isChecked()) selectedEventTypes.add("Family");
            if (checkTypeArtsCulture.isChecked()) selectedEventTypes.add("Arts & Culture");
            if (checkTypeOther.isChecked()) selectedEventTypes.add("Other");

            // Apply filters
            applyFilters();

            // Clear search when filters are applied
            searchEditText.setText("");

            dialog.dismiss();
        });

        // Clear filters button
        btnClearFilters.setOnClickListener(v -> {
            sortOrder = "newest";
            maxPrice = null;
            locationFilter = null;
            selectedEventTypes.clear();

            // Reset dialog UI
            radioSortNewest.setChecked(true);
            seekBarPrice.setProgress(4);
            updatePriceText(textPriceValue, 4, priceRanges);
            editLocation.setText("");
            autoCompleteEventType.setText("All types");
            layoutEventTypes.setVisibility(View.GONE);
            
            // Clear all event type checkboxes
            checkTypeSports.setChecked(false);
            checkTypeFood.setChecked(false);
            checkTypeMusic.setChecked(false);
            checkTypeEducation.setChecked(false);
            checkTypeWorkshop.setChecked(false);
            checkTypeVolunteer.setChecked(false);
            checkTypeSocial.setChecked(false);
            checkTypeFitness.setChecked(false);
            checkTypeFamily.setChecked(false);
            checkTypeArtsCulture.setChecked(false);
            checkTypeOther.setChecked(false);

            // Apply cleared filters
            applyFilters();

            // Clear search
            searchEditText.setText("");

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
     */
    private void applyFilters() {
        filteredEvents = new ArrayList<>(allEvents);

        // Filter by price
        if (maxPrice != null) {
            filteredEvents.removeIf(event -> event.getPrice() > maxPrice);
        }

        // Filter by location
        if (locationFilter != null && !locationFilter.isEmpty()) {
            String locationLower = locationFilter.toLowerCase();
            filteredEvents.removeIf(event ->
                event.getLocation() == null ||
                !event.getLocation().toLowerCase().contains(locationLower)
            );
        }

        // Filter by event types
        if (selectedEventTypes != null && !selectedEventTypes.isEmpty()) {
            filteredEvents.removeIf(event -> {
                String eventType = event.getEventType();
                if (eventType == null || eventType.isEmpty()) {
                    return true; // Remove events with no type if filtering by type
                }
                return !selectedEventTypes.contains(eventType);
            });
        }

        // Sort by date
        filteredEvents.sort((e1, e2) -> {
            Date date1 = e1.getRegistrationStartDate();
            Date date2 = e2.getRegistrationStartDate();

            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;

            int comparison = date1.compareTo(date2);
            return sortOrder.equals("newest") ? -comparison : comparison;
        });

        updateStatusCounts();
        applyStatusFilterAndRefresh();

        Log.d(TAG, "Applied filters - showing " + filteredEvents.size() + " of " + allEvents.size() + " events before status filter");
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
                    // Reload events to refresh the list and maintain filter state
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), 
                            "Failed to delete event: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }
}

