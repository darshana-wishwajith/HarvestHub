package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Page 5.1: Verify Cargo Evidence
 * Allows buyers to inspect live transporter photos and GPS telemetry
 * before releasing transit escrow or rejecting the load.
 */
public class VerifyCargoActivity extends AppCompatActivity {

    private TextView tvExpiringTimer;
    private TextView tvTelemetryLat;
    private TextView tvTelemetryLong;
    private TextView tvTelemetryAlt;
    private TextView tvEscrowProtectedText;
    private ImageView ivCargoEvidence;
    private Button btnRejectCargo;
    private Button btnApproveLoad;

    private CountDownTimer countDownTimer;
    // 14 minutes and 22 seconds in milliseconds (862,000 ms)
    private static final long INITIAL_TIME_MS = (14 * 60 + 22) * 1000L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_cargo);

        // Make status bar icons dark for light background
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        initializeViews();
        setupCountdownTimer();
        setupClickListeners();
    }

    private void initializeViews() {
        tvExpiringTimer = findViewById(R.id.tvExpiringTimer);
        tvTelemetryLat = findViewById(R.id.tvTelemetryLat);
        tvTelemetryLong = findViewById(R.id.tvTelemetryLong);
        tvTelemetryAlt = findViewById(R.id.tvTelemetryAlt);
        tvEscrowProtectedText = findViewById(R.id.tvEscrowProtectedText);
        ivCargoEvidence = findViewById(R.id.ivCargoEvidence);
        btnRejectCargo = findViewById(R.id.btnRejectCargo);
        btnApproveLoad = findViewById(R.id.btnApproveLoad);
    }

    private void setupCountdownTimer() {
        countDownTimer = new CountDownTimer(INITIAL_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d mins", minutes, seconds);
                if (tvExpiringTimer != null) {
                    tvExpiringTimer.setText(timeFormatted);
                }
            }

            @Override
            public void onFinish() {
                if (tvExpiringTimer != null) {
                    tvExpiringTimer.setText("Expired");
                }
            }
        }.start();
    }

    private void setupClickListeners() {
        // Enlarge or preview image on tap
        if (ivCargoEvidence != null) {
            ivCargoEvidence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showImagePreviewDialog();
                }
            });
        }

        // Reject Cargo button
        if (btnRejectCargo != null) {
            btnRejectCargo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRejectConfirmationDialog();
                }
            });
        }

        // Approve & Load button
        if (btnApproveLoad != null) {
            btnApproveLoad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showApprovalSuccessDialog();
                }
            });
        }
    }

    private void showImagePreviewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView fullImageView = new ImageView(this);
        fullImageView.setImageResource(R.drawable.img_cargo_evidence);
        fullImageView.setAdjustViewBounds(true);
        fullImageView.setPadding(24, 24, 24, 24);

        builder.setView(fullImageView)
                .setTitle("Cargo Loading Evidence Inspection")
                .setMessage("GPS: Dambulla Farm | LAT: 7.8731° N, LONG: 80.6517° E | ALT: 168m\nTimestamp: Today, 10:45 AM")
                .setPositiveButton("Close", null)
                .show();
    }

    private void showRejectConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reject Cargo Load?")
                .setMessage("Rejecting will cancel transporter assignment and trigger an immediate 100% refund of Rs. 11,845 to your escrow balance.")
                .setNegativeButton("Keep Reviewing", null)
                .setPositiveButton("Confirm Rejection", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(VerifyCargoActivity.this, "Cargo Rejected. 100% Escrow Refund Processed.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .show();
    }

    private void showApprovalSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cargo Approved!")
                .setMessage("Physical cargo verified successfully. Transporter has been authorized to depart. Real-time GPS tracking is now active.")
                .setPositiveButton("Proceed to Live Tracking", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(VerifyCargoActivity.this, "Transit Authorized!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(VerifyCargoActivity.this, TransitTrackingActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
