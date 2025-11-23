package com.example.eventmaster.ui.entrant.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.ui.entrant.activities.EntrantNotificationsActivity;
import com.example.eventmaster.ui.entrant.activities.EventDetailsActivity;
import com.example.eventmaster.ui.entrant.adapters.EventListAdapter;
import com.example.eventmaster.ui.shared.activities.ProfileActivity;
import com.example.eventmaster.ui.shared.activities.QRScannerActivity;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
    private EventListAdapter adapter;
    private String userId;

    private RecyclerView recyclerView;
    private EditText searchEditText;
    private MaterialButton filterButton;
    private TextView emptyStateText;
    private BottomNavigationView bottomNavigationView;

    private List<Event> allEvents = new ArrayList<>();

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
        emptyStateText = view.findViewById(R.id.empty_state_text);
        bottomNavigationView = view.findViewById(R.id.bottom_navigation);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search functionality
        setupSearch();

        // Setup filter button
        filterButton.setOnClickListener(v -> showFilterDialog());

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
        recyclerView.setAdapter(adapter);
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
                adapter.filter(s.toString(), allEvents);
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
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_search) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(requireContext(), ProfileActivity.class));
                return true;
            } else if (itemId == R.id.nav_notifications) {
                Intent intent = new Intent(requireContext(), EntrantNotificationsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_scan_qr){
                Intent i = new Intent(requireContext(), QRScannerActivity.class);
                startActivity(i);
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

        Map<String, Object> data = new HashMap<>();
        data.put("entryId", entryId);
        data.put("eventId", event.getEventId());
        data.put("userId", userId);
        data.put("joinedAt", new Date());
        data.put("lat", lat);
        data.put("lng", lng);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(event.getEventId())
                .collection("waiting_list")   // MUST MATCH WaitingListRepositoryFs
                .document(userId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(),
                            "Successfully joined! (Location included)",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to join: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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
}
