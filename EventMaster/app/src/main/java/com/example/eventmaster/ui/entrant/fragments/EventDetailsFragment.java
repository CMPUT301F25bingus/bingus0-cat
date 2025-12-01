package com.example.eventmaster.ui.entrant.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.DialogInterface;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventRepository;
import com.example.eventmaster.data.api.WaitingListRepository;
import com.example.eventmaster.data.firestore.EventRepositoryFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.WaitingListRepositoryFs;
import com.example.eventmaster.data.firestore.InvitationServiceFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Invitation;
import com.example.eventmaster.model.Profile;
import com.example.eventmaster.model.WaitingListEntry;
import com.example.eventmaster.utils.DeviceUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
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

    private static final String TAG = "EventDetailsFragment";
    private static final String ARG_EVENT_ID = "event_id";

    private EventRepository eventRepository;
    private WaitingListRepository waitingListRepository;
    private InvitationServiceFs invitationService;
    private RegistrationServiceFs registrationService;

    private String eventId;
    private Event currentEvent;
    private String userId;
    private boolean isInWaitingList = false;  // Track if user is in waiting list

    private Profile currentProfile;  //needed to attach into waiting list entry and loaded using deviceID
    private ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    // UI Elements
    private ImageView posterImage;
    private ImageView backButton;
    private MaterialButton qrButton;
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

    // Replacement lottery
    private View replacementLotterySection;
    private TextView replacementLotteryText;
    private MaterialButton btnJoinReplacementLottery;

    private boolean testMode = false;
    private boolean testForceInvited = false;
    
    // Firestore listeners for real-time updates
    private ListenerRegistration invitationListener;

    //when invite is recived show time left to reply
    private TextView inviteCountdownText;


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
        profileRepo = new ProfileRepositoryFs();

        userId = DeviceUtils.getDeviceId(requireContext());
        loadUserProfile();

        // Debug logging
        Log.d(TAG, "onCreate: eventId=" + eventId + ", userId=" + userId);
    }

    /**
     * Retrieves the user's profile using the device ID. If no existing profile
     * is found, it tries to get Firebase UID and create profile properly.
     * This ensures that every entrant interacting with the system has a valid profile record.
     */
    private void loadUserProfile() {
        profileRepo.getByDeviceId(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        currentProfile = profile;
                        Log.d(TAG, "Loaded profile for device: " + profile.getName());
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
                                            Log.d(TAG, "Loaded profile by Firebase UID: " + existingProfile.getName());
                                        } else {
                                            // No profile by UID either, create new one with Firebase UID
                                            Profile newProf = new Profile();
                                            newProf.setUserId(firebaseUser.getUid());
                                            newProf.setDeviceId(userId);
                                            newProf.setName("Guest User");
                                            newProf.setEmail("");
                                            newProf.setRole("entrant");
                                            newProf.setActive(true);
                                            newProf.setBanned(false);
                                            profileRepo.upsert(newProf);
                                            currentProfile = newProf;
                                            Log.d(TAG, "Created new profile with Firebase UID.");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error getting by UID, create new profile with Firebase UID
                                        Profile newProf = new Profile();
                                        newProf.setUserId(firebaseUser.getUid());
                                        newProf.setDeviceId(userId);
                                        newProf.setName("Guest User");
                                        newProf.setEmail("");
                                        newProf.setRole("entrant");
                                        newProf.setActive(true);
                                        newProf.setBanned(false);
                                        profileRepo.upsert(newProf);
                                        currentProfile = newProf;
                                        Log.d(TAG, "Created new profile with Firebase UID (fallback).");
                                    });
                        } else {
                            // Not signed in - this shouldn't happen for entrants, but handle gracefully
                            Log.w(TAG, "No Firebase user signed in, cannot create profile properly");
                            // Don't create profile with deviceId as userId - wait for proper sign-in
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Profile load failed", e);
                    // Try to get Firebase user as fallback
                    com.google.firebase.auth.FirebaseUser firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null && firebaseUser.getUid() != null) {
                        profileRepo.get(firebaseUser.getUid())
                                .addOnSuccessListener(profile -> {
                                    if (profile != null) {
                                        currentProfile = profile;
                                        Log.d(TAG, "Loaded profile by Firebase UID (fallback): " + profile.getName());
                                    }
                                });
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.entrant_fragment_event_details, container, false);

        // Initialize UI
        posterImage = view.findViewById(R.id.event_poster_image);
        backButton = view.findViewById(R.id.back_button);
        qrButton = view.findViewById(R.id.btn_show_qr);
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
        inviteCountdownText = view.findViewById(R.id.invite_countdown_text);
        btnAccept = view.findViewById(R.id.btnAccept);
        btnDecline = view.findViewById(R.id.btnDecline);

        replacementLotterySection = view.findViewById(R.id.replacement_lottery_section);
        replacementLotteryText = view.findViewById(R.id.replacement_lottery_text);
        btnJoinReplacementLottery = view.findViewById(R.id.btnJoinReplacementLottery);

        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);
        replacementLotterySection.setVisibility(View.GONE);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        qrButton.setOnClickListener(v -> showQrDialog());
        joinButton.setOnClickListener(v -> handleJoinOrExitWaitingList());

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore listeners to prevent memory leaks
        if (invitationListener != null) {
            invitationListener.remove();
            invitationListener = null;
        }
    }

    /** Loads event details from Firestore and displays them. */
    private void loadEventDetails() {
        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                displayEventDetails(event);
                loadWaitingListCountWithLimit(event);
                // loadWaitingListCount(); CODE CHECK
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Sets up real-time listener for invitation changes
     * If an invitation exists, the invitation UI is shown. If no invitation
     * is found, the join waiting list button is shown. Automatically calls
     *the expiration handler if the reply-by deadline has passed. */
    private void decideInviteOrJoin() {
        // Remove old listener if it exists
        if (invitationListener != null) {
            invitationListener.remove();
        }
        
        // Set up real-time listener for invitations
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        invitationListener = db.collection("events")
                .document(eventId)
                .collection("invitations")
                .whereEqualTo("entrantId", userId)
                .limit(1)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to invitations", error);
                        showJoinButtonWithState();
                        return;
                    }
                    
                    if (snapshots != null && !snapshots.isEmpty()) {
                        Invitation inv = snapshots.getDocuments().get(0).toObject(Invitation.class);
                        if (inv != null) {
                            inv.setId(snapshots.getDocuments().get(0).getId());
                            Log.d(TAG, "📩 Invitation status changed: " + inv.getStatus());

                            //AUTO-EXPIRE INVITATION IF DEADLINE PASSED
                            if (inv.getReplyBy() != null && "PENDING".equals(inv.getStatus())) {

                                Date now = new Date();
                                Date deadline = inv.getReplyBy();

                                if (now.after(deadline)) {
                                    autoExpireInvitation(inv);
                                    return; // stop normal flow (DO NOT show normal invite UI)
                                }
                            }

                            showInvitationInclude(inv);
                        } else {
                            showJoinButtonWithState();
                        }
                    } else {
                        // No invitation found
                        showJoinButtonWithState();
                    }
                });
    }

    /**
     * Populates the invitation action UI based on the current invitation status.
     * Handles pending, accepted, declined, and organizer-cancelled states.
     * Enables and disables Accept/Decline buttons appropriately.
     *
     * @param inv the invitation object retrieved from Firestore
     */
    private void showInvitationInclude(@NonNull Invitation inv) {
        inviteInclude.setVisibility(View.VISIBLE);
        joinButton.setVisibility(View.GONE);

        // SHOW COUNTDOWN TIMER (only when pending & replyBy set)
        if (inv.getReplyBy() != null && "PENDING".equals(inv.getStatus())) {
            inviteCountdownText.setVisibility(View.VISIBLE);
            inviteCountdownText.setText(getCountdownText(inv.getReplyBy()));
        } else {
            inviteCountdownText.setVisibility(View.GONE);
        }


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
                            
                            // Send notification when invitation is accepted
                            sendInvitationAcceptedNotification(eventId, userId);
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
                if (inv.getStatus().equals("CANCELLED_BY_ORGANIZER")) {
                    inviteStatusText.setText("Did not meet the reply deadline");
                } else {
                    inviteStatusText.setText("Invitation declined");
                }
                inviteStatusText.setVisibility(View.VISIBLE);
                btnAccept.setEnabled(false);
                btnDecline.setEnabled(false);
                break;
        }
    }

    /**
     * Generates a countdown message showing the remaining
     * time an entrant has to respond to an invitation.
     *
     * @param replyBy the reply deadline timestamp
     * @return a formatted string such as "2 days left to reply"
     */
    private String getCountdownText(Date replyBy) {
        long now = System.currentTimeMillis();
        long diff = replyBy.getTime() - now;

        if (diff <= 0) {
            return "Reply deadline passed";
        }

        long days = diff / (1000 * 60 * 60 * 24);
        long hours = (diff / (1000 * 60 * 60)) % 24;
        long minutes = (diff / (1000 * 60)) % 60;

        if (days > 0) return days + " day" + (days == 1 ? "" : "s") + " left to reply";
        if (hours > 0) return hours + " hour" + (hours == 1 ? "" : "s") + " left to reply";
        return minutes + " minute" + (minutes == 1 ? "" : "s") + " left to reply";
    }

    /**
     * Enables or disables both Accept and Decline buttons in the invitation UI.
     *
     * @param enabled true to enable buttons, false to disable them
     */
    private void setInviteButtonsEnabled(boolean enabled) {
        btnAccept.setEnabled(enabled);
        btnDecline.setEnabled(enabled);
    }

    /**
     * Hides the invitation UI and displays the join waiting list button.
     * Also checks if the user appears in the not_selected list, which updates
     * the button text accordingly.
     */
    private void showJoinButtonWithState() {
        inviteInclude.setVisibility(View.GONE);
        joinButton.setVisibility(View.VISIBLE);
        // checkNotSelected(); // CODE CHECK


        // CODE CHECK - ACCEPTED START

        checkIfUserInWaitingList();
        // Check if user is eligible for replacement lottery
        checkReplacementLotteryEligibility();
    }

    /**
     * Checks if user is eligible for replacement lottery (when someone else declines).
     * Implements US 01.05.01 - Get another chance if selected user declines.
     */
    private void checkReplacementLotteryEligibility() {
        // Only show if no active invitation exists
        if (inviteInclude.getVisibility() == View.VISIBLE) {
            replacementLotterySection.setVisibility(View.GONE);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if user has a replacement lottery notification for this event
        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", "REPLACEMENT_LOTTERY_AVAILABLE")
                .whereEqualTo("isRead", false)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User has replacement lottery notification, show option
                        showReplacementLotteryOption();
                    } else {
                        // Check if user was not selected initially
                        checkIfCanRejoinForReplacement();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking replacement lottery eligibility", e);
                    checkIfCanRejoinForReplacement();
                });
    }

    /**
     * Checks if user was not selected initially and can rejoin waiting list for replacement lottery.
     */
    private void checkIfCanRejoinForReplacement() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if user was previously not selected (has LOTTERY_NOT_SELECTED notification)
        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("type", "LOTTERY_NOT_SELECTED")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User was not selected, check if they can rejoin waiting list
                        showReplacementLotteryOption();
                    } else {
                        replacementLotterySection.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if can rejoin for replacement", e);
                    replacementLotterySection.setVisibility(View.GONE);
                });
    }

    /**
     * Shows the replacement lottery option UI.
     */
    private void showReplacementLotteryOption() {
        // Check if user is already in waiting list
        waitingListRepository.isUserInWaitingList(eventId, userId,
                new WaitingListRepository.OnCheckListener() {
                    @Override
                    public void onSuccess(boolean exists) {
                        if (exists) {
                            replacementLotterySection.setVisibility(View.GONE);
                        } else {
                            replacementLotterySection.setVisibility(View.VISIBLE);
                            replacementLotteryText.setText(
                                "A spot has opened up! Join the waiting list for another chance to be selected. 🎲"
                            );

                            btnJoinReplacementLottery.setOnClickListener(v -> {
                                btnJoinReplacementLottery.setEnabled(false);
                                btnJoinReplacementLottery.setText("Joining...");
                                handleJoinWaitingListForReplacement();
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        replacementLotterySection.setVisibility(View.VISIBLE);
                        replacementLotteryText.setText(
                            "A spot has opened up! Join the waiting list for another chance to be selected. 🎲"
                        );

                        btnJoinReplacementLottery.setOnClickListener(v -> {
                            btnJoinReplacementLottery.setEnabled(false);
                            btnJoinReplacementLottery.setText("Joining...");
                            handleJoinWaitingListForReplacement();
                        });
                    }
                });
    }

    /**
     * Handle joining the waiting list from replacement lottery section.
     */
    private void handleJoinWaitingListForReplacement() {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty() || currentEvent == null) {
            Toast.makeText(requireContext(), "Error: Missing information", Toast.LENGTH_LONG).show();
            btnJoinReplacementLottery.setEnabled(true);
            btnJoinReplacementLottery.setText("Join Replacement Lottery");
            return;
        }

        if (currentEvent.getRegistrationStartDate() == null || currentEvent.getRegistrationEndDate() == null) {
            Toast.makeText(requireContext(), "Registration dates not available yet", Toast.LENGTH_SHORT).show();
            btnJoinReplacementLottery.setEnabled(true);
            btnJoinReplacementLottery.setText("Join Replacement Lottery");
            return;
        }

        Date now = new Date();
        if (now.before(currentEvent.getRegistrationStartDate()) || now.after(currentEvent.getRegistrationEndDate())) {
            Toast.makeText(requireContext(), "Registration is not open", Toast.LENGTH_SHORT).show();
            btnJoinReplacementLottery.setEnabled(true);
            btnJoinReplacementLottery.setText("Join Replacement Lottery");
            return;
        }

        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(entryId, eventId, userId, new Date());

        waitingListRepository.addToWaitingList(entry, new WaitingListRepository.OnWaitingListOperationListener() {
            @Override
            public void onSuccess() {
                isInWaitingList = true;

                if (joinButton.getVisibility() == View.VISIBLE) {
                    joinButton.setText("Exit Waiting List");
                    joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Red color
                    joinButton.setEnabled(true);
                }

                // Send notification when joining replacement lottery
                sendJoinedWaitingListNotification(eventId, userId);

                if (currentEvent != null) {
                    loadWaitingListCountWithLimit(currentEvent);
                }
                replacementLotterySection.setVisibility(View.GONE);
                btnJoinReplacementLottery.setEnabled(true);
                btnJoinReplacementLottery.setText("Join Replacement Lottery");
            }

            @Override
            public void onFailure(Exception e) {
                btnJoinReplacementLottery.setEnabled(true);
                btnJoinReplacementLottery.setText("Join Replacement Lottery");
            }
        });
    }

    // CODE CHECK - ACCEPTED END ^

    /** Displays event details in the UI. */
    private void displayEventDetails(Event event) {
        eventNameText.setText(event.getName() != null ? event.getName() : "Unnamed Event");
        String organizerId = event.getOrganizerId();

        if (organizerId != null) {
            FirebaseFirestore.getInstance()
                    .collection("profiles")
                    .document(organizerId)
                    .get()
                    .addOnSuccessListener(profileDoc -> {
                        if (profileDoc.exists()) {
                            String organizerName = profileDoc.getString("name");

                            if (organizerName != null && !organizerName.isEmpty()) {
                                organizerText.setText("Hosted by: " + organizerName);
                            } else {
                                organizerText.setText("Hosted by: " + organizerId);
                            }

                        } else {
                            organizerText.setText("Hosted by: " + organizerId);
                        }
                    })
                    .addOnFailureListener(e -> organizerText.setText("Hosted by: " + organizerId));
        } else {
            organizerText.setText("Hosted by: Unknown");
        }

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

        if (event.getQrUrl() != null && !event.getQrUrl().isEmpty()) {
            qrButton.setVisibility(View.VISIBLE);
            qrButton.setEnabled(true);
        } else {
            qrButton.setVisibility(View.GONE);
        }
    }

    /**
     * Loads the number of users currently in the waiting list and updates
     * the UI to reflect whether the entrant may join, exit, or is blocked
     * due to a full waiting list. Applies waiting list limit rules defined
     * by the organizer.
     *
     * @param event the event containing waiting list configuration
     */
    private void loadWaitingListCountWithLimit(Event event) {

        Integer waitingLimit = event.getWaitingListLimit();   // null = unlimited

        waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
            @Override
            public void onSuccess(int count) {
                waitingListCountText.setText(count + " people have joined the waiting list");

                // CODE CHECK START

                // If user is already in the WL → always allow them to leave
                waitingListRepository.isUserInWaitingList(eventId, userId, new WaitingListRepository.OnCheckListener() {
                    @Override
                    public void onSuccess(boolean exists) {

                        isInWaitingList = exists;

                        // If user already joined → allow them to exit even if full
                        if (exists) {
                            joinButton.setEnabled(true);
                            joinButton.setText("Exit Waiting List");
                            return;
                        }

                        // --- If no limit set → normal behaviour ---
                        if (waitingLimit == null || waitingLimit == 0) {
                            joinButton.setEnabled(true);
                            joinButton.setText("Join Waiting List");
                            return;
                        }

                        // --- If FULL → disable joining ---
                        if (count >= waitingLimit) {
                            joinButton.setEnabled(false);
                            joinButton.setText("Waiting List Full");
                        } else {
                            joinButton.setEnabled(true);
                            joinButton.setText("Join Waiting List");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        joinButton.setEnabled(true);
                        joinButton.setText("Join Waiting List");
                    }
                });
            }

            // CODE CHECK END ^

            @Override
            public void onFailure(Exception e) {
                waitingListCountText.setText("Unable to load waiting list count");
            }
        });
    }

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
                .placeholder(R.drawable.ic_launcher_background)
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

    /**
     * Checks whether the user is currently in the event's waiting list.
     * Updates the join button text and enabled state to match the user's
     * status.
     */
    private void checkIfUserInWaitingList() {
        if (eventId == null || userId == null) {
            Log.w(TAG, "checkIfUserInWaitingList  skipped: NULL id(s).");
            return;
        }
        waitingListRepository.isUserInWaitingList(eventId, userId,
                new WaitingListRepository.OnCheckListener() {
                    @Override
                    public void onSuccess(boolean exists) {
                        isInWaitingList = exists;
                        if (exists) {
                            joinButton.setText("Exit Waiting List");
                            joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Red color
                            joinButton.setEnabled(true);
                        } else {
                            joinButton.setText("Join Waiting List");
                            joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D8B87)); // Teal color
                            joinButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        isInWaitingList = false;
                        joinButton.setEnabled(true);
                        joinButton.setText("Join Waiting List");
                        joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D8B87)); // Teal color
                    }
                });
    }

    /**
     * Handle join or exit based on current state
     */
    private void handleJoinOrExitWaitingList() {
        if (isInWaitingList) {
            handleExitWaitingList();
        } else {
            handleJoinWaitingList();
        }
    }
    private void requestLocationThenJoin() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fetchLocationAndJoin();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocationAndJoin();
                } else {
                    Toast.makeText(requireContext(),
                            "Location required to join this event",
                            Toast.LENGTH_SHORT).show();
                }
            });

    private void fetchLocationAndJoin() {
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
                        // pass lat/lng into join method
                        joinWaitingListWithLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(requireContext(),
                                "Unable to fetch location",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinWaitingListWithLocation(double lat, double lng) {
        String entryId = UUID.randomUUID().toString();

        // CODE CHECK START - COMMENTED

//        Map<String, Object> data = new HashMap<>();
//        data.put("entryId", entryId);
//        data.put("eventId", eventId);
//        data.put("userId", userId);
//        data.put("joinedAt", new Date());
//        data.put("lat", lat);
//        data.put("lng", lng);
        WaitingListEntry entry = new WaitingListEntry(
                entryId,
                eventId,
                userId,
                new Date()
        );
        entry.setProfile(currentProfile);  //ensure profile is set for organizer later
        entry.setlat(lat);
        entry.setlng(lng);

        // CODE CHECK END ^


        // CODE CHECK ENTRANT START
//        FirebaseFirestore.getInstance()
//                .collection("events")
//                .document(eventId)
//                .collection("waiting_list")
//                .document(userId)
//                .set(data)
//                .addOnSuccessListener(unused -> {
//                    isInWaitingList = true;
//                    joinButton.setText("Exit Waiting List");
//                    joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Red color
//                    joinButton.setEnabled(true);
//                    loadWaitingListCount();
//                })
//                .addOnFailureListener(e -> {
//                    joinButton.setEnabled(true);
//                    joinButton.setText("Join Waiting List");
//                    joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D8B87)); // Teal color
//                });

        // CODE CHECK ENTRANT END ^

        // CODE CHECK DEV START (CHOOSE ONE UP OR DOWN)

        ((WaitingListRepositoryFs) waitingListRepository)
            .joinWithLimitCheck(entry, new WaitingListRepository.OnWaitingListOperationListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(requireContext(),
                        "Successfully joined waiting list!",
                        Toast.LENGTH_SHORT).show();

                isInWaitingList = true;
                loadEventDetails(); // refresh count & button state
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(),
                        e.getMessage(),
                        Toast.LENGTH_LONG).show();
                loadEventDetails();
            }
        });
    }

    // CODE CHECK DEV END ^

    /**
     * Handles the standard process of joining the waiting list when no
     * geolocation requirement exists. Validates registration dates, event
     * data, and user identity before creating a waiting list entry.
     */
    private void handleJoinWaitingList() {
        // --- GEOLOCATION REQUIREMENT CHECK ---
        if (currentEvent != null && currentEvent.isGeolocationRequired()) {
            requestLocationThenJoin();
            return; // stop normal joining path
        }
        // Validate eventId and userId first
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: Event ID is missing", Toast.LENGTH_LONG).show();
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(requireContext(), "Error: User ID is missing", Toast.LENGTH_LONG).show();
            return;
        }

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

        // Show guidelines dialog before joining
        showGuidelinesDialog();
    }

    /**
     * Shows guidelines dialog that user must accept before joining waiting list
     */
    private void showGuidelinesDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setCancelable(true);

        // Inflate custom layout
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_guidelines, null);
        builder.setView(dialogView);

        // Get views from custom layout
        com.google.android.material.checkbox.MaterialCheckBox checkbox = dialogView.findViewById(R.id.guidelines_checkbox);
        com.google.android.material.button.MaterialButton cancelButton = dialogView.findViewById(R.id.btn_cancel);
        com.google.android.material.button.MaterialButton acceptButton = dialogView.findViewById(R.id.btn_accept);

        // Create dialog
        android.app.AlertDialog dialog = builder.create();

        // Set window properties for rounded corners and padding
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        params.horizontalMargin = 0.02f; // 2% margin on each side - reduced for more space
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        // Add margins to prevent edge clipping
        dialog.getWindow().setDimAmount(0.5f);

        // Cancel button action
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Checkbox listener - enable/disable accept button
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            acceptButton.setEnabled(isChecked);
            if (isChecked) {
                acceptButton.setAlpha(1.0f);
            } else {
                acceptButton.setAlpha(0.5f);
            }
        });

        // Accept button action
        acceptButton.setOnClickListener(v -> {
            if (checkbox.isChecked()) {
                dialog.dismiss();
                // Proceed with joining (check geolocation if required)
                if (currentEvent != null && currentEvent.isGeolocationRequired()) {
                    requestLocationThenJoin();
                } else {
                    proceedWithJoin();
                }
            }
        });

        dialog.show();
    }

    /**
     * Proceeds with joining the waiting list after guidelines acceptance
     */
    private void proceedWithJoin() {
        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(entryId, eventId, userId, new Date());

        entry.setProfile(currentProfile);

        Log.d(TAG, "proceedWithJoin: Creating entry with entryId=" + entryId +
                ", eventId=" + eventId + ", userId=" + userId);

        joinButton.setEnabled(false);
        joinButton.setText("Joining...");

        // CODE CHECK DEV START
        ((WaitingListRepositoryFs) waitingListRepository)
                .joinWithLimitCheck(entry, new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                        isInWaitingList = true;

                        // Send notification when user joins waiting list
                        sendJoinedWaitingListNotification(eventId, userId);

                        loadEventDetails(); // refresh limit + button
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        loadEventDetails(); // refresh button state
                    }
                });

    }

    // CODE CHECK DEV END ^

    // CODE CHECK ENTRANT START ( CHOOSE ONE UP OR DOWN)

//     waitingListRepository.addToWaitingList(entry, new WaitingListRepository.OnWaitingListOperationListener() {
//            @Override
//            public void onSuccess() {
//                Log.d(TAG, "Successfully joined waiting list");
//                isInWaitingList = true;
//                joinButton.setText("Exit Waiting List");
//                joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252)); // Red color
//                joinButton.setEnabled(true);
//                loadWaitingListCount();
//
//                // Send notification to user
//                sendJoinedWaitingListNotification(eventId, userId);
//            }
//
//            @Override
//            public void onFailure(Exception e) {
//                Log.e(TAG, "Failed to join waiting list", e);
//                joinButton.setEnabled(true);
//                joinButton.setText("Join Waiting List");
//                joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D8B87)); // Teal color
//            }
//        });
//    }

    // CODE CHECK ENTRANT END ^



    /**
     * Handle exiting the waiting list
     */
    private void handleExitWaitingList() {
        joinButton.setEnabled(false);
        joinButton.setText("Leaving...");

        // Cast to WaitingListRepositoryFs to access the new remove method
        if (waitingListRepository instanceof WaitingListRepositoryFs) {
            WaitingListRepositoryFs repo = (WaitingListRepositoryFs) waitingListRepository;
            repo.removeFromWaitingList(eventId, userId, new WaitingListRepository.OnWaitingListOperationListener() {
                @Override
                public void onSuccess() {
                    isInWaitingList = false;
                    joinButton.setText("Join Waiting List");
                    joinButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3D8B87)); // Teal color
                    joinButton.setEnabled(true);
                    if (currentEvent != null) {
                        loadWaitingListCountWithLimit(currentEvent);
                    }
                    // loadEventDetails(); // CODE CHECK DEV IF ACCEPTED DELETE ALL FROM  JOIN BUTTON
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(requireContext(), "Failed to leave: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    joinButton.setEnabled(true);
                    joinButton.setText("Exit Waiting List");
                }
            });
        }
    }

    /**
     * Send notification when user joins waiting list
     */
    private void sendJoinedWaitingListNotification(String eventId, String userId) {
        if (eventId == null || userId == null) {
            Log.w(TAG, "Cannot send notification: missing eventId or userId");
            return;
        }
        
        // For entrants: ALWAYS use deviceId as recipientUserId (userId parameter IS the deviceId)
        // Don't use Firebase UID even if available - entrants are identified by deviceId
        final String recipientUserId = userId; // deviceId for entrants
        final String deviceIdForNotification = userId;
        
        Log.d(TAG, "Sending notification: eventId=" + eventId + ", recipientUserId=" + recipientUserId + " (deviceId), deviceId=" + deviceIdForNotification);
        
        // Helper method to create and send notification
        java.util.function.Consumer<String> sendNotification = (eventName) -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("eventId", eventId);
            notification.put("recipientUserId", recipientUserId); // deviceId
            notification.put("recipientId", recipientUserId); // deviceId (legacy)
            notification.put("deviceId", deviceIdForNotification); // deviceId
            notification.put("senderUserId", "system");
            notification.put("type", "GENERAL");
            notification.put("title", "Joined Waiting List");
            notification.put("message", "You've successfully joined the waiting list for \"" + eventName + "\". Good luck!");
            notification.put("isRead", false);
            notification.put("sentAt", com.google.firebase.Timestamp.now());
            
            db.collection("notifications")
                    .add(notification)
                    .addOnSuccessListener(docRef -> {
                        Log.d(TAG, "✅ Sent waiting list notification to deviceId: " + deviceIdForNotification);
                        docRef.update("notificationId", docRef.getId());
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to send notification", e));
        };
        
        // Try to get profile by deviceId (for entrants, profile doc ID = deviceId)
        profileRepo.getByDeviceId(deviceIdForNotification)
                .addOnSuccessListener(profile -> {
                    // Only send notification if user has notifications enabled
                    if (profile != null && profile.isNotificationsEnabled()) {
                        // Fetch event details
                        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
                            @Override
                            public void onSuccess(Event event) {
                                String eventName = event != null ? event.getName() : "this event";
                                sendNotification.accept(eventName);
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to fetch event for notification", e);
                                sendNotification.accept("this event");
                            }
                        });
                    } else {
                        Log.d(TAG, "⏭️ Skipping waiting list notification (opted out)");
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't fetch profile, still send notification (backward compatibility)
                    Log.w(TAG, "⚠️ Could not fetch profile for deviceId " + deviceIdForNotification + ", sending notification anyway", e);
                    eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
                        @Override
                        public void onSuccess(Event event) {
                            String eventName = event != null ? event.getName() : "this event";
                            sendNotification.accept(eventName);
                        }
                        
                        @Override
                        public void onFailure(Exception err) {
                            Log.e(TAG, "Failed to fetch event for notification", err);
                            sendNotification.accept("this event");
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

    /**
     * Send notification when user accepts an invitation and gets enrolled
     */
    private void sendInvitationAcceptedNotification(String eventId, String userId) {
        if (eventId == null || userId == null) {
            Log.w(TAG, "Cannot send notification: missing eventId or userId");
            return;
        }
        
        // Get the Firebase Auth UID if available, otherwise use the provided userId (deviceId)
        final String recipientUserId = getCurrentFirebaseUserId() != null ? getCurrentFirebaseUserId() : userId;
        final String deviceIdForNotification = userId;
        
        Log.d(TAG, "Sending invitation accepted notification: eventId=" + eventId + ", recipientUserId=" + recipientUserId);
        
        // First, check if user has notifications enabled
        profileRepo.get(recipientUserId)
                .addOnSuccessListener(profile -> {
                    // Only send notification if user has notifications enabled
                    if (profile != null && profile.isNotificationsEnabled()) {
                        // Fetch event details to include event name
                        eventRepository.getEventById(eventId, new EventRepository.OnEventListener() {
                            @Override
                            public void onSuccess(Event event) {
                                String eventName = event != null ? event.getName() : "this event";
                                
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                
                                Map<String, Object> notification = new HashMap<>();
                                notification.put("eventId", eventId);
                                notification.put("recipientUserId", recipientUserId);
                                notification.put("recipientId", recipientUserId); // Legacy field
                                // Also store deviceId for query flexibility
                                if (!recipientUserId.equals(deviceIdForNotification)) {
                                    notification.put("deviceId", deviceIdForNotification);
                                }
                                notification.put("senderUserId", "system");
                                notification.put("type", "INVITATION");
                                notification.put("title", "🎉 You're Enrolled!");
                                notification.put("message", "Congratulations! You've successfully enrolled in \"" + eventName + "\". We look forward to seeing you there!");
                                notification.put("isRead", false);
                                notification.put("sentAt", com.google.firebase.Timestamp.now());
                                
                                db.collection("notifications")
                                        .add(notification)
                                        .addOnSuccessListener(docRef -> {
                                            Log.d(TAG, "✅ Sent invitation accepted notification to userId: " + recipientUserId);
                                            docRef.update("notificationId", docRef.getId());
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to send notification", e));
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to fetch event for notification", e);
                                // Fallback message
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                Map<String, Object> notification = new HashMap<>();
                                notification.put("eventId", eventId);
                                notification.put("recipientUserId", recipientUserId);
                                notification.put("recipientId", recipientUserId);
                                if (!recipientUserId.equals(deviceIdForNotification)) {
                                    notification.put("deviceId", deviceIdForNotification);
                                }
                                notification.put("senderUserId", "system");
                                notification.put("type", "INVITATION");
                                notification.put("title", "🎉 You're Enrolled!");
                                notification.put("message", "Congratulations! You've successfully enrolled in this event. We look forward to seeing you there!");
                                notification.put("isRead", false);
                                notification.put("sentAt", com.google.firebase.Timestamp.now());
                                
                                db.collection("notifications")
                                        .add(notification)
                                        .addOnSuccessListener(docRef -> {
                                            Log.d(TAG, "✅ Sent invitation accepted notification (fallback) to userId: " + recipientUserId);
                                            docRef.update("notificationId", docRef.getId());
                                        })
                                        .addOnFailureListener(err -> Log.e(TAG, "❌ Failed to send notification", err));
                            }
                        });
                    } else {
                        Log.d(TAG, "⏭️ Skipping enrollment notification for " + recipientUserId + " (opted out)");
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't fetch profile, default to sending notification (backward compatibility)
                    Log.w(TAG, "⚠️ Could not fetch profile for " + recipientUserId + ", sending notification anyway", e);
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
                            notification.put("type", "INVITATION");
                            notification.put("title", "🎉 You're Enrolled!");
                            notification.put("message", "Congratulations! You've successfully enrolled in \"" + eventName + "\". We look forward to seeing you there!");
                            notification.put("isRead", false);
                            notification.put("sentAt", com.google.firebase.Timestamp.now());
                            
                            db.collection("notifications")
                                    .add(notification)
                                    .addOnSuccessListener(docRef -> {
                                        Log.d(TAG, "✅ Sent invitation accepted notification to userId: " + recipientUserId + " (profile fetch failed)");
                                        docRef.update("notificationId", docRef.getId());
                                    })
                                    .addOnFailureListener(err -> Log.e(TAG, "❌ Failed to send notification", err));
                        }
                        
                        @Override
                        public void onFailure(Exception err) {
                            Log.e(TAG, "Failed to fetch event for notification", err);
                        }
                    });
                });
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }


    /**
     * Automatically updates an invitation to CANCELLED_BY_ORGANIZER when
     * the reply-by deadline is missed. Performs a Firestore batch operation
     * that:
     * 1) Cancels the registration
     * 2) Updates the invitation document
     * 3) Removes the entrant from the chosen_list collection
     *
     * @param inv the invitation that has expired
     */
    private void autoExpireInvitation(Invitation inv) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String invitationId = inv.getId();
        String entrantId = inv.getEntrantId();

        // 1) Cancel registration (same as organizer button logic)
        Map<String, Object> reg = new HashMap<>();
        reg.put("eventId", eventId);
        reg.put("entrantId", entrantId);
        reg.put("status", "CANCELLED_BY_ORGANIZER");
        reg.put("cancelledAtUtc", new Date());

        DocumentReference regRef = db.collection("events")
                .document(eventId)
                .collection("registrations")
                .document(entrantId);

        // 2) Update the invitation status
        DocumentReference invRef = db.collection("events")
                .document(eventId)
                .collection("invitations")
                .document(invitationId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "CANCELLED_BY_ORGANIZER");
        updates.put("autoExpiredAt", new Date());

        // 3) Remove from chosen_list (same as organizer)
        DocumentReference chosenRef = db.collection("events")
                .document(eventId)
                .collection("chosen_list")
                .document(entrantId);

        WriteBatch batch = db.batch();

        batch.set(regRef, reg, SetOptions.merge());
        batch.update(invRef, updates);
        batch.delete(chosenRef);

        batch.commit()
                .addOnSuccessListener(v -> {
                    inviteStatusText.setVisibility(View.VISIBLE);
                    inviteStatusText.setText("Did not meet reply deadline");

                    inviteCountdownText.setVisibility(View.GONE);

                    btnAccept.setEnabled(false);
                    btnDecline.setEnabled(false);
                })
                .addOnFailureListener(e -> Log.e("AutoExpire", "Failed to expire invitation", e));
    }

    /**
     * Want to check if they are not selected when the lottery is ran... if that is the case, button should reflect that
     */
    private void checkNotSelected() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("not_selected")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // User was NOT SELECTED in the last lottery
                        joinButton.setText("Unfortunately not selected for now");
                        joinButton.setEnabled(false);
                    } else {
                        // If not in not_selected, continue normal WL check
                        checkIfUserInWaitingList();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check not_selected collection", e);
                    checkIfUserInWaitingList();
                });
    }



}
