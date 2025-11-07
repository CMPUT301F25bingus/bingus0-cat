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

import com.bumptech.glide.Glide;
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

    // Invitation include
    private View inviteInclude;
    private TextView inviteStatusText;
    private MaterialButton btnAccept;
    private MaterialButton btnDecline;

    private boolean testMode = false;
    private boolean testForceInvited = false;

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

        Bundle a = getArguments();
        if (a != null) {
            testMode = a.getBoolean("TEST_MODE", false);
            testForceInvited = a.getBoolean("TEST_FORCE_INVITED", false);
            eventId = a.getString(ARG_EVENT_ID);
        }

        if ((a == null || !a.containsKey("TEST_MODE")) && getActivity() != null && getActivity().getIntent() != null) {
            testMode = getActivity().getIntent().getBooleanExtra("TEST_MODE", false);
            testForceInvited = getActivity().getIntent().getBooleanExtra("TEST_FORCE_INVITED", false);
        }

        eventRepository = new EventRepositoryFs();
        waitingListRepository = new WaitingListRepositoryFs();
        invitationService = new InvitationServiceFs();
        registrationService = new RegistrationServiceFs();

        userId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_details, container, false);

        // Initialize UI
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

        inviteInclude = view.findViewById(R.id.invitation_include);
        inviteStatusText = view.findViewById(R.id.invite_status_text);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnDecline = view.findViewById(R.id.btnDecline);

        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        favoriteIcon.setOnClickListener(v -> handleFavoriteClick());
        joinButton.setOnClickListener(v -> handleJoinWaitingList());

        loadEventDetails();

        if (!testMode) {
            decideInviteOrJoin();
        } else {
            if (testForceInvited) {
                inviteInclude.setVisibility(View.VISIBLE);
            } else {
                joinButton.setVisibility(View.VISIBLE);
            }
        }

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
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Decides whether to show invitation actions or join button. */
    private void decideInviteOrJoin() {
        invitationService.getMyInvitation(eventId, userId, inv -> {
            if (inv != null) {
                showInvitationInclude(inv);
            } else {
                showJoinButtonWithState();
            }
        }, err -> showJoinButtonWithState());
    }

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

            default:
                inviteStatusText.setText("Invitation declined");
                inviteStatusText.setVisibility(View.VISIBLE);
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                break;
        }
    }

    private void setInviteButtonsEnabled(boolean enabled) {
        btnAccept.setEnabled(enabled);
        btnDecline.setEnabled(enabled);
    }

    private void showJoinButtonWithState() {
        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.VISIBLE);
        checkIfUserInWaitingList();
    }

    /** Displays event details in the UI. */
    private void displayEventDetails(Event event) {
        eventNameText.setText(event.getName() != null ? event.getName() : "Unnamed Event");
        organizerText.setText("Hosted by: " +
                (event.getOrganizerName() != null ? event.getOrganizerName() : "Unknown"));
        locationText.setText(event.getLocation() != null ? event.getLocation() : "Location TBA");
        descriptionText.setText(event.getDescription() != null ? event.getDescription() : "No description available");

        // Price formatting
        if (event.getPrice() % 1 == 0) {
            priceText.setText(String.format(Locale.getDefault(), "$%.0f", event.getPrice()));
        } else {
            priceText.setText(String.format(Locale.getDefault(), "$%.2f", event.getPrice()));
        }

        capacityText.setText(String.valueOf(event.getCapacity()));

        // Date display
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat eventDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        if (event.getRegistrationStartDate() != null && event.getRegistrationEndDate() != null) {
            String start = shortDateFormat.format(event.getRegistrationStartDate());
            String end = shortDateFormat.format(event.getRegistrationEndDate());
            registrationDateText.setText("Registration: " + start + " - " + end);
        } else {
            registrationDateText.setText("Registration: TBA");
        }

        if (event.getEventDate() != null) {
            eventDateText.setText(eventDateFormat.format(event.getEventDate()));
        } else {
            eventDateText.setText("TBA");
        }

        // Poster image
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getPosterUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(posterImage);
        } else {
            posterImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // ✅ Load QR image from Firestore (not generate locally)
        if (event.getQrUrl() != null && !event.getQrUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getQrUrl())
                    .into(qrCodeImage);
        } else {
            qrCodeImage.setVisibility(View.GONE);
        }
    }

    /** Loads number of people on waiting list. */
    private void loadWaitingListCount() {
        waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
            @Override
            public void onSuccess(int count) {
                waitingListCountText.setText(count + " people have joined the waiting list");
            }

            @Override
            public void onFailure(Exception e) {
                waitingListCountText.setText("Unable to load waiting list count");
            }
        });
    }

    private void handleFavoriteClick() {
        Toast.makeText(requireContext(), "Favorite feature coming soon!", Toast.LENGTH_SHORT).show();
    }

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
                        joinButton.setEnabled(true);
                        joinButton.setText("Join Waiting List");
                    }
                });
    }

    private void handleJoinWaitingList() {
        if (currentEvent == null) {
            Toast.makeText(requireContext(), "Event data not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

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

        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(entryId, eventId, userId, new Date());

        joinButton.setEnabled(false);
        joinButton.setText("Joining...");

        waitingListRepository.addToWaitingList(entry, new WaitingListRepository.OnWaitingListOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(), "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                joinButton.setText("Already in Waiting List");
                loadWaitingListCount();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                joinButton.setEnabled(true);
                joinButton.setText("Join Waiting List");
            }
        });
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
