package com.example.eventmaster.ui.entrant.invitations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.InvitationService;
import com.example.eventmaster.data.firestore.InvitationServiceFs;
import com.example.eventmaster.model.Invitation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvitationInboxFragment extends Fragment {
    private static final boolean DEBUG_SEEDER = true; // set false to hide the + button

    private static final String ARG_ENTRANT_ID = "entrantId";
    private static final String DEFAULT_ENTRANT_ID = "demo-user";

    private InvitationService invitationService;
    private InvitationListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_invitation_inbox, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        RecyclerView recycler = v.findViewById(R.id.invitationRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new InvitationListAdapter(this::onAccept, this::onDecline);
        recycler.setAdapter(adapter);

        invitationService = new InvitationServiceFs(FirebaseFirestore.getInstance());

        refresh();

//        // Optional debug seeder
//        FloatingActionButton fab = v.findViewById(R.id.fabSeed);
//        if (fab != null) fab.setOnClickListener(btn -> seedPendingInvitation());
//        View fab = v.findViewById(R.id.fabSeed);
//        if (fab != null) fab.setVisibility(DEBUG_SEEDER ? View.VISIBLE : View.GONE);
//        if (DEBUG_SEEDER && fab != null) {
//            fab.setOnClickListener(btn -> seedPendingInvitation());
//        }
    }

    // --- UI Actions ---

    private void onAccept(Invitation inv) {
        setUiEnabled(false);
        invitationService.respond(inv.getEventId(), inv.getId(), true, inv.getEntrantId())
                .addOnSuccessListener(x -> { snackbar("Accepted"); refresh(); })
                .addOnFailureListener(e -> { setUiEnabled(true); snackbar(e.getMessage()); });
    }

    private void onDecline(Invitation inv) {
        setUiEnabled(false);
        invitationService.respond(inv.getEventId(), inv.getId(), false, inv.getEntrantId())
                .addOnSuccessListener(x -> { snackbar("Declined"); refresh(); })
                .addOnFailureListener(e -> { setUiEnabled(true); snackbar(e.getMessage()); });
    }

    // --- Data loading ---

    private void refresh() {
        invitationService.listByEntrant(currentEntrantId())
                .addOnSuccessListener(list -> { adapter.submitList(list); snackbar("Loaded "+list.size()); })
                //.addOnSuccessListener(this::display)
                .addOnFailureListener(e -> { setUiEnabled(true); snackbar(e.getMessage()); });
        //.addOnSuccessListener(list -> { adapter.submitList(list); setUiEnabled(true); })
        //.addOnFailureListener(e -> { setUiEnabled(true); snackbar(e.getMessage()); });
    }

    // --- Helpers ---

    /** Determine entrant id without FirebaseAuth. */
    private String currentEntrantId() {
        Bundle args = getArguments();
        if (args != null) {
            String fromArgs = args.getString(ARG_ENTRANT_ID);
            if (fromArgs != null && !fromArgs.isEmpty()) return fromArgs;
        }
        return DEFAULT_ENTRANT_ID;
    }

    private void setUiEnabled(boolean enabled) {
        View root = getView();
        if (root != null) root.setEnabled(enabled);
    }

    private void snackbar(String msg) {
        View root = getView();
        if (root != null) Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }

    /** Debug helper: seed minimal event + one PENDING invitation for this entrant. */
//    private void seedPendingInvitation() {
//        String uid = currentEntrantId();
//        String eventId = "event-123";
//        String invitationId = "inv-" + System.currentTimeMillis();
//
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        DocumentReference eventRef = db.collection("events").document(eventId);
//        Map<String, Object> eventStub = new HashMap<>();
//        eventStub.put("title", "Debug Event");
//        eventStub.put("capacity", 5);
//        eventStub.put("createdAtUtc", System.currentTimeMillis());
//
//        long now = System.currentTimeMillis();
//        Map<String, Object> inv = new HashMap<>();
//        inv.put("eventId", eventId);
//        inv.put("entrantId", uid);
//        inv.put("status", "PENDING");
//        inv.put("issuedAtUtc", now);
//        inv.put("expiresAtUtc", now + 86_400_000L);
//
//        setUiEnabled(false);
//        eventRef.set(eventStub, SetOptions.merge())
//                .continueWithTask(t -> eventRef.collection("invitations").document(invitationId).set(inv))
//                .addOnSuccessListener(x -> { snackbar("Seeded invitation"); refresh(); })
//                .addOnFailureListener(e -> { setUiEnabled(true); snackbar(e.getMessage()); });
//    }

    // --- Static factory if you want to pass entrantId from activity ---
    public static InvitationInboxFragment newInstance(String entrantId) {
        InvitationInboxFragment f = new InvitationInboxFragment();
        Bundle b = new Bundle();
        b.putString(ARG_ENTRANT_ID, entrantId);
        f.setArguments(b);
        return f;
    }

    private void display(List<Invitation> pending) {
        adapter.submitList(pending);
        View root = getView();
        if (root != null) {
            View empty = root.findViewById(R.id.emptyText);
            boolean isEmpty = pending == null || pending.isEmpty();
            if (empty != null) empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        setUiEnabled(true);
    }

}