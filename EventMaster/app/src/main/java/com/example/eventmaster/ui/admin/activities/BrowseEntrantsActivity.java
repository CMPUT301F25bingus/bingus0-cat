package com.example.eventmaster.ui.admin.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.ui.admin.adapters.EntrantAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * Admin screen to browse Entrant profiles.
 *
 * Responsibilities:
 *  - Subscribes to Firestore entrants stream and renders a list.
 *  - Allows admin to remove an entrant (hard delete) with confirmation.
 *
 * Notes:
 *  - Relies on ProfileRepositoryFs.listenEntrants().
 *  - Deleting will be reflected by the snapshot listener (no manual remove needed).
 */
public class BrowseEntrantsActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private ListenerRegistration reg;
    private EntrantAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_activity_browse_entrants);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvProfiles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EntrantAdapter(
                new ArrayList<>(),
                p -> { /* optional: open detail */ },
                (pos, p) -> {
                    if (p.getId() == null) return;

                    new MaterialAlertDialogBuilder(BrowseEntrantsActivity.this)
                            .setTitle("Remove entrant?")
                            .setMessage("Are you sure you want to permanently remove this entrant?")
                            // If you have a warning icon, uncomment the next line and replace with your drawable:
                            // .setIcon(R.drawable.ic_warning)
                            .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                            .setPositiveButton("Remove", (d, w) -> {
                                repo.delete(
                                        p.getId(),
                                        v -> Snackbar.make(findViewById(android.R.id.content),
                                                "Entrant removed", Snackbar.LENGTH_SHORT).show(),
                                        e -> Snackbar.make(findViewById(android.R.id.content),
                                                "Failed to remove: " + e.getMessage(), Snackbar.LENGTH_LONG).show()
                                );
                            })
                            .show();
                }
        );

        rv.setAdapter(adapter);

        View vm = findViewById(R.id.tvViewMore);
        if (vm != null) vm.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        reg = repo.listenEntrants(
                list -> adapter.replace(list),
                err -> {
                    // Optionally show/log error
                    Snackbar.make(findViewById(android.R.id.content),
                            "Failed to load entrants.", Snackbar.LENGTH_LONG).show();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }
}

