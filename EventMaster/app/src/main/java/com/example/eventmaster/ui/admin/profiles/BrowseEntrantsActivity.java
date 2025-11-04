// app/src/main/java/com/example/eventmaster/ui/admin/profiles/BrowseEntrantsActivity.java
package com.example.eventmaster.ui.admin.profiles;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

public class BrowseEntrantsActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private ListenerRegistration reg;
    private EntrantAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_entrants);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvProfiles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EntrantAdapter(new ArrayList<>(),
                p -> { /* optional: open detail */ },
                (pos, p) -> {
                    // delete from Firestore; realtime listener updates the list
                    if (p.getId() != null) {
                        repo.delete(p.getId(), v -> {}, e -> {});
                    }
                });
        rv.setAdapter(adapter);

        // If your layout still has a "View more" text, hide it for the Firestore version.
        View vm = findViewById(R.id.tvViewMore);
        if (vm != null) vm.setVisibility(View.GONE);
    }

    @Override protected void onStart() {
        super.onStart();
        reg = repo.listenEntrants(
                list -> adapter.replace(list),   // <-- now exists on the adapter
                err  -> { /* log/toast if you want */ });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }
}
