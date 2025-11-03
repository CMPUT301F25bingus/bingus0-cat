package com.example.eventmaster.ui.admin.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eventmaster.R;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class BrowseOrganizersActivity extends AppCompatActivity {

    private AdminProfileAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_browse_organizers);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvProfiles);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileAdapter(seedOrganizers(), p -> {
            Intent i = new Intent(this, AdminProfileDetailActivity.class);
            i.putExtra("name",  p.getName());
            i.putExtra("email", p.getEmail());
            i.putExtra("phone", p.getPhone());
            startActivity(i);
        });
        rv.setAdapter(adapter);

        TextView tvViewMore = findViewById(R.id.tvViewMore);
        tvViewMore.setOnClickListener(v -> adapter.addAll(seedMore()));
    }

    private List<Profile> seedOrganizers() {
        List<Profile> list = new ArrayList<>();
        list.add(new Profile(null,"Bingus10","bingus10@example.com","+1 (785) 534-1229","organizer"));
        list.add(new Profile(null,"Bingus11","bingus11@example.com","+1 (780) 633-4567","organizer"));
        list.add(new Profile(null,"Bingus12","bingus12@example.com","+1 (785) 534-1229","organizer"));
        list.add(new Profile(null,"Bingus13","bingus13@example.com","+1 (780) 633-4567","organizer"));
        list.add(new Profile(null,"Bingus14","bingus14@example.com","+1 (785) 534-1229","organizer"));
        return list;
    }

    private List<Profile> seedMore() {
        List<Profile> list = new ArrayList<>();
        list.add(new Profile(null,"Organizer A","orga@example.com","+1 (555) 000-0001","organizer"));
        list.add(new Profile(null,"Organizer B","orgb@example.com","+1 (555) 000-0002","organizer"));
        return list;
    }
}
