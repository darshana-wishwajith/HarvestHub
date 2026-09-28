package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 5.2: Live Transit Tracking
 * Shows real-time GPS transit map with driver Sunil Perera,
 * delivery ETA, route progress, and direct access to Page 5.1 Loading Evidence.
 */
public class TransitTrackingActivity extends AppCompatActivity {

    private TextView tvOrderNumber;
    private TextView tvOrderDetails;
    private TextView tvArrivingTime;
    private TextView tvDistanceRemaining;
    private ProgressBar progressBarTransit;
    private TextView tvProgressPercent;
    private TextView tvOrigin;
    private TextView tvDestination;
    private TextView tvDriverName;
    private TextView tvDriverVehicle;
    private ImageButton btnCallDriver;
    private Button btnViewLoadingEvidence;
    private ImageView ivTransitMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transit_tracking);

        // Light status bar with dark icons
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvOrderDetails = findViewById(R.id.tvOrderDetails);
        tvArrivingTime = findViewById(R.id.tvArrivingTime);
        tvDistanceRemaining = findViewById(R.id.tvDistanceRemaining);
        progressBarTransit = findViewById(R.id.progressBarTransit);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDestination = findViewById(R.id.tvDestination);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverVehicle = findViewById(R.id.tvDriverVehicle);
        btnCallDriver = findViewById(R.id.btnCallDriver);
        btnViewLoadingEvidence = findViewById(R.id.btnViewLoadingEvidence);
        ivTransitMap = findViewById(R.id.ivTransitMap);
    }

    private void setupListeners() {
        // Driver phone call action
        if (btnCallDriver != null) {
            btnCallDriver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCallDriverDialog();
                }
            });
        }

        // View Loading Evidence button -> Opens Page 5.1 (VerifyCargoActivity)
        if (btnViewLoadingEvidence != null) {
            btnViewLoadingEvidence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TransitTrackingActivity.this, VerifyCargoActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Tap on map to show live telemetry
        if (ivTransitMap != null) {
            ivTransitMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(TransitTrackingActivity.this, "Live GPS Signal Active • Speed: 42 km/h • On Route", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showCallDriverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Call Driver")
                .setMessage("Connect directly with driver Sunil Perera?\n\nVehicle: 1–Ton Lorry (WP-LO492)\nPhone: +94 77 458 9201")
                .setPositiveButton("Call Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:+94774589201"));
                            startActivity(callIntent);
                        } catch (Exception e) {
                            Toast.makeText(TransitTrackingActivity.this, "Calling Sunil Perera (+94 77 458 9201)", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
