package com.example.eventmaster.ui.profile;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.ProfileLocalStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private ProfileLocalStore store;
    private TextView tvName, tvEmail, tvPhone;
    private MaterialButton btnEdit, btnDelete;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        store = new ProfileLocalStore(this);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        tvName  = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        btnEdit   = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        btnEdit.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete profile?")
                    .setMessage("This will remove your saved name, email, and phone.")
                    .setPositiveButton("Delete", (d, which) -> {
                        store.clear();
                        Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        bind();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        bind();
    }

    @Override protected void onResume() {
        super.onResume();
        bind();
    }

    private void bind() {
        boolean has = store.hasProfile();

        // Text fields
        tvName.setText("Name: "  + (has ? store.name()  : "—"));
        tvEmail.setText("Email: " + (has ? store.email() : "—"));
        tvPhone.setText("Phone: " + (has ? (store.phone() == null ? "—" : store.phone()) : "—"));

        btnEdit.setText(has ? "Update Profile" : "Create Profile");

        btnDelete.setVisibility(has ? View.VISIBLE : View.GONE);
    }
}

