package com.example.eventmaster.ui.admin.profiles;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

/**
 * Admin screen listing organizers (live Firestore stream).
 * Taps open AdminProfileDetailActivity.
 */
public class BrowseOrganizersActivity extends AppCompatActivity {

    private AdminProfileAdapter adapter;
    private ProfileRepositoryFs repo;
    private ListenerRegistration reg;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_organizers);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        repo = new ProfileRepositoryFs();

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileAdapter(new ArrayList<>(), p ->
                startActivity(new Intent(this, AdminProfileDetailActivity.class)
                        .putExtra("profileId", p.getId()))
        );
        recycler.setAdapter(adapter);
    }

    @Override protected void onStart() {
        super.onStart();
        reg = repo.listenOrganizers(
                list -> adapter.replace(list),
                err  -> { /* TODO: toast/log if desired */ });
    }

    @Override protected void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }
}
