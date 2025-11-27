package com.example.eventmaster.ui.organizer.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eventmaster.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEntrantMapActivity extends AppCompatActivity {

    private MapView mapView;
    private String eventId;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid config
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.organizer_activity_entrant_map);

        mapView = findViewById(R.id.mapView);
        eventId = getIntent().getStringExtra("eventId");
        TextView emptyState = findViewById(R.id.empty_state_message);


        // Basic map setup
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        ImageView back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());

        loadEntrantLocations();
    }

    private void loadEntrantLocations() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("waiting_list")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<GeoPoint> allPoints = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snaps) {
                        Double lat = doc.getDouble("lat");
                        Double lng = doc.getDouble("lng");

                        if (lat == null || lng == null) continue;

                        GeoPoint point = new GeoPoint(lat, lng);
                        allPoints.add(point);

                        Marker m = new Marker(mapView);
                        m.setPosition(point);
                        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        m.setTitle("User: " + doc.getString("userId"));
                        mapView.getOverlays().add(m);
                    }

                    if (allPoints.isEmpty()) {
                        // Show empty state
                        mapView.setVisibility(View.GONE);
                        findViewById(R.id.empty_state_message).setVisibility(View.VISIBLE);
                        return;
                    } else {
                        // Show map normally
                        mapView.setVisibility(View.VISIBLE);
                        findViewById(R.id.empty_state_message).setVisibility(View.GONE);
                    }

                    // Zoom to first location
                    mapView.getController().setZoom(12.0);
                    mapView.getController().setCenter(allPoints.get(0));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed loading locations: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}

