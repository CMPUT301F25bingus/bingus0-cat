package com.example.eventmaster.ui.organizer.enrollments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;

public class OrganizerEntrantsHubFragment extends Fragment {

    public static final String ARG_EVENT_ID = "eventId";

    public static OrganizerEntrantsHubFragment newInstance(String eventId) {
        OrganizerEntrantsHubFragment f = new OrganizerEntrantsHubFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        f.setArguments(b);
        return f;
    }

    private String eventId;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_org_entrants_hub, container, false);
        eventId = requireArguments().getString(ARG_EVENT_ID);

        View btnSelected  = v.findViewById(R.id.btnFinal);
        View btnCancelled = v.findViewById(R.id.btnCancelled);

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

    private int resolveContainerId() {
        return R.id.fragment_container;
    }
}
