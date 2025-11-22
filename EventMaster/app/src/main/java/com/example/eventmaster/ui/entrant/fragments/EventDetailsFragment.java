package com.example.eventmaster.ui.entrant.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

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
    
    // Firestore listeners for real-time updates
    private ListenerRegistration invitationListener;

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

    private void loadUserProfile() {
        profileRepo.getByDeviceId(userId)
                .addOnSuccessListener(profile -> {
                    if (profile != null) {
                        currentProfile = profile;
                        Log.d(TAG, "Loaded profile for device: " + profile.getName());
                    } else {
                        //
                        Profile newProf = new Profile(
                                userId,
                                "Guest User",
                                "",
                                null
                        );
                        profileRepo.upsert(newProf);
                        currentProfile = newProf;
                        Log.d(TAG, "Created new profile for device.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Profile load failed, creating fallback profile", e);
                    Profile fallback = new Profile(userId, "Guest User", "", null);
                    profileRepo.upsert(fallback);
                    currentProfile = fallback;
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
//                loadWaitingListCount();
                loadWaitingListCountWithLimit(event);

            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(requireContext(), "Failed to load event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Sets up real-time listener for invitation changes */
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

        // ✅ Load QR image from Firestore (not generate locally)
        if (event.getQrUrl() != null && !event.getQrUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(event.getQrUrl())
                    .into(qrCodeImage);
        } else {
            qrCodeImage.setVisibility(View.GONE);
        }
    }

    /** Loads waiting list count AND applies waitingListLimit rules properly. */
    private void loadWaitingListCountWithLimit(Event event) {

        Integer waitingLimit = event.getWaitingListLimit();   // null = unlimited

        waitingListRepository.getWaitingListCount(eventId, new WaitingListRepository.OnCountListener() {
            @Override
            public void onSuccess(int count) {

                waitingListCountText.setText(count + " people have joined the waiting list");

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
                            joinButton.setEnabled(true);
                        } else {
                            joinButton.setText("Join Waiting List");
                            joinButton.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        isInWaitingList = false;
                        joinButton.setEnabled(true);
                        joinButton.setText("Join Waiting List");
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

        Map<String, Object> data = new HashMap<>();
        data.put("entryId", entryId);
        data.put("eventId", eventId);
        data.put("userId", userId);
        data.put("joinedAt", new Date());
        data.put("lat", lat);
        data.put("lng", lng);

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("waitingList")   // <-- FIXED
                .document(userId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Joined event!", Toast.LENGTH_SHORT).show();
                    isInWaitingList = true;
                    joinButton.setText("Exit Waiting List");
                    joinButton.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    joinButton.setEnabled(true);
                });
    }

    /**
     * Handle joining the waiting list
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

        String entryId = UUID.randomUUID().toString();
        WaitingListEntry entry = new WaitingListEntry(entryId, eventId, userId, new Date());

        entry.setProfile(currentProfile);

        Log.d(TAG, "handleJoinWaitingList: Creating entry with entryId=" + entryId + 
                ", eventId=" + eventId + ", userId=" + userId);

        joinButton.setEnabled(false);
        joinButton.setText("Joining...");

        ((WaitingListRepositoryFs) waitingListRepository)
                .joinWithLimitCheck(entry, new WaitingListRepository.OnWaitingListOperationListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(requireContext(), "Successfully joined waiting list!", Toast.LENGTH_SHORT).show();
                        isInWaitingList = true;

                        loadEventDetails(); // refresh limit + button
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        loadEventDetails(); // refresh button state
                    }
                });

    }

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
                    Toast.makeText(requireContext(), "Successfully left waiting list", Toast.LENGTH_SHORT).show();
                    isInWaitingList = false;
                    loadEventDetails();
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("eventId", eventId);
        notification.put("recipientId", userId);
        notification.put("type", "WAITING_LIST_JOINED");
        notification.put("title", "Joined Waiting List");
        notification.put("message", "You've successfully joined the waiting list for this event. Good luck!");
        notification.put("isRead", false);
        notification.put("createdAt", com.google.firebase.Timestamp.now());
        
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> Log.d(TAG, "✅ Sent waiting list notification"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to send notification", e));
    }

    private void toast(String msg) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
