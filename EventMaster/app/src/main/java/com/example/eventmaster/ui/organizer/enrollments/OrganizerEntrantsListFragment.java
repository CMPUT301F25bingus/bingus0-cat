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
import com.example.eventmaster.data.api.RegistrationRepository;
import com.example.eventmaster.data.firestore.RegistrationRepositoryFs;
import com.google.android.material.appbar.MaterialToolbar;

public class OrganizerEntrantsListFragment extends Fragment {
    private static final String ARG_EVENT_ID = "eventId";
    private static final String ARG_STATUS  = "status";
    private static final String ARG_TITLE   = "title";

    private RegistrationRepository repo;
    private EntrantRowAdapter adapter;
    private ProgressBar progress;
    private TextView empty, header;

    public static OrganizerEntrantsListFragment newInstance(String eventId, String status, String title) {
        OrganizerEntrantsListFragment f = new OrganizerEntrantsListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_EVENT_ID, eventId);
        b.putString(ARG_STATUS, status);
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle s) {
        return inf.inflate(R.layout.fragment_org_entrants_list, c, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        MaterialToolbar bar = v.findViewById(R.id.topBar);
        if (bar != null) {
                      // e.g., "Selected entrants" or "Cancelled entrants"
            bar.setNavigationOnClickListener(click ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }


        header = v.findViewById(R.id.headerTitle);
        empty  = v.findViewById(R.id.empty);
        progress = v.findViewById(R.id.progress);

        RecyclerView rv = v.findViewById(R.id.recycler);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EntrantRowAdapter();
        rv.setAdapter(adapter);

        repo = new RegistrationRepositoryFs();

        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";
        header.setText(title);

        load();
    }

    private void load() {
        String eventId = getArguments().getString(ARG_EVENT_ID);
        String status  = getArguments().getString(ARG_STATUS);

        setBusy(true);
        repo.listByStatus(eventId, status)
                .addOnSuccessListener(rows -> {
                    adapter.submit(rows);
                    empty.setVisibility(rows == null || rows.isEmpty() ? View.VISIBLE : View.GONE);
                    setBusy(false);
                })
                .addOnFailureListener(e -> {
                    empty.setText(e.getMessage());
                    empty.setVisibility(View.VISIBLE);
                    setBusy(false);
                });
    }

    private void setBusy(boolean b) {
        progress.setVisibility(b ? View.VISIBLE : View.GONE);
    }
}
