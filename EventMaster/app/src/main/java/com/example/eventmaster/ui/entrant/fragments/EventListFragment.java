package com.example.eventmaster.ui.entrant.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.entrant.activities.EntrantHistoryActivity;
import com.example.eventmaster.ui.entrant.activities.EntrantNotificationsActivity;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.example.eventmaster.ui.entrant.adapters.EventListAdapter;
import com.example.eventmaster.ui.entrant.adapters.StatusFilterAdapter;
import com.example.eventmaster.ui.entrant.model.StatusFilter;
import com.example.eventmaster.ui.shared.activities.ProfileActivity;
import com.example.eventmaster.ui.shared.activities.QRScannerActivity;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Fragment displaying a list of all available events.
 * Implements US 01.01.03 - View list of all available events.
 */
public class EventListFragment extends Fragment implements EventListAdapter.OnEventClickListener {

    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private EventListAdapter adapter;
    private String userId;

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private MaterialButton filterButton;
    private ImageButton qrScannerButton;
    private TextView emptyStateText;
    private BottomNavigationView bottomNavigationView;
    private StatusFilterAdapter statusFilterAdapter;
    private ConcatAdapter concatAdapter;

    private List<Event> allEvents = new ArrayList<>();
    
    // Filter state variables
    private List<Event> filteredEvents = new ArrayList<>();
    private List<Event> statusFilteredEvents = new ArrayList<>();
    private String sortOrder = "newest"; // "newest" or "oldest"
    private Double maxPrice = null; // null means no price limit
    private String locationFilter = null;
    private boolean onlyAvailableSpots = false;
    private StatusFilter currentStatusFilter = StatusFilter.ALL;
    
    private static final String TAG = "EventListFragment";

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

    private void applyStatusFilterAndRefresh() {
        if (filteredEvents == null) {
            filteredEvents = new ArrayList<>();
        }
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

    private boolean matchesCurrentFilter(EventLifecycleState state) {
        if (currentStatusFilter == StatusFilter.ALL) return true;
        if (currentStatusFilter == StatusFilter.OPEN && state == EventLifecycleState.OPEN) return true;
        if (currentStatusFilter == StatusFilter.CLOSED && state == EventLifecycleState.CLOSED) return true;
        return currentStatusFilter == StatusFilter.DONE && state == EventLifecycleState.DONE;
    }

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

    private void updateStatusCounts() {
        if (statusFilterAdapter == null) return;
        int total = allEvents != null ? allEvents.size() : 0;
        int open = 0;
        int closed = 0;
        int done = 0;
        Date now = new Date();
        if (allEvents != null) {
            for (Event event : allEvents) {
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
        }

        statusFilterAdapter.setCounts(total, open, closed, done);
    }

    private enum EventLifecycleState {
        OPEN, CLOSED, DONE
    }

    public EventListFragment() {
        // Required empty public constructor
    }

    public static EventListFragment newInstance() {
        return new EventListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize repositories
        eventRepository = new EventRepositoryFs();
        waitingListRepository = new WaitingListRepositoryFs();
        
        // Get device-based user ID
        userId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.entrant_fragment_event_list, container, false);

        // Initialize UI elements
        recyclerView = view.findViewById(R.id.events_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        filterButton = view.findViewById(R.id.filter_button);
        qrScannerButton = view.findViewById(R.id.qr_scanner_button);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        bottomNavigationView = view.findViewById(R.id.bottom_navigation);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearch();

        // Setup filter button
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Setup QR scanner button
        qrScannerButton.setOnClickListener(v -> openQRScanner());

        // Setup bottom navigation
        setupBottomNavigation();

        // Load events
        loadEvents();

        return view;
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        adapter = new EventListAdapter(this);
        statusFilterAdapter = new StatusFilterAdapter(this::selectStatusFilter);
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
     * Sets up bottom navigation item selection.
     */
    private void setupBottomNavigation() {
        // Set Home as selected (current screen)
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                // Already on Home (Browse Events) screen
                return true;
            } else if (itemId == R.id.nav_history) {
                Intent intent = new Intent(requireContext(), EntrantHistoryActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_alerts) {
               Intent intent = new Intent(requireContext(), EntrantNotificationsActivity.class);
               startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent intent = new Intent(requireContext(), ProfileActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
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
                applyFilters(); // Apply any active filters
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
     * Shows filter dialog with options for sorting and filtering events.
     * Implements US 01.01.04 - Filter events by interest or availability.
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
        AutoCompleteTextView autoCompleteEventType = dialogView.findViewById(R.id.autoCompleteEventType);
        LinearLayout layoutEventTypes = dialogView.findViewById(R.id.layoutEventTypes);
        MaterialCheckBox checkAvailableSpots = dialogView.findViewById(R.id.checkAvailableSpots);
        MaterialButton btnClearFilters = dialogView.findViewById(R.id.btnClearFilters);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);
        
        // Price ranges: 0=$0, 1=$50, 2=$100, 3=$150, 4=$200+
        final double[] priceRanges = {0, 50, 100, 150, 200};
        
        // Set up Event Type dropdown
        String[] eventTypes = {"All types", "Recreational", "Athletic", "Educational", "Social", "Cultural"};
        ArrayAdapter<String> eventTypeAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            eventTypes
        );
        autoCompleteEventType.setAdapter(eventTypeAdapter);
        autoCompleteEventType.setFocusable(true);
        autoCompleteEventType.setFocusableInTouchMode(true);
        
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
        
        checkAvailableSpots.setChecked(onlyAvailableSpots);
        
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
        
        // Event type dropdown handler
        autoCompleteEventType.setOnItemClickListener((parent, view, position, id) -> {
            String selected = eventTypes[position];
            autoCompleteEventType.setText(selected, false);
            if (position == 0) {
                layoutEventTypes.setVisibility(View.GONE);
            } else {
                layoutEventTypes.setVisibility(View.VISIBLE);
            }
        });
        
        autoCompleteEventType.setOnClickListener(v -> autoCompleteEventType.showDropDown());
        
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
            
            // Get availability filter
            onlyAvailableSpots = checkAvailableSpots.isChecked();
            
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
            onlyAvailableSpots = false;
            
            // Reset dialog UI
            radioSortNewest.setChecked(true);
            seekBarPrice.setProgress(4);
            updatePriceText(textPriceValue, 4, priceRanges);
            editLocation.setText("");
            checkAvailableSpots.setChecked(false);
            autoCompleteEventType.setText("All types", false);
            layoutEventTypes.setVisibility(View.GONE);
            
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
        
        // Filter by available spots
        if (onlyAvailableSpots) {
            filteredEvents.removeIf(event -> event.getCapacity() <= 0);
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
        // Navigate to event details
        Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    @Override
    public void onJoinButtonClick(Event event) {
        // Check if registration dates are available
        if (event.getRegistrationStartDate() == null || event.getRegistrationEndDate() == null) {
            Toast.makeText(requireContext(), 
                    "Registration dates not available yet", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check if registration is open
        Date now = new Date();
        if (now.before(event.getRegistrationStartDate())) {
            Toast.makeText(requireContext(), 
                    "Registration hasn't opened yet", 
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (now.after(event.getRegistrationEndDate())) {
            Toast.makeText(requireContext(), 
                    "Registration has closed", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user is already in waiting list
        waitingListRepository.isUserInWaitingList(event.getEventId(), userId,
                new WaitingListRepository.OnCheckListener() {
                    @Override
                    public void onSuccess(boolean exists) {
                        if (exists) {
                            Toast.makeText(requireContext(), 
                                    "You're already in the waiting list", 
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            joinWaitingList(event);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // If check fails, try to join anyway
                        joinWaitingList(event);
                    }
                });
    }

    /**
     * Adds the user to the event's waiting list.
     *
     * @param event The event to join
     */
    private void joinWaitingList(Event event) {
        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(
                entryId, 
                event.getEventId(), 
                userId, 
                new Date()
        );

        waitingListRepository.addToWaitingList(entry,
                new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Successfully joined waiting list for " + event.getName(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed to join: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onQRCodeClick(Event event) {
        // Navigate to event details (same as clicking the card)
        onEventClick(event);
        Toast.makeText(requireContext(), "QR Code for " + event.getName(), Toast.LENGTH_SHORT).show();
    }

    /**
     * Opens the QR code scanner activity.
     */
    private void openQRScanner() {
        Intent intent = new Intent(requireContext(), QRScannerActivity.class);
        startActivity(intent);
    }
}

