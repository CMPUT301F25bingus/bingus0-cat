package com.example.eventmaster.ui.entrant.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Profile;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fragment displaying a list of all available events.
 * Implements US 01.01.03 - View list of all available events.
 */
public class EventListFragment extends Fragment implements EventListAdapter.OnEventClickListener {

    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    private EventListAdapter adapter;
    private String userId;
    private Profile currentProfile;

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private MaterialButton filterButton;
    private ImageButton qrScannerButton;
    private TextView emptyStateText;
    private BottomNavigationView bottomNavigationView;
    private StatusFilterAdapter statusFilterAdapter;
    private ConcatAdapter concatAdapter;

    private List<Event> allEvents = new ArrayList<>();
    private Map<String, Integer> waitingListCounts = new HashMap<>(); // eventId -> count

    // Filter state variables
    private List<Event> filteredEvents = new ArrayList<>();
    private List<Event> statusFilteredEvents = new ArrayList<>();
    private String sortOrder = "newest"; // "newest" or "oldest"
    private Double maxPrice = null; // null means no price limit
    private String locationFilter = null;
    private List<String> selectedEventTypes = new ArrayList<>(); // List of selected event types for filtering
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
        // Ensure waiting list counts are always available in adapter
        if (adapter != null && !waitingListCounts.isEmpty()) {
            adapter.setWaitingListCounts(waitingListCounts);
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
        
        // Count from filteredEvents instead of allEvents to reflect active filters
        // Use filteredEvents if it exists (even if empty - that means filters resulted in 0 matches)
        // Only fall back to allEvents if filteredEvents hasn't been initialized yet
        List<Event> eventsToCount = (filteredEvents != null) 
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

    private enum EventLifecycleState {
        OPEN, CLOSED, DONE
    }

    // --- GEOLOCATION SUPPORT ---
    private Event pendingGeolocationEvent; // Store event requiring location

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (pendingGeolocationEvent != null) {
                        fetchLocationAndJoin(pendingGeolocationEvent);
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Location is required to join this event",
                            Toast.LENGTH_SHORT).show();
                }
            });

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

        // --- Load profile for attaching to waiting list entries ---
        profileRepo.getByDeviceId(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        currentProfile = profile;
                    } else {
                        // No profile found by deviceId
                        // Check if user is signed in with Firebase
                        com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null && firebaseUser.getUid() != null) {
                            // User is signed in, check profile by Firebase UID
                            profileRepo.get(firebaseUser.getUid())
                                    .addOnSuccessListener(existingProfile -> {
                                        if (existingProfile != null) {
                                            // Profile exists by UID, update deviceId if needed
                                            if (existingProfile.getDeviceId() == null || existingProfile.getDeviceId().isEmpty()) {
                                                existingProfile.setDeviceId(userId);
                                                profileRepo.upsert(existingProfile);
                                            }
                                            currentProfile = existingProfile;
                                        } else {
                                            // No profile by UID either, create new one with Firebase UID
                                            Profile newP = new Profile();
                                            newP.setUserId(firebaseUser.getUid());
                                            newP.setDeviceId(userId);
                                            newP.setName("Guest User");
                                            newP.setEmail("");
                                            newP.setRole("entrant");
                                            newP.setActive(true);
                                            newP.setBanned(false);
                                            profileRepo.upsert(newP);
                                            currentProfile = newP;
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error getting by UID, create new profile with Firebase UID
                                        Profile newP = new Profile();
                                        newP.setUserId(firebaseUser.getUid());
                                        newP.setDeviceId(userId);
                                        newP.setName("Guest User");
                                        newP.setEmail("");
                                        newP.setRole("entrant");
                                        newP.setActive(true);
                                        newP.setBanned(false);
                                        profileRepo.upsert(newP);
                                        currentProfile = newP;
                                    });
                        }
                        // If not signed in, don't create profile - wait for proper sign-in
                    }
                });
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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
            public void afterTextChanged(Editable s) {}
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
//            } else if (itemId == R.id.nav_notifications) {
//                Intent intent = new Intent(requireContext(), EntrantNotificationsActivity.class);
//                startActivity(intent);
//                return true;
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
                filteredEvents = new ArrayList<>(events); // Initialize filteredEvents with all events
                updateStatusCounts(); // Update the status tab counts
                loadWaitingListCounts(events); // Load waiting list counts for all events
                applyStatusFilterAndRefresh(); // Apply status filter and refresh the display
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
     * Loads waiting list counts for all events and updates the adapter.
     */
    private void loadWaitingListCounts(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        waitingListCounts.clear();
        final int[] completedCount = {0};
        final int totalCount = events.size();

        for (Event event : events) {
            if (event == null || event.getId() == null) {
                completedCount[0]++;
                continue;
            }

            String eventId = event.getId();
            waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
                @Override
                public void onSuccess(int count) {
                    waitingListCounts.put(eventId, count);
                    completedCount[0]++;

                    // Update adapter when all counts are loaded
                    if (completedCount[0] == totalCount && adapter != null) {
                        adapter.setWaitingListCounts(waitingListCounts);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    // Default to 0 if fetch fails
                    waitingListCounts.put(eventId, 0);
                    completedCount[0]++;

                    // Update adapter when all counts are loaded
                    if (completedCount[0] == totalCount && adapter != null) {
                        adapter.setWaitingListCounts(waitingListCounts);
                    }
                }
            });
        }

        // If no events, update adapter immediately
        if (totalCount == 0 && adapter != null) {
            adapter.setWaitingListCounts(waitingListCounts);
        }
    }

    /**
     * Updates the empty state visibility based on event count.
     */
    private void updateEmptyState() {
        // Always keep RecyclerView visible so status filter bar stays visible
        // Only show/hide the empty state text overlay
        if (adapter.getItemCount() == 0) {
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
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
        // Navigate to event details
        Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
        intent.putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getEventId());
        startActivity(intent);
    }

    @Override
    public void onJoinButtonClick(Event event) {

        // --- GEOLOCATION REQUIREMENT (matches EventDetailsFragment) ---
        if (event.isGeolocationRequired()) {
            pendingGeolocationEvent = event;
            requestLocationPermissionThenJoin(event);
            return;
        }

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
                        joinWaitingList(event);
                    }
                });
    }

    // ---- GEOLOCATION JOIN LOGIC (NEW, same as EventDetailsFragment) ----

    private void requestLocationPermissionThenJoin(Event event) {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        fetchLocationAndJoin(event);
    }

    private void fetchLocationAndJoin(Event event) {
        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(requireContext());

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        client.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        joinWaitingListWithLocation(event, location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(requireContext(),
                                "Unable to fetch location",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinWaitingListWithLocation(Event event, double lat, double lng) {
        String entryId = UUID.randomUUID().toString();

        WaitingListEntry entry = new WaitingListEntry(
                entryId,
                event.getEventId(),
                userId,
                new Date()
        );

        entry.setProfile(currentProfile);  // ⭐ PROFILE INCLUDED
        entry.setlat(lat);
        entry.setlng(lng);

        ((WaitingListRepositoryFs) waitingListRepository)
                .joinWithLimitCheck(entry, new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Successfully joined with location!",
                                Toast.LENGTH_SHORT).show();
                        
                        // Send notification when user joins waiting list with location
                        sendJoinedWaitingListNotification(event.getEventId(), userId);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed to join: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Adds the user to the event's waiting list (non-geolocation path).
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

        entry.setProfile(currentProfile); //ensure profile detiasl are added to db

        waitingListRepository.addToWaitingList(entry,
                new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Successfully joined waiting list for " + event.getName(),
                                Toast.LENGTH_SHORT).show();
                        
                        // Send notification when user joins waiting list
                        sendJoinedWaitingListNotification(event.getEventId(), userId);
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

    /**
     * Send notification when user joins waiting list
     */
    private void sendJoinedWaitingListNotification(String eventId, String userId) {
        if (eventId == null || userId == null) {
            Log.w("EventListFragment", "Cannot send notification: missing eventId or userId");
            return;
        }
        
        // Get the Firebase Auth UID if available, otherwise use the provided userId (deviceId)
        final String recipientUserId = getCurrentFirebaseUserId() != null ? getCurrentFirebaseUserId() : userId;
        final String deviceIdForNotification = userId;
        
        Log.d("EventListFragment", "Sending notification: eventId=" + eventId + ", recipientUserId=" + recipientUserId + ", deviceId=" + deviceIdForNotification);
        
        // First, check if user has notifications enabled
        profileRepo.get(recipientUserId)
                .addOnSuccessListener(profile -> {
                    // Only send notification if user has notifications enabled
                    if (profile != null && profile.isNotificationsEnabled()) {
                        // Fetch event details to include event name in notification
                        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override
            public void onSuccess(Event event) {
                String eventName = event != null ? event.getName() : "this event";
                
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("eventId", eventId);
                notification.put("recipientUserId", recipientUserId); // Use Firebase Auth UID (primary field)
                notification.put("recipientId", recipientUserId); // Also add legacy field for backward compatibility
                // Also store deviceId for query flexibility
                if (!recipientUserId.equals(deviceIdForNotification)) {
                    notification.put("deviceId", deviceIdForNotification);
                }
                notification.put("senderUserId", "system");
                notification.put("type", "GENERAL"); // Use valid NotificationType
                notification.put("title", "Joined Waiting List");
                notification.put("message", "You've successfully joined the waiting list for \"" + eventName + "\". Good luck!");
                notification.put("isRead", false);
                notification.put("sentAt", Timestamp.now()); // Use sentAt (matches Notification model)
                
                db.collection("notifications")
                        .add(notification)
                        .addOnSuccessListener(docRef -> {
                            Log.d("EventListFragment", "✅ Sent waiting list notification to userId: " + recipientUserId);
                            // Update with notificationId
                            docRef.update("notificationId", docRef.getId());
                        })
                        .addOnFailureListener(e -> Log.e("EventListFragment", "❌ Failed to send notification", e));
            }
            
            @Override
            public void onFailure(Exception e) {
                Log.e("EventListFragment", "Failed to fetch event for notification", e);
                // Fallback to generic message if event fetch fails
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> notification = new HashMap<>();
                notification.put("eventId", eventId);
                notification.put("recipientUserId", recipientUserId);
                notification.put("recipientId", recipientUserId);
                if (!recipientUserId.equals(deviceIdForNotification)) {
                    notification.put("deviceId", deviceIdForNotification);
                }
                notification.put("senderUserId", "system");
                notification.put("type", "GENERAL");
                notification.put("title", "Joined Waiting List");
                notification.put("message", "You've successfully joined the waiting list for this event. Good luck!");
                notification.put("isRead", false);
                notification.put("sentAt", Timestamp.now());
                
                db.collection("notifications")
                        .add(notification)
                        .addOnSuccessListener(docRef -> {
                            Log.d("EventListFragment", "✅ Sent waiting list notification (fallback) to userId: " + recipientUserId);
                            docRef.update("notificationId", docRef.getId());
                        })
                        .addOnFailureListener(err -> Log.e("EventListFragment", "❌ Failed to send notification", err));
                            }
                        });
                    } else {
                        Log.d("EventListFragment", "⏭️ Skipping waiting list notification for " + recipientUserId + " (opted out)");
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't fetch profile, default to sending notification (backward compatibility)
                    Log.w("EventListFragment", "⚠️ Could not fetch profile for " + recipientUserId + ", sending notification anyway", e);
                    eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
                        @Override
                        public void onSuccess(Event event) {
                            String eventName = event != null ? event.getName() : "this event";
                            
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("eventId", eventId);
                            notification.put("recipientUserId", recipientUserId);
                            notification.put("recipientId", recipientUserId);
                            if (!recipientUserId.equals(deviceIdForNotification)) {
                                notification.put("deviceId", deviceIdForNotification);
                            }
                            notification.put("senderUserId", "system");
                            notification.put("type", "GENERAL");
                            notification.put("title", "Joined Waiting List");
                            notification.put("message", "You've successfully joined the waiting list for \"" + eventName + "\". Good luck!");
                            notification.put("isRead", false);
                            notification.put("sentAt", Timestamp.now());
                            
                            db.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(docRef -> {
                                        Log.d("EventListFragment", "✅ Sent waiting list notification to userId: " + recipientUserId + " (profile fetch failed)");
                                        docRef.update("notificationId", docRef.getId());
                                    })
                                    .addOnFailureListener(err -> Log.e("EventListFragment", "❌ Failed to send notification", err));
                        }
                        
                        @Override
                        public void onFailure(Exception err) {
                            Log.e("EventListFragment", "Failed to fetch event for notification", err);
                        }
                    });
                });
    }

    /**
     * Gets the current Firebase Auth UID if user is authenticated, otherwise null
     */
    private String getCurrentFirebaseUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }
}

