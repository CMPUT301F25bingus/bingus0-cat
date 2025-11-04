package com.example.eventmaster.ui.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.example.eventmaster.data.firestore.ProfileRepositoryFs;
import com.example.eventmaster.model.Profile;

public class EditProfileActivity extends AppCompatActivity {
    private final ProfileRepositoryFs repo = new ProfileRepositoryFs();
    private String profileId;

    private EditText etName, etEmail, etPhone;
    private Button btnSave;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileId = getIntent().getStringExtra("profileId");
        etName  = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);

        repo.get(profileId, p -> {
            etName.setText(p.getName());
            etEmail.setText(p.getEmail());
            etPhone.setText(p.getPhone());
        }, e -> {});

        btnSave.setOnClickListener(v -> {
            Profile p = new Profile(profileId,
                    etName.getText().toString().trim(),
                    etEmail.getText().toString().trim(),
                    etPhone.getText().toString().trim());
            repo.upsert(profileId, p, x -> finish(), err -> {});
        });
    }
}
