package com.example.eventmaster.ui.admin.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Intent;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.ui.admin.activities.AdminNotificationLogActivity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Fragment displaying read-only event details for admin viewing.
 * Shows event information without any action buttons (no join, accept, decline, etc.).
 */
public class AdminEventDetailsFragment extends Fragment {

    private static final String TAG = "AdminEventDetailsFragment";
    private static final String ARG_EVENT_ID = "event_id";

    private EventRepository eventRepository;
    private String eventId;
    private Event currentEvent;

    // UI Elements
    private ImageView posterImage;
    private ImageView backButton;
    private TextView eventNameText;
    private TextView organizerText;
    private TextView eventDateText;
    private TextView locationText;
    private TextView priceText;
    private TextView capacityText;
    private TextView registrationDateText;
    private TextView descriptionText;
    private MaterialButton btnViewNotificationLogs;

    public static AdminEventDetailsFragment newInstance(String eventId) {
        AdminEventDetailsFragment fragment = new AdminEventDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
        }

        eventRepository = new EventRepositoryFs();
        Log.d(TAG, "onCreate: eventId=" + eventId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.admin_fragment_event_details, container, false);

        // Initialize UI
        posterImage = view.findViewById(R.id.event_poster_image);
        backButton = view.findViewById(R.id.back_button);
        eventNameText = view.findViewById(R.id.event_name_text);
        organizerText = view.findViewById(R.id.event_organizer_text);
        eventDateText = view.findViewById(R.id.event_date_text);
        locationText = view.findViewById(R.id.event_location_text);
        priceText = view.findViewById(R.id.event_price_text);
        capacityText = view.findViewById(R.id.event_capacity_text);
        registrationDateText = view.findViewById(R.id.registration_date_text);
        descriptionText = view.findViewById(R.id.event_description_text);
        btnViewNotificationLogs = view.findViewById(R.id.btn_view_notification_logs);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        btnViewNotificationLogs.setOnClickListener(v -> openNotificationLogs());

        // Load event details
        loadEventDetails();

        return view;
    }

    /**
     * Loads event details from Firebase.
     */
    private void loadEventDetails() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid event ID", Toast.LENGTH_SHORT).show();
            return;
        }

        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load event details", e);
                Toast.makeText(requireContext(), 
                        "Failed to load event: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Displays event details in the UI.
     */
    private void displayEventDetails(Event event) {
        // Event name
        eventNameText.setText(event.getName() != null ? event.getName() : "Unnamed Event");

        // Organizer
        organizerText.setText("Organizer: " + (event.getOrganizerId() != null ? event.getOrganizerId() : "Unknown"));

        // Event date
        if (event.getEventDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            eventDateText.setText("Date: " + dateFormat.format(event.getEventDate()));
        } else {
            eventDateText.setText("Date: TBA");
        }

        // Location
        locationText.setText("Location: " + (event.getLocation() != null ? event.getLocation() : "TBA"));

        // Price
        if (event.getPrice() > 0.0) {
            priceText.setText("Price: $" + String.format(Locale.getDefault(), "%.2f", event.getPrice()));
        } else {
            priceText.setText("Price: Free");
        }

        // Capacity
        if (event.getCapacity() > 0) {
            capacityText.setText("Capacity: " + event.getCapacity() + " attendees");
        } else {
            capacityText.setText("Capacity: Unlimited");
        }

        // Registration deadline
        if (event.getRegistrationEndDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            registrationDateText.setText("Registration Deadline: " + dateFormat.format(event.getRegistrationEndDate()));
        } else {
            registrationDateText.setText("Registration Deadline: TBA");
        }

        // Description
        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "No description available.");

        // Load poster image
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getPosterUrl())
                    .into(posterImage);
        }
    }

    /**
     * Opens notification logs filtered by this event.
     */
    private void openNotificationLogs() {
        if (eventId != null) {
            Intent intent = new Intent(requireContext(), AdminNotificationLogActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        }
    }
}

