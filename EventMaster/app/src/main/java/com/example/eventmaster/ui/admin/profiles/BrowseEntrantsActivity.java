package com.example.eventmaster.ui.admin.profiles;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class BrowseEntrantsActivity extends AppCompatActivity {

    private EntrantAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_entrants);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvProfiles);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EntrantAdapter(seedEntrants(),
                p -> {/* no-op for now */},
                (pos, p) -> confirmRemove(pos, p));
        rv.setAdapter(adapter);

        TextView tvViewMore = findViewById(R.id.tvViewMore);
        tvViewMore.setOnClickListener(v -> adapter.addAll(seedMore()));
    }

    private void confirmRemove(int position, Profile p) {
        new AlertDialog.Builder(this)
                .setTitle("Remove entrant?")
                .setMessage("Are you sure you want to remove " + (p.getName() == null ? "this entrant" : p.getName()) + "?")
                .setPositiveButton("Remove", (DialogInterface dialog, int which) -> adapter.removeAt(position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Dummy data for now  */
    private List<Profile> seedEntrants() {
        List<Profile> list = new ArrayList<>();
        list.add(new Profile(null,"Bingus3","bingus3@example.com","+1 (780) 533-4567","entrant"));
        list.add(new Profile(null,"Bingus4","bingus4@example.com","+1 (780) 533-4567","entrant"));
        list.add(new Profile(null,"Bingus5","bingus5@example.com","+1 (785) 534-1229","entrant"));
        list.add(new Profile(null,"Bingus6","bingus6@example.com","+1 (780) 533-4567","entrant"));
        list.add(new Profile(null,"Bingus7","bingus7@example.com","+1 (780) 533-4567","entrant"));
        return list;
    }

    private List<Profile> seedMore() {
        List<Profile> list = new ArrayList<>();
        list.add(new Profile(null,"Entrant A","entrant.a@example.com","+1 (555) 000-0101","entrant"));
        list.add(new Profile(null,"Entrant B","entrant.b@example.com","+1 (555) 000-0102","entrant"));
        return list;
    }
}
