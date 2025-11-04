// app/src/main/java/com/example/eventmaster/ui/profile/ProfileActivity.java
package com.example.eventmaster.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.api.EventReadService;
import com.example.eventmaster.data.api.RegistrationService;
import com.example.eventmaster.data.firestore.EventReadServiceFs;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.data.firestore.RegistrationServiceFs;
import com.example.eventmaster.model.Event;
import com.example.eventmaster.model.Registration;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entrant Profile screen:
 * - Shows name/email/phone (+ banned chip when applicable)
 * - Edit/Delete actions
 * - Event history list (reads registrations by entrant, then fetches event titles)
 *
 * Layout must provide:
 *  - TextViews: tvName, tvEmail, tvPhone, tvBanned
 *  - Buttons:   btnEdit, btnDelete
 *  - RecyclerView (optional): rvHistory
 */
public class ProfileActivity extends AppCompatActivity {

    // Data / services
    private final ProfileRepositoryFs profileRepo = new ProfileRepositoryFs();
    private final RegistrationService regSvc =
            new RegistrationServiceFs(FirebaseFirestore.getInstance());
    private final EventReadService eventRead = new EventReadServiceFs();

    // Replace with FirebaseAuth uid when available
    private String currentId = "demoUser123";

    // UI
    private TextView tvName, tvEmail, tvPhone, tvBanned;
    private Button btnEdit, btnDelete;

    // History list (optional in layout)
    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Prefer real uid if Auth is set up
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Summary fields
        tvName   = findViewById(R.id.tvName);
        tvEmail  = findViewById(R.id.tvEmail);
        tvPhone  = findViewById(R.id.tvPhone);
        tvBanned = findViewById(R.id.tvBanned);
        btnEdit  = findViewById(R.id.btnEdit);
        btnDelete= findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)
                        .putExtra("profileId", currentId)));

        btnDelete.setOnClickListener(v -> confirmDelete());

        // History list (present only if your layout includes it)
        rvHistory = findViewById(R.id.rvHistory);
        if (rvHistory != null) {
            rvHistory.setLayoutManager(new LinearLayoutManager(this));
            historyAdapter = new HistoryAdapter();
            rvHistory.setAdapter(historyAdapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
        loadHistory();
    }

    // -------- Profile load / delete --------

    private void loadProfile() {
        profileRepo.get(currentId, p -> {
            tvName.setText(ns(p.getName()));
            tvEmail.setText(ns(p.getEmail()));
            tvPhone.setText(ns(p.getPhone()));
            boolean banned = p.getBanned();
            tvBanned.setText(banned ? "BANNED" : "");
            tvBanned.setVisibility(banned ? TextView.VISIBLE : TextView.GONE);
        }, e -> tvName.setText("Error: " + e.getMessage()));
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete profile?")
                .setMessage("This removes your profile from the system.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (d, which) ->
                        profileRepo.delete(currentId, v -> finish(), err -> {}))
                .show();
    }

    // -------- History --------

    private void loadHistory() {
        if (rvHistory == null || historyAdapter == null) return;

        historyAdapter.replace(new ArrayList<>()); // clear while loading

        regSvc.listByEntrant(currentId)
                .addOnSuccessListener(regs -> {
                    // ✅ never reassign a captured var; create a final alias
                    final List<Registration> regsFinal =
                            (regs == null) ? new ArrayList<>() : new ArrayList<>(regs);

                    android.util.Log.d("ProfileHistory", "Got regs count=" + regsFinal.size());

                    // Collect unique eventIds (final refs only)
                    final Map<String, Event> cache = new HashMap<>();
                    final List<String> eventIds = new ArrayList<>();
                    for (final Registration r : regsFinal) {
                        final String eid = r.getEventId();
                        if (eid != null && !cache.containsKey(eid)) {
                            cache.put(eid, null);
                            eventIds.add(eid);
                        }
                    }

                    android.util.Log.d("ProfileHistory", "Need to fetch events: " + eventIds);

                    if (eventIds.isEmpty()) {
                        // show rows even if no events fetched
                        final List<HistoryAdapter.Row> rows = new ArrayList<>();
                        for (final Registration r : regsFinal) {
                            rows.add(new HistoryAdapter.Row(r, null));
                        }
                        historyAdapter.replace(rows);
                        return;
                    }

                    // ✅ use AtomicInteger (or int[]), not a plain int
                    final AtomicInteger done = new AtomicInteger(0);
                    final int total = eventIds.size();

                    for (final String eventId : eventIds) {
                        eventRead.get(eventId)
                                .addOnSuccessListener(e -> {
                                    cache.put(eventId, e);
                                    if (done.incrementAndGet() == total) bindHistory(regsFinal, cache);
                                })
                                .addOnFailureListener(err -> {
                                    android.util.Log.e("ProfileHistory", "Event fetch failed: " + eventId, err);
                                    if (done.incrementAndGet() == total) bindHistory(regsFinal, cache);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ProfileHistory", "listByEntrant failed", e);
                    android.widget.Toast.makeText(this,
                            "History load failed: " + e.getMessage(),
                            android.widget.Toast.LENGTH_LONG).show();
                });
    }


    private void bindHistory(List<Registration> regs, Map<String, Event> cache) {
        List<HistoryAdapter.Row> rows = new ArrayList<>();
        for (Registration r : regs) {
            Event e = cache.get(r.getEventId());
            String title = (e == null) ? null : e.getTitle();
            rows.add(new HistoryAdapter.Row(r, title));
        }
        // Newest first
        rows.sort((a, b) -> Long.compare(b.reg.getCreatedAtUtc(), a.reg.getCreatedAtUtc()));
        historyAdapter.replace(rows);
    }

    // -------- utils --------
    private String ns(String s) {
        return (s == null || s.trim().isEmpty()) ? "—" : s;
    }
}
