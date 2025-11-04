package com.example.eventmaster.ui.organizer.enrollments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventmaster.R;

public class OrganizerEntrantsHubFragment  extends Fragment {
    private static final String ARG_EVENT_ID = "eventId";

    public static OrganizerEntrantsHubFragment newInstance(String eventId) {
        OrganizerEntrantsHubFragment f = new OrganizerEntrantsHubFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_org_entrants_hub, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        String eventId = getArguments() != null ? getArguments().getString(ARG_EVENT_ID) : null;
        if (eventId == null) { requireActivity().onBackPressed(); return; }

        v.findViewById(R.id.btnFinal).setOnClickListener(x ->
                openList(eventId, "ACTIVE", getString(R.string.selected_entrants)));

        v.findViewById(R.id.btnCancelled).setOnClickListener(x ->
                openList(eventId, "CANCELLED", getString(R.string.cancelled_entrants)));

        // Waitlist button can be added later (different data source)
    }

    private void openList(String eventId, String status, String title) {
        Fragment f = OrganizerEntrantsListFragment.newInstance(eventId, status, title);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, f)
                .addToBackStack(null)
                .commit();
    }
}
