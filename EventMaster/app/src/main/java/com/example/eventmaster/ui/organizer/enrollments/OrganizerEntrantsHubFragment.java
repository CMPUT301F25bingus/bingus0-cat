package com.example.eventmaster.ui.organizer.enrollments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;
import com.example.eventmaster.ui.organizer.SelectedEntrantsActivity;
import com.example.eventmaster.ui.organizer.waitinglist.WaitingListActivity;
import com.example.eventmaster.ui.organizer.chosenlist.ChosenListActivity;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Fragment that serves as the hub for viewing entrants of a specific event.
 * Provides two navigation options for the organizer:
 *  Selected Entrants — Entrants who were successfully enrolled in the event (US 02.06.03: View final enrolled list)
 *  Cancelled Entrants — Entrants whose registrations were cancelled -(US 02.06.02: View cancelled entrants)
 * Each option opens a {@link OrganizerEntrantsListFragment} displaying entrants
 * filtered by their registration status.
 *
 * */

public class OrganizerEntrantsHubFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";

    /**
     * Factory method for creating a new instance of {@link OrganizerEntrantsHubFragment}.
     *
     * @param eventId The ID of the event whose entrants are being viewed.
     * @return A configured instance of this fragment with the provided arguments.
     */
    public static OrganizerEntrantsHubFragment newInstance(String eventId) {
        OrganizerEntrantsHubFragment f = new OrganizerEntrantsHubFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        f.setArguments(b);
        return f;
    }

    private String eventId;


    /**
     * Inflates the layout and initializes buttons for viewing selected and cancelled entrants.
     *
     * @param inflater  LayoutInflater used to inflate the view.
     * @param container Parent container view.
     * @param savedInstanceState Saved instance state (if any).
     * @return The inflated fragment view.
     */

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_org_entrants_hub, container, false);
        eventId = requireArguments().getString(ARG_EVENT_ID);

        View btnWaitingList = v.findViewById(R.id.btnWaitingList);
        View btnChosenList  = v.findViewById(R.id.btnChosenList);
        View btnSelected    = v.findViewById(R.id.btnFinal);
        View btnCancelled   = v.findViewById(R.id.btnCancelled);

        // Navigate to Waiting List Activity
        btnWaitingList.setOnClickListener(x -> {
            Intent intent = new Intent(requireContext(), WaitingListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Navigate to Chosen List Activity
        btnChosenList.setOnClickListener(x -> {
            Intent intent = new Intent(requireContext(), ChosenListActivity.class);
            intent.putExtra("eventId", eventId);
            startActivity(intent);
        });

        // Navigate to Selected Entrants (Final List) using real Firestore data
        btnSelected.setOnClickListener(x ->
                openList("ACTIVE", getString(R.string.selected_entrants)));

        btnCancelled.setOnClickListener(x ->
                openList("CANCELLED_ANY", getString(R.string.cancelled_entrants)));

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        MaterialToolbar topBar = v.findViewById(R.id.topBar);
        if (topBar != null) {
            // Tap on toolbar back → pop just this fragment
            topBar.setNavigationOnClickListener(
                    click -> getParentFragmentManager().popBackStack()
            );
        }

        // Also handle system back gestures while this fragment is visible
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override public void handleOnBackPressed() {
                        getParentFragmentManager().popBackStack();
                    }
                }
        );
    }

    /**
     * Opens a list fragment filtered by entrant status.
     *
     * @param status Registration status to filter by ("ACTIVE" or "CANCELLED_ANY").
     * @param title  Title for the next screen.
     */
    private void openList(String status, String title) {
        OrganizerEntrantsListFragment frag =
                OrganizerEntrantsListFragment.newInstance(eventId, status, title);

        int containerId = resolveContainerId();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(containerId, frag)
                .addToBackStack(null)
                .commit();
    }


    /**
     * Resolves the container view ID where the next fragment will be placed.
     *
     * @return The resource ID of the container frame layout.
     */
    private int resolveContainerId() {
        return R.id.fragment_container;
    }
}
