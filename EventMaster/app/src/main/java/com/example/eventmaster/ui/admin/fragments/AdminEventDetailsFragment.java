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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.content.DialogInterface;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Profile;

import com.example.eventmaster.ui.admin.activities.AdminNotificationLogActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
    private ProfileRepositoryFs profileRepository;
    private String eventId;
    private Event currentEvent;

    // UI Elements
    private ImageView posterImage;
    private ImageView backButton;
    private TextView eventNameText;
    private TextView organizerText;
    private TextView eventDateText;
    private TextView eventTimeText;
    private TextView locationText;
    private TextView priceText;
    private TextView capacityText;
    private TextView descriptionText;
    private MaterialButton btnViewNotificationLogs;
    
        // Additional Details UI Elements
        private TextView waitingListSizeText;
        private TextView eventTypeText;
        private TextView registrationStartText;
        private TextView registrationEndText;
    private ImageView geolocationIcon;
    private TextView geolocationText;
    private com.google.android.material.card.MaterialCardView qrCodeButtonCard;

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
        profileRepository = new ProfileRepositoryFs();
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
        eventTimeText = view.findViewById(R.id.event_time_text);
        locationText = view.findViewById(R.id.event_location_text);
        priceText = view.findViewById(R.id.event_price_text);
        capacityText = view.findViewById(R.id.event_capacity_text);
        descriptionText = view.findViewById(R.id.event_description_text);
        btnViewNotificationLogs = view.findViewById(R.id.btn_view_notification_logs);
        
        // Additional Details UI
        waitingListSizeText = view.findViewById(R.id.waiting_list_size_text);
        eventTypeText = view.findViewById(R.id.event_type_text);
        registrationStartText = view.findViewById(R.id.registration_start_text);
        registrationEndText = view.findViewById(R.id.registration_end_text);
        geolocationIcon = view.findViewById(R.id.geolocation_icon);
        geolocationText = view.findViewById(R.id.geolocation_text);
        qrCodeButtonCard = view.findViewById(R.id.qr_code_button_card);
        
        // Setup QR code button
        if (qrCodeButtonCard != null) {
            qrCodeButtonCard.setOnClickListener(v -> showQrDialog());
        }

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

        // Organizer - format as "by [name]"
        String organizerName = event.getOrganizerName();
        if (organizerName != null && !organizerName.isEmpty()) {
            organizerText.setText("by " + organizerName);
        } else if (event.getOrganizerId() != null) {
            // Fetch organizer name from profile
            loadOrganizerName(event.getOrganizerId());
        } else {
            organizerText.setText("by Unknown");
        }

        // Event date
        if (event.getEventDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            eventDateText.setText(dateFormat.format(event.getEventDate()));
        } else {
            eventDateText.setText("TBA");
        }

        // Event time
        if (event.getEventDate() != null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            eventTimeText.setText(timeFormat.format(event.getEventDate()));
        } else {
            eventTimeText.setText("TBA");
        }

        // Location
        locationText.setText(event.getLocation() != null ? event.getLocation() : "TBA");

        // Price
        if (event.getPrice() > 0.0) {
            priceText.setText("$" + String.format(Locale.getDefault(), "%.2f", event.getPrice()));
        } else {
            priceText.setText("Free");
        }

        // Capacity
        if (event.getCapacity() > 0) {
            capacityText.setText(String.valueOf(event.getCapacity()));
        } else {
            capacityText.setText("Unlimited");
        }


        // Description
        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "No description available.");

        // Load poster image
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getPosterUrl())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .into(posterImage);
        }

        // Additional Details
        displayAdditionalDetails(event);
    }

    /**
     * Displays additional event details.
     */
    private void displayAdditionalDetails(Event event) {
        // Waiting List Size
        if (event.getWaitingListLimit() != null && event.getWaitingListLimit() > 0) {
            waitingListSizeText.setText(String.valueOf(event.getWaitingListLimit()));
        } else {
            waitingListSizeText.setText("Unlimited");
        }

        // Event Type
        if (event.getEventType() != null && !event.getEventType().isEmpty()) {
            eventTypeText.setText(event.getEventType());
        } else {
            eventTypeText.setText("Not specified");
        }

        // Registration Start Date
        if (event.getRegistrationStartDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = dateFormat.format(event.getRegistrationStartDate());
            registrationStartText.setText(dateStr);
        } else {
            registrationStartText.setText("TBA");
        }

        // Registration End Date (already shown as Deadline in first grid, but show here for consistency)
        if (event.getRegistrationEndDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = dateFormat.format(event.getRegistrationEndDate());
            registrationEndText.setText(dateStr);
        } else {
            registrationEndText.setText("TBA");
        }


        // QR Code button - enable if QR code exists
        boolean hasQrCode = event.getQrUrl() != null && !event.getQrUrl().isEmpty();
        if (qrCodeButtonCard != null) {
            if (hasQrCode) {
                qrCodeButtonCard.setEnabled(true);
                qrCodeButtonCard.setAlpha(1.0f);
            } else {
                qrCodeButtonCard.setEnabled(false);
                qrCodeButtonCard.setAlpha(0.5f);
            }
        }

        // Geolocation requirement
        if (event.isGeolocationRequired()) {
            geolocationText.setText("Requires geolocation on join");
            geolocationIcon.setVisibility(View.VISIBLE);
            geolocationIcon.setColorFilter(requireContext().getResources().getColor(R.color.teal_dark, null));
        } else {
            geolocationText.setText("Does not require geolocation on join");
            geolocationIcon.setVisibility(View.VISIBLE);
            geolocationIcon.setColorFilter(requireContext().getResources().getColor(android.R.color.darker_gray, null));
        }
    }

    /**
     * Loads organizer name from profile if not already stored in event.
     */
    private void loadOrganizerName(String organizerId) {
        if (organizerId == null || organizerId.isEmpty()) {
            organizerText.setText("by Unknown");
            return;
        }

        // Set a temporary text while loading
        organizerText.setText("by " + organizerId);

        profileRepository.get(organizerId,
            profile -> {
                if (profile != null && profile.getName() != null && !profile.getName().isEmpty()) {
                    organizerText.setText("by " + profile.getName());
                } else {
                    organizerText.setText("by Unknown");
                }
            },
            error -> {
                Log.e(TAG, "Failed to load organizer profile", error);
                organizerText.setText("by Unknown");
            }
        );
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

    /**
     * Shows QR code dialog similar to entrant view.
     */
    private void showQrDialog() {
        if (currentEvent == null || currentEvent.getQrUrl() == null || currentEvent.getQrUrl().isEmpty()) {
            Toast.makeText(requireContext(), "QR code not available yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_event_qr, null, false);
        ImageView qrImage = dialogView.findViewById(R.id.dialog_qr_image);
        Glide.with(requireContext())
                .load(currentEvent.getQrUrl())
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(qrImage);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Event QR Code")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();
        
        // Change dialog background from purple to white and make it smaller
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int maxWidth = (int) (screenWidth * 0.85); // 85% of screen width
            params.width = maxWidth;
            dialog.getWindow().setAttributes(params);
        }
        
        // Change Close button color from purple to teal
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.teal_dark)
                );
            }
        });
        
        dialog.show();
    }
}

