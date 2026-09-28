package com.example.project_part_2;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ArrivingPickupActivity extends AppCompatActivity {

    private CardView cardTurnInstruction;
    private CardView cardTelemetry;
    private FrameLayout btnRecenter;
    private FrameLayout btnSpeaker;
    private FrameLayout btnLayers;
    private FrameLayout btnSos;
    private LinearLayout btnArrivedLocation;
    private LinearLayout btnCallFarmer;
    private LinearLayout btnAccessIssue;

    private boolean isMuted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arriving_pickup);

        // Light status bar icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(0xFFFFFFFF);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardTurnInstruction = findViewById(R.id.cardTurnInstruction);
        cardTelemetry = findViewById(R.id.cardTelemetry);
        btnRecenter = findViewById(R.id.btnRecenter);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnLayers = findViewById(R.id.btnLayers);
        btnSos = findViewById(R.id.btnSos);
        btnArrivedLocation = findViewById(R.id.btnArrivedLocation);
        btnCallFarmer = findViewById(R.id.btnCallFarmer);
        btnAccessIssue = findViewById(R.id.btnAccessIssue);
    }

    private void setupListeners() {
        if (cardTurnInstruction != null) {
            cardTurnInstruction.setOnClickListener(v ->
                Toast.makeText(this, "In 150m: Turn right onto Mandya Agro Corridor", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnRecenter != null) {
            btnRecenter.setOnClickListener(v ->
                Toast.makeText(this, "GPS Re-centered to current vehicle location", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnSpeaker != null) {
            btnSpeaker.setOnClickListener(v -> {
                isMuted = !isMuted;
                Toast.makeText(this, isMuted ? "Voice Guidance Muted" : "Voice Guidance Active", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnLayers != null) {
            btnLayers.setOnClickListener(v ->
                Toast.makeText(this, "Map Layer toggled: Satellite / Agro-Corridors", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnSos != null) {
            btnSos.setOnClickListener(v ->
                Toast.makeText(this, "EMERGENCY SOS: Dispatch Desk & Escrow Alerted!", Toast.LENGTH_LONG).show()
            );
        }

        if (btnArrivedLocation != null) {
            btnArrivedLocation.setOnClickListener(v -> {
                Toast.makeText(this, "Arrived at Location: GreenValley Farms Gate 2 Verified", Toast.LENGTH_SHORT).show();
                // Optionally proceed to Liability Checkpoint
                Intent intent = new Intent(this, LiabilityCheckpointActivity.class);
                startActivity(intent);
            });
        }

        if (btnCallFarmer != null) {
            btnCallFarmer.setOnClickListener(v ->
                Toast.makeText(this, "Calling Farmer Ramesh Gowda (+94 77 123 4567)...", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnAccessIssue != null) {
            btnAccessIssue.setOnClickListener(v ->
                Toast.makeText(this, "Access Issue Reported: Dispatch alerted for Gate 2 clearance", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
