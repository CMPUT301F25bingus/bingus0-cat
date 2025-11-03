package com.example.eventmaster.ui.admin.profiles;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eventmaster.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class AdminProfileDetailActivity extends AppCompatActivity {

    private String name, email, phone;
    private MaterialButton btnBan;
    private BanStore banStore;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile_detail); // <- ensure this matches your XML file

        banStore = new BanStore(this);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        tb.setNavigationOnClickListener(v -> finish());

        name  = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");
        Log.d("AdminDetail","name="+name+" email="+email+" phone="+phone);

        ((TextView)findViewById(R.id.tvName)).setText("Name: "  + nz(name));
        ((TextView)findViewById(R.id.tvEmail)).setText("Email: " + nz(email));
        ((TextView)findViewById(R.id.tvPhone)).setText("Phone: " + nz(phone));

        btnBan = findViewById(R.id.btnBan); // <- must not be null
        if (btnBan == null) {
            Toast.makeText(this, "btnBan not found in layout", Toast.LENGTH_LONG).show();
            return;
        }

        refreshBanButton();

        btnBan.setOnClickListener(v -> showBanDialog());
    }

    private void showBanDialog() {
        final boolean currentlyBanned = banStore.isBanned(email);
        final String action = currentlyBanned ? "unban" : "ban";
        new AlertDialog.Builder(this)
                .setTitle(cap(action) + " organizer?")
                .setMessage("Are you sure you want to " + action + " " + nz(name) + "?")
                .setPositiveButton(cap(action), (d, w) -> {
                    banStore.setBanned(email, !currentlyBanned);
                    Toast.makeText(this,
                            (currentlyBanned ? "Unbanned " : "Banned ") + nz(name),
                            Toast.LENGTH_SHORT).show();
                    refreshBanButton();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshBanButton() {
        boolean banned = banStore.isBanned(email);
        btnBan.setText(banned ? "Unban organizer" : "Ban organizer");
        btnBan.setBackgroundTintList(ContextCompat.getColorStateList(
                this, banned ? R.color.btn_neutral : R.color.btn_danger));
        btnBan.setRippleColor(ContextCompat.getColorStateList(
                this, banned ? R.color.btn_neutral_pressed : R.color.btn_danger_pressed));
    }

    private String nz(String s) { return s == null ? "—" : s; }
    private String cap(String s) { return s == null || s.isEmpty() ? "" : s.substring(0,1).toUpperCase()+s.substring(1); }
}
