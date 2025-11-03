package com.example.eventmaster.ui.profile;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.ProfileLocalStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class EditProfileActivity extends AppCompatActivity {

    private ProfileLocalStore store;
    private EditText etName, etEmail, etPhone;
    private MaterialButton btnUpdate;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        store = new ProfileLocalStore(this);

        MaterialToolbar tb = findViewById(R.id.toolbarEdit);
        tb.setNavigationOnClickListener(v -> finish());

        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnUpdate = findViewById(R.id.btnUpdate);

        // Pre-fill if exists
        if (store.hasProfile()) {
            if (store.name()  != null) etName.setText(store.name());
            if (store.email() != null) etEmail.setText(store.email());
            if (store.phone() != null) etPhone.setText(store.phone());
            btnUpdate.setText("Update");
        } else {
            btnUpdate.setText("Create");
        }

        btnUpdate.setOnClickListener(v -> doSave());
    }

    private void doSave() {
        String name  = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and email are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        store.save(name, email, phone);
        Toast.makeText(this, store.hasProfile() ? "Profile updated" : "Profile created", Toast.LENGTH_SHORT).show();
        finish();
    }
}


