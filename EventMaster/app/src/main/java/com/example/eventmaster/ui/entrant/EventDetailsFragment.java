package com.example.eventmaster.ui.entrant;

import android.graphics.Bitmap;
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
import com.example.eventmaster.data.firestore.InvitationServiceFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Invitation;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.utils.DeviceUtils;
import com.example.eventmaster.utils.QRCodeGenerator;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Fragment displaying event details and allowing users to join the waiting list.
 * Implements:
 *  - US 01.06.01 View event details
 *  - US 01.06.02 Sign up from event details
 *  - US 01.05.02 Accept invitation
 *  - US 01.05.03 Decline invitation
 */
public class EventDetailsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";

    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;

    // Added services for Owner D
    private InvitationServiceFs invitationService;
    private RegistrationServiceFs registrationService;

    private String eventId;
    private Event currentEvent;
    private String userId;

    // UI Elements
    private ImageView posterImage;
    private ImageView backButton;
    private ImageView favoriteIcon;
    private ImageView qrCodeImage;
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

    // Invitation include (container + inner views)
    private View inviteInclude;                 // <include layout="@layout/include_invitation_actions">
    private TextView inviteStatusText;         // @id/invite_status_text (chip)
    private MaterialButton btnAccept;          // @id/btnAccept
    private MaterialButton btnDecline;         // @id/btnDecline

    /** Factory method */
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

        // Owner D services
        invitationService   = new InvitationServiceFs();
        registrationService = new RegistrationServiceFs();

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
        qrCodeImage = view.findViewById(R.id.qr_code_image);
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

        // Invitation include + inner controls
        inviteInclude    = view.findViewById(R.id.invitation_include);
        inviteStatusText = view.findViewById(R.id.invite_status_text);
        btnAccept        = view.findViewById(R.id.btnAccept);
        btnDecline       = view.findViewById(R.id.btnDecline);

        // Default states while loading
        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);

        // Set click listeners
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        favoriteIcon.setOnClickListener(v -> handleFavoriteClick());
        joinButton.setOnClickListener(v -> handleJoinWaitingList());

        // Load event details (poster/qr/etc.)
        loadEventDetails();

        // Decide which CTA to show: invitation include vs join button
        decideInviteOrJoin();

        return view;
    }

    /** Loads event details from Firestore and displays them. */
    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
                loadWaitingListCount();
                // (Do not call checkIfUserInWaitingList() here—only show join after we know no invite)
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Decide which CTA to show. If invited → show include; else → show join button. */
    private void decideInviteOrJoin() {
        invitationService.getMyInvitation(eventId, userId, inv -> {
            if (inv != null) {
                // There is an invitation (any status)
                showInvitationInclude(inv);
            } else {
                // No invitation → allow joining (respect your existing waiting-list logic)
                showJoinButtonWithState();
            }
        }, err -> {
            // On error, fall back to join
            showJoinButtonWithState();
        });
    }

    /** Shows the invitation include and wires Accept/Decline. */
    private void showInvitationInclude(@NonNull Invitation inv) {
        inviteInclude.setVisibility(View.VISIBLE);
        joinButton.setVisibility(View.GONE);

        String status = String.valueOf(inv.getStatus());
        switch (status) {
            case "PENDING":
                inviteStatusText.setVisibility(View.GONE);
                btnAccept.setEnabled(true);
                btnDecline.setEnabled(true);

                btnAccept.setOnClickListener(v -> {
                    setInviteButtonsEnabled(false);
                    invitationService.accept(inv.getId(), eventId, userId, ok -> {
                        registrationService.enroll(eventId, userId, r -> {
                            inviteStatusText.setText("You're enrolled 🎉");
                            inviteStatusText.setVisibility(View.VISIBLE);
                        }, e -> {
                            toast(e.getMessage());
                            setInviteButtonsEnabled(true);
                        });
                    }, e -> {
                        toast(e.getMessage());
                        setInviteButtonsEnabled(true);
                    });
                });

                btnDecline.setOnClickListener(v -> {
                    setInviteButtonsEnabled(false);
                    invitationService.decline(inv.getId(), eventId, userId, ok -> {
                        inviteStatusText.setText("Invitation declined");
                        inviteStatusText.setVisibility(View.VISIBLE);
                    }, e -> {
                        toast(e.getMessage());
                        setInviteButtonsEnabled(true);
                    });
                });
                break;

            case "ACCEPTED":
                inviteStatusText.setText("You're enrolled 🎉");
                inviteStatusText.setVisibility(View.VISIBLE);
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                break;

            default: // DECLINED
                inviteStatusText.setText("Invitation declined");
                inviteStatusText.setVisibility(View.VISIBLE);
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                break;
        }
    }

    /** Shows the Join button and reuses your existing state checks to enable/disable it. */
    private void showJoinButtonWithState() {
        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.VISIBLE);
        // Now apply your current "already joined?" logic
        checkIfUserInWaitingList();
    }

    private void setInviteButtonsEnabled(boolean enabled) {
        btnAccept.setEnabled(enabled);
        btnDecline.setEnabled(enabled);
    }

    /** Displays event details in the UI. */
    private void displayEventDetails(Event event) {
        eventNameText.setText(event.getName() != null ? event.getName() : "Unnamed Event");
        organizerText.setText("Hosted by: " + (event.getOrganizerName() != null ? event.getOrganizerName() : "Unknown"));
        locationText.setText(event.getLocation() != null ? event.getLocation() : "Location TBA");

        // Price
        if (event.getPrice() % 1 == 0) {
            priceText.setText(String.format(Locale.getDefault(), "$%.0f", event.getPrice()));
        } else {
            priceText.setText(String.format(Locale.getDefault(), "$%.2f", event.getPrice()));
        }

        capacityText.setText(String.valueOf(event.getCapacity()));
        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "No description available");

        // Dates
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat eventDateFormat = new SimpleDateFormat("MMM dd - MMM, yyyy", Locale.getDefault());

        if (event.getRegistrationEndDate() != null) {
            registrationDateText.setText(shortDateFormat.format(event.getRegistrationEndDate()));
        } else {
            registrationDateText.setText("TBA");
        }

        if (event.getEventDate() != null) {
            eventDateText.setText(eventDateFormat.format(event.getEventDate()));
        } else {
            eventDateText.setText("TBA");
        }

        // TODO: Load poster image if posterUrl exists (Glide/Picasso)

        // Generate QR code for deep link / sharing
        generateQRCode();
    }

    /** Generates and displays the QR code for this event. */
    private void generateQRCode() {
        if (eventId == null || eventId.isEmpty()) return;

        new Thread(() -> {
            final Bitmap qrCodeBitmap = QRCodeGenerator.generateQRCode(eventId, 400, 400);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (qrCodeBitmap != null) {
                        qrCodeImage.setImageBitmap(qrCodeBitmap);
                    } else {
                        qrCodeImage.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Failed to generate QR code",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /** Loads the count of people on the waiting list. */
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

    /** Favorite placeholder */
    private void handleFavoriteClick() {
        Toast.makeText(requireContext(), "Favorite feature coming soon!", Toast.LENGTH_SHORT).show();
    }

    /** Checks if the current user is already in the waiting list (kept from your original logic). */
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
                        joinButton.setText("Join Waiting List");
                    }
                });
    }

    /** Handles the join waiting list button click. */
    private void handleJoinWaitingList() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check registration window
        if (currentEvent.getRegistrationStartDate() == null || currentEvent.getRegistrationEndDate() == null) {
            Toast.makeText(requireContext(), "Registration dates not available yet", Toast.LENGTH_SHORT).show();
            return;
        }

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
        // Optional: set geolocation here if required

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

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
