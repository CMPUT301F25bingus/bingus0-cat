package com.example.eventmaster.ui.organizer.enrollments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;

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
