package com.example.eventmaster.ui.admin.profiles;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;
import com.google.android.material.appbar.MaterialToolbar;

public class AdminProfileDetailActivity extends AppCompatActivity {

    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private String profileId;
    private TextView tvName, tvEmail, tvPhone, tvState;
    private Button btnBan;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_admin_profile_detail);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        profileId = getIntent().getStringExtra("profileId");

        tvName  = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvState = findViewById(R.id.tvState);
        btnBan  = findViewById(R.id.btnBan);

        btnBan.setOnClickListener(v -> toggleBan());
    }

    @Override protected void onResume(){ super.onResume(); load(); }

    private void load(){
        repo.get(profileId, this::bind, e -> tvName.setText("Error loading profile"));
    }

    private void bind(Profile p){
        tvName.setText(ns(p.getName()));
        tvEmail.setText(ns(p.getEmail()));
        tvPhone.setText(ns(p.getPhone()));
        boolean banned = p.getBanned();
        tvState.setText(banned ? "BANNED" : "Active");
        btnBan.setText(banned ? "Unban organizer" : "Ban organizer");
    }

    private void toggleBan(){
        repo.get(profileId, p -> {
            boolean next = !p.getBanned();
            new AlertDialog.Builder(this)
                    .setTitle(next ? "Ban organizer?" : "Unban organizer?")
                    .setMessage((next ? "Ban " : "Unban ") + ns(p.getName()) + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(next ? "Ban" : "Unban",
                            (d, w) -> repo.setBanned(profileId, next, v -> load(), err -> {}))
                    .show();
        }, e -> {});
    }

    private String ns(String s){ return (s == null || s.trim().isEmpty()) ? "—" : s; }
}
