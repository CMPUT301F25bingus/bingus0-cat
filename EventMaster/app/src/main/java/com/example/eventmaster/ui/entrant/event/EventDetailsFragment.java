package com.example.eventmaster.ui.entrant.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.InvitationService;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.InvitationServiceFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Invitation;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class EventDetailsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "event_id";
    public static final String ARG_USER_ID  = "user_id";


    private FirebaseFirestore db;
    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private InvitationService invitationService;

    private String eventId;
    private String userId;
    private Event currentEvent;

    // Top/UI
    private ImageView posterImage, backButton, favoriteIcon;
    private TextView eventNameText, organizerText, eventDateText, locationText,
            priceText, capacityText, registrationDateText, descriptionText, waitingListCountText;
    private MaterialButton joinButton;

    // Invitation include UI
    private View invitationInclude;
    private TextView inviteStatusText;
    private Button acceptBtn, declineBtn;

    public static EventDetailsFragment newInstance(String eventId, String userId) {
        EventDetailsFragment f = new EventDetailsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        b.putString(ARG_USER_ID,  userId);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            userId  = getArguments().getString(ARG_USER_ID); // << use this
        }
        if (userId == null || userId.isEmpty()) {
            // fallback if you didn’t pass it
            userId = com.example.eventmaster.utils.DeviceUtils.getDeviceId(requireContext());
        }

        db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        eventRepository = new com.example.eventmaster.data.firestore.EventRepositoryFs();
        waitingListRepository = new com.example.eventmaster.data.firestore.WaitingListRepositoryFs();
        invitationService = new com.example.eventmaster.data.firestore.InvitationServiceFs(db);


        // IMPORTANT: this must MATCH the `entrantId` stored in your invitation doc(s)
        // You can hardcode for testing, or use DeviceUtils (if that's what your app uses).
        // userId = "testUser1";
//        userId = DeviceUtils.getDeviceId(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_event_details, container, false);

        posterImage = v.findViewById(R.id.event_poster_image);
        backButton  = v.findViewById(R.id.back_button);
        favoriteIcon= v.findViewById(R.id.favorite_icon);

        eventNameText = v.findViewById(R.id.event_name_text);
        organizerText = v.findViewById(R.id.event_organizer_text);
        eventDateText = v.findViewById(R.id.event_date_text);
        locationText  = v.findViewById(R.id.event_location_text);
        priceText     = v.findViewById(R.id.event_price_text);
        capacityText  = v.findViewById(R.id.event_capacity_text);
        registrationDateText = v.findViewById(R.id.registration_date_text);
        descriptionText = v.findViewById(R.id.event_description_text);
        waitingListCountText = v.findViewById(R.id.waiting_list_count_text);
        joinButton = v.findViewById(R.id.join_waiting_list_button);

        // Invitation block (include)
        invitationInclude = v.findViewById(R.id.invitation_include);
        if (invitationInclude != null) {
            inviteStatusText = invitationInclude.findViewById(R.id.invite_status_text);
            acceptBtn = invitationInclude.findViewById(R.id.btnAccept);
            declineBtn = invitationInclude.findViewById(R.id.btnDecline);
        }

        backButton.setOnClickListener(click -> requireActivity().onBackPressed());
        favoriteIcon.setOnClickListener(click ->
                Toast.makeText(requireContext(), "Favorite coming soon!", Toast.LENGTH_SHORT).show());
        if (joinButton != null) joinButton.setOnClickListener(click -> handleJoinWaitingList());

        loadEvent();
        return v;
    }

    private void loadEvent() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override public void onSuccess(Event event) {
                currentEvent = event;
                bindEvent(event);
                // After event loads, decide which action UI to show
                mountInvitationUi();
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindEvent(Event e) {
        eventNameText.setText(e.getName() != null ? e.getName() : "Unnamed Event");
        organizerText.setText("Hosted by: " + (e.getOrganizerName() != null ? e.getOrganizerName() : "Unknown"));
        locationText.setText(e.getLocation() != null ? e.getLocation() : "Location TBA");

        if (e.getPrice() % 1 == 0) {
            priceText.setText(String.format(Locale.getDefault(), "$%.0f", e.getPrice()));
        } else {
            priceText.setText(String.format(Locale.getDefault(), "$%.2f", e.getPrice()));
        }

        capacityText.setText(String.valueOf(e.getCapacity()));
        descriptionText.setText(e.getDescription() != null ? e.getDescription() : "No description available");

        SimpleDateFormat shortDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat eventDates = new SimpleDateFormat("MMM dd - MMM, yyyy", Locale.getDefault());

        if (e.getRegistrationEndDate() != null) {
            registrationDateText.setText(shortDate.format(e.getRegistrationEndDate()));
        } else {
            registrationDateText.setText("TBA");
        }

        if (e.getEventDate() != null) {
            eventDateText.setText(eventDates.format(e.getEventDate()));
        } else {
            eventDateText.setText("TBA");
        }
    }

    private void mountInvitationUi() {
        invitationService.getForEventAndEntrant(eventId, userId)
                .addOnSuccessListener(inv -> {
                    android.util.Log.d("INV", "eventId=" + eventId + " userId=" + userId + " inv=" + inv);
                    boolean hasPendingInvite = false;
                    if (inv != null && inv.getStatus() != null) {
                        hasPendingInvite = "PENDING".equalsIgnoreCase(inv.getStatus().trim());
                    }

                    // Toggle views
                    if (invitationInclude != null) {
                        invitationInclude.setVisibility(hasPendingInvite ? View.VISIBLE : View.GONE);
                    }
                    if (joinButton != null) {
                        joinButton.setVisibility(hasPendingInvite ? View.GONE : View.VISIBLE);
                    }

                    if (!hasPendingInvite) {
                        // Only show waiting-list helpers if there is no pending invite
                        checkIfUserInWaitingList();
                        loadWaitingListCount();
                        return;
                    }

                    // Wire Accept/Decline for this invite
                    final String invitationId = inv.getId(); // if your doc id is entrantId, this might equal userId
                    acceptBtn.setEnabled(true);
                    declineBtn.setEnabled(true);
                    inviteStatusText.setVisibility(View.GONE);

                    acceptBtn.setOnClickListener(v -> {
                        setActionBusy(true, "Accepting…");
                        invitationService.respond(eventId, invitationId, true, userId)
                                .addOnSuccessListener(x -> showInviteStatus("Invitation accepted"))
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                    setActionBusy(false, "Accept");
                                });
                    });

                    declineBtn.setOnClickListener(v -> {
                        setActionBusy(true, "Declining…");
                        invitationService.respond(eventId, invitationId, false, userId)
                                .addOnSuccessListener(x -> showInviteStatus("Invitation declined"))
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                                    setActionBusy(false, "Decline");
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("INV", "getForEventAndEntrant failed", e);
                    if (invitationInclude != null) invitationInclude.setVisibility(View.GONE);
                    if (joinButton != null) joinButton.setVisibility(View.VISIBLE);
                    checkIfUserInWaitingList();
                    loadWaitingListCount();
                });
    }

    private void setActionBusy(boolean busy, String labelIfBusy) {
        if (acceptBtn == null || declineBtn == null) return;
        acceptBtn.setEnabled(!busy);
        declineBtn.setEnabled(!busy);
        if (busy) {
            acceptBtn.setText(labelIfBusy);
            declineBtn.setText(labelIfBusy);
        } else {
            acceptBtn.setText("Accept");
            declineBtn.setText("Decline");
        }
    }

    private void showInviteStatus(String msg) {
        if (inviteStatusText != null) {
            inviteStatusText.setText(msg);
            inviteStatusText.setVisibility(View.VISIBLE);
        }
        if (acceptBtn != null) acceptBtn.setEnabled(false);
        if (declineBtn != null) declineBtn.setEnabled(false);
    }

    // ----- Waiting list helpers (only when no invite) -----

    private void loadWaitingListCount() {
        waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
            @Override public void onSuccess(int count) {
                waitingListCountText.setText(count + " People have joined the waiting list");
            }
            @Override public void onFailure(Exception e) {
                waitingListCountText.setText("Unable to load waiting list count");
            }
        });
    }

    private void checkIfUserInWaitingList() {
        if (joinButton == null) return;
        waitingListRepository.isUserInWaitingList(eventId, userId,
                new WaitingListRepository.OnCheckListener() {
                    @Override public void onSuccess(boolean exists) {
                        if (exists) {
                            joinButton.setText("Already in Waiting List");
                            joinButton.setEnabled(false);
                        } else {
                            joinButton.setText("Join Waiting List");
                            joinButton.setEnabled(true);
                        }
                    }
                    @Override public void onFailure(Exception e) {
                        joinButton.setEnabled(true);
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
            @Override public void onSuccess() {
                Toast.makeText(requireContext(), "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                joinButton.setText("Already in Waiting List");
                loadWaitingListCount();
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                joinButton.setEnabled(true);
                joinButton.setText("Join Waiting List");
            }
        });
    }
}
