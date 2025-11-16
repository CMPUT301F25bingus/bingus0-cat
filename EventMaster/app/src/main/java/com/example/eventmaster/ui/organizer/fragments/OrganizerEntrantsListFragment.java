package com.example.eventmaster.ui.organizer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Registration;
import com.example.eventmaster.ui.organizer.adapters.*;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows entrants for an event filtered by registration status.
 * Use status = "ACTIVE" for Final list; use "CANCELLED_ANY" for both cancelled statuses.
 */
public class OrganizerEntrantsListFragment extends Fragment {

    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_STATUS   = "status";   // "ACTIVE" or "CANCELLED_ANY"
    private static final String ARG_TITLE    = "title";

    /**
     * Creates a new instance of the list fragment for a given event and status filter.
     * @param eventId ID of the event whose entrants are to be listed.
     * @param status  Registration status filter ("ACTIVE" or "CANCELLED_ANY").
     * @param title   Title to display in the header bar.
     * @return A configured fragment instance.
     */
    public static OrganizerEntrantsListFragment newInstance(@NonNull String eventId,
                                                            @NonNull String status,
                                                            @NonNull String title) {
        OrganizerEntrantsListFragment f = new OrganizerEntrantsListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        b.putString(ARG_STATUS, status);
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    private String eventId;
    private String status;
    private String title;

    private RegistrationServiceFs service;

    // UI
    private TextView header;
    private ProgressBar progress;
    private RecyclerView recycler;
    private TextView empty;

    // Adapter (your EntrantRowAdapter)
    private EntrantRowAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.organizer_fragment_entrants_list, container, false);

        // args
        Bundle args = getArguments();
        if (args != null) {
            eventId = args.getString(ARG_EVENT_ID);
            status  = args.getString(ARG_STATUS);
            title   = args.getString(ARG_TITLE, "");
        }

        // find views
        header   = v.findViewById(R.id.headerTitle);
        progress = v.findViewById(R.id.progress);
        recycler = v.findViewById(R.id.recycler);
        empty    = v.findViewById(R.id.empty);

        header.setText(title != null ? title : "");

        // adapter + recycler
        adapter = new EntrantRowAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        // service
        service = new RegistrationServiceFs();

        load();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // wire the toolbar back
        MaterialToolbar topBar = v.findViewById(R.id.topBar);
        if (topBar != null) {
            // Tap on toolbar back → go back to the hub
            topBar.setNavigationOnClickListener(
                    click -> getParentFragmentManager().popBackStack()
            );
        }

        // Also support hardware back/gestures here
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
     * Loads entrant data from Firestore via {@link RegistrationServiceFs}.
     * Displays loading indicator during data retrieval and handles both
     * success and error cases.
     */

    private void load() {
        if (eventId == null || status == null) {
            showLoading(false);
            showEmpty("Invalid arguments");
            return;
        }

        showLoading(true);

        if ("CANCELLED_ANY".equals(status)) {
            // list cancelled (both statuses)
            service.listCancelled(eventId, this::bindRegs, this::onError);
        } else {
            // list by exact status (e.g., "ACTIVE")
            service.listByStatus(eventId, status, this::bindRegs, this::onError);
        }
    }

    /**
     * Binds the list of {@link Registration} objects into UI rows.
     *
     * @param regs The list of registration entries from Firestore.
     */
    private void bindRegs(@Nullable List<Registration> regs) {
        showLoading(false);

        if (regs == null || regs.isEmpty()) {
            adapter.submit(new ArrayList<>()); // adapter expects List<EntrantRow>
            showEmpty("No entrants found");
            return;
        }

        // Convert Registration -> EntrantRow (placeholder fields; replace with real Profile lookup if you have it)
        List<EntrantRow> rows = new ArrayList<>(regs.size());
        for (Registration r : regs) {
            String entrantId = r.getEntrantId();
            // If you have a Profile, fetch real name/email/phone. For now, show IDs.
            rows.add(new EntrantRow(
                    "Entrant " + entrantId, // name
                    "ID: " + entrantId,     // email line placeholder
                    "—",                     //  placeholder
                    "—"                     // phone placeholder
            ));
        }

        adapter.submit(rows);
        empty.setVisibility(View.GONE);
    }


    /**
     * Displays an error message when data loading fails.
     *
     * @param t The exception or error thrown by the Firestore call.
     */
    private void onError(@NonNull Throwable t) {
        showLoading(false);
        showEmpty(t.getMessage() != null ? t.getMessage() : "Failed to load entrants");
    }

    /** Toggles the loading spinner and main list visibility. */
    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        empty.setVisibility(View.GONE);
    }


    /**
     * Shows an empty message when there are no entrants or an error occurred.
     *
     * @param msg Message text to display.
     */
    private void showEmpty(@NonNull String msg) {
        empty.setText(msg);
        empty.setVisibility(View.VISIBLE);
    }
}
