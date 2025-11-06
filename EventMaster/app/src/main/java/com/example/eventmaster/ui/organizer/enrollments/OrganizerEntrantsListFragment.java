package com.example.eventmaster.ui.organizer.enrollments;

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

        View v = inflater.inflate(R.layout.fragment_org_entrants_list, container, false);

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

    private void onError(@NonNull Throwable t) {
        showLoading(false);
        showEmpty(t.getMessage() != null ? t.getMessage() : "Failed to load entrants");
    }

    private void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        recycler.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        empty.setVisibility(View.GONE);
    }

    private void showEmpty(@NonNull String msg) {
        empty.setText(msg);
        empty.setVisibility(View.VISIBLE);
    }
}
