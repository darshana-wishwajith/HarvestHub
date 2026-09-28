package com.example.project_part_2;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Delivery Disputed Activity - Transporter Transit Incident Screen
 */
public class DeliveryDisputedActivity extends AppCompatActivity {

    private LinearLayout btnReverseLogistics;
    private LinearLayout btnFileAppeal;
    private TextView btnTapToEnlarge;
    private CardView cardPhoto1;
    private CardView cardPhoto2;
    private CardView cardPhoto3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_disputed);

        // Turn screen on and keep awake for testing on phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Light status bar with slate background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }

        initViews();
        setupListeners();

        final ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    if (getIntent().getBooleanExtra("scroll_to_bottom", false)) {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    } else {
                        scrollView.scrollTo(0, 0);
                    }
                }
            });
        }
    }

    private void initViews() {
        btnReverseLogistics = findViewById(R.id.btnReverseLogistics);
        btnFileAppeal = findViewById(R.id.btnFileAppeal);
        btnTapToEnlarge = findViewById(R.id.btnTapToEnlarge);
        cardPhoto1 = findViewById(R.id.cardPhoto1);
        cardPhoto2 = findViewById(R.id.cardPhoto2);
        cardPhoto3 = findViewById(R.id.cardPhoto3);
    }

    private void setupListeners() {
        // Reverse Logistics Routing Button
        if (btnReverseLogistics != null) {
            btnReverseLogistics.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(DeliveryDisputedActivity.this)
                            .setTitle("Initiate Reverse Logistics")
                            .setMessage("Assigning transit routing to nearest salvage liquidation center:\n\n• Destination: Bio-Compost Hub, Peliyagoda\n• Rejected Load: 12 Crates (600 kg)\n\nProceed with dispatch navigation?")
                            .setPositiveButton("Accept Route", (dialog, which) -> {
                                Toast.makeText(DeliveryDisputedActivity.this, "Reverse Logistics Route Assigned", Toast.LENGTH_LONG).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        // File Appeal Button
        if (btnFileAppeal != null) {
            btnFileAppeal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(DeliveryDisputedActivity.this)
                            .setTitle("File Dispute Appeal")
                            .setMessage("You have 23h 48m remaining to submit evidence against Cargo Damage Claim #842.\n\nRequired:\n1. Pre-transit inspection signoff\n2. Reefer temperature logs\n3. Counter-photos")
                            .setPositiveButton("Open Appeal Form", (dialog, which) -> {
                                Toast.makeText(DeliveryDisputedActivity.this, "Opening Dispute Appeal Portal...", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Later", null)
                            .show();
                }
            });
        }

        // Photo Clicks & Enlarge
        View.OnClickListener photoClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String photoLabel = "Terminal Evidence Photo";
                if (v.getId() == R.id.cardPhoto1) {
                    photoLabel = "Evidence Crate #04: Lower tier crush damage";
                } else if (v.getId() == R.id.cardPhoto2) {
                    photoLabel = "Evidence Crate #09: Side impact collapse";
                } else if (v.getId() == R.id.cardPhoto3) {
                    photoLabel = "Evidence Barcode QA: Rejected consignment sheet";
                }
                Toast.makeText(DeliveryDisputedActivity.this, photoLabel, Toast.LENGTH_SHORT).show();
            }
        };

        if (cardPhoto1 != null) cardPhoto1.setOnClickListener(photoClickListener);
        if (cardPhoto2 != null) cardPhoto2.setOnClickListener(photoClickListener);
        if (cardPhoto3 != null) cardPhoto3.setOnClickListener(photoClickListener);

        if (btnTapToEnlarge != null) {
            btnTapToEnlarge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(DeliveryDisputedActivity.this, "Opening full-resolution damage gallery (3 photos)", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
