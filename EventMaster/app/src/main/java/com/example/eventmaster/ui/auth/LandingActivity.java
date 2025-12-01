package com.example.eventmaster.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.MainActivity;
import com.example.eventmaster.R;
import com.google.android.material.button.MaterialButton;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        MaterialButton continueBtn = findViewById(R.id.btnContinue);

        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
    }
}
