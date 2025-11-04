package com.example.eventmaster.ui.entrant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Fragment displaying event details and allowing users to join the waiting list.
 * Implements US 01.06.01 (View event details) and US 01.06.02 (Sign up from event details).
 */
public class EventDetailsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;

    private String eventId;
    private Event currentEvent;
    private String userId;

    // UI Elements
    private ImageView posterImage;
    private ImageView backButton;
    private ImageView favoriteIcon;
    private TextView eventNameText;
    private TextView organizerText;
    private TextView eventDateText;
    private TextView locationText;
    private TextView priceText;
    private TextView capacityText;
    private TextView registrationDateText;
    private TextView descriptionText;
    private TextView waitingListCountText;
    private MaterialButton joinButton;

    /**
     * Factory method to create a new instance of this fragment.
     *
     * @param eventId The event ID to display
     * @return A new instance of EventDetailsFragment
     */
    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        // Initialize repositories
        eventRepository = new EventRepositoryFs();
        waitingListRepository = new WaitingListRepositoryFs();

        // Get device-based user ID (US 01.07.01)
        userId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        // Initialize UI elements
        posterImage = view.findViewById(R.id.event_poster_image);
        backButton = view.findViewById(R.id.back_button);
        favoriteIcon = view.findViewById(R.id.favorite_icon);
        eventNameText = view.findViewById(R.id.event_name_text);
        organizerText = view.findViewById(R.id.event_organizer_text);
        eventDateText = view.findViewById(R.id.event_date_text);
        locationText = view.findViewById(R.id.event_location_text);
        priceText = view.findViewById(R.id.event_price_text);
        capacityText = view.findViewById(R.id.event_capacity_text);
        registrationDateText = view.findViewById(R.id.registration_date_text);
        descriptionText = view.findViewById(R.id.event_description_text);
        waitingListCountText = view.findViewById(R.id.waiting_list_count_text);
        joinButton = view.findViewById(R.id.join_waiting_list_button);

        // Set click listeners
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        favoriteIcon.setOnClickListener(v -> handleFavoriteClick());
        joinButton.setOnClickListener(v -> handleJoinWaitingList());

        // Load event details
        loadEventDetails();

        return view;
    }

    /**
     * Loads event details from Firestore and displays them.
     */
    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
                checkIfUserInWaitingList();
                loadWaitingListCount();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays event details in the UI.
     *
     * @param event The event to display
     */
    private void displayEventDetails(Event event) {
        eventNameText.setText(event.getName() != null ? event.getName() : "Unnamed Event");
        organizerText.setText("Hosted by: " + (event.getOrganizerName() != null ? event.getOrganizerName() : "Unknown"));
        locationText.setText(event.getLocation() != null ? event.getLocation() : "Location TBA");
        
        // Format price without decimals if it's a whole number
        if (event.getPrice() % 1 == 0) {
            priceText.setText(String.format(Locale.getDefault(), "$%.0f", event.getPrice()));
        } else {
            priceText.setText(String.format(Locale.getDefault(), "$%.2f", event.getPrice()));
        }
        
        capacityText.setText(String.valueOf(event.getCapacity()));
        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "No description available");

        // Format dates for the bottom section with null checks
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat eventDateFormat = new SimpleDateFormat("MMM dd - MMM, yyyy", Locale.getDefault());
        
        // Registration date (shown with calendar icon)
        if (event.getRegistrationEndDate() != null) {
            registrationDateText.setText(shortDateFormat.format(event.getRegistrationEndDate()));
        } else {
            registrationDateText.setText("TBA");
        }
        
        // Event date/duration (shown with clock icon)
        if (event.getEventDate() != null) {
            eventDateText.setText(eventDateFormat.format(event.getEventDate()));
        } else {
            eventDateText.setText("TBA");
        }

        // TODO: Load poster image if posterUrl is available
        // For now, you can use an image loading library like Glide or Picasso
    }

    /**
     * Loads the count of people on the waiting list.
     */
    private void loadWaitingListCount() {
        waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
            @Override
            public void onSuccess(int count) {
                String countText = count + " People have joined the waiting list";
                waitingListCountText.setText(countText);
            }

            @Override
            public void onFailure(Exception e) {
                waitingListCountText.setText("Unable to load waiting list count");
            }
        });
    }

    /**
     * Handles the favorite icon click.
     * TODO: Implement favorite functionality (save to user's favorites).
     */
    private void handleFavoriteClick() {
        // Toggle favorite state (visual feedback)
        // You can implement actual favorite saving logic here
        Toast.makeText(requireContext(), "Favorite feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Checks if the current user is already in the waiting list.
     */
    private void checkIfUserInWaitingList() {
        waitingListRepository.isUserInWaitingList(eventId, userId,
                new WaitingListRepository.OnCheckListener() {
                    @Override
                    public void onSuccess(boolean exists) {
                        if (exists) {
                            joinButton.setText("Already in Waiting List");
                            joinButton.setEnabled(false);
                        } else {
                            joinButton.setText("Join Waiting List");
                            joinButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // If check fails, enable button by default
                        joinButton.setEnabled(true);
                    }
                });
    }

    /**
     * Handles the join waiting list button click.
     * Implements US 01.06.02 - Sign up from event details.
     */
    private void handleJoinWaitingList() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if registration dates are available
        if (currentEvent.getRegistrationStartDate() == null || currentEvent.getRegistrationEndDate() == null) {
            Toast.makeText(requireContext(), "Registration dates not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if registration is open
        Date now = new Date();
        if (now.before(currentEvent.getRegistrationStartDate())) {
            Toast.makeText(requireContext(), "Registration hasn't opened yet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (now.after(currentEvent.getRegistrationEndDate())) {
            Toast.makeText(requireContext(), "Registration has closed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create waiting list entry
        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(entryId, eventId, userId, new Date());

        // TODO: If geolocation is required, get location and set it on the entry
        // entry.setLatitude(latitude);
        // entry.setLongitude(longitude);

        // Disable button while processing
        joinButton.setEnabled(false);
        joinButton.setText("Joining...");

        // Add to waiting list
        waitingListRepository.addToWaitingList(entry,
                new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(),
                                "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                        joinButton.setText("Already in Waiting List");
                        loadWaitingListCount(); // Refresh count
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(),
                                "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        joinButton.setEnabled(true);
                        joinButton.setText("Join Waiting List");
                    }
                });
    }
}

