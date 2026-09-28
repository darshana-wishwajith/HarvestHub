package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 6.1: Arrival Inspection
 * Allows buyer to physically inspect crop crates against pre-transit photo baseline,
 * release remaining 80% Escrow balance, or report transit damage.
 */
public class ArrivalInspectionActivity extends AppCompatActivity {

    private TextView tvOrderIdPill;
    private TextView tvRemainingBalance;
    private ImageView ivBaselineCargo;
    private Button btnRejectDamage;
    private Button btnAcceptPayBalance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrival_inspection);

        // Light status bar with dark icons
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        tvOrderIdPill = findViewById(R.id.tvOrderIdPill);
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance);
        ivBaselineCargo = findViewById(R.id.ivBaselineCargo);
        btnRejectDamage = findViewById(R.id.btnRejectDamage);
        btnAcceptPayBalance = findViewById(R.id.btnAcceptPayBalance);
    }

    private void setupClickListeners() {
        // Enlarge or inspect pre-transit photo baseline
        if (ivBaselineCargo != null) {
            ivBaselineCargo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBaselineInspectionDialog();
                }
            });
        }

        // Reject (Transit Damage) button
        if (btnRejectDamage != null) {
            btnRejectDamage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTransitDamageDialog();
                }
            });
        }

        // Accept & Pay Balance button
        if (btnAcceptPayBalance != null) {
            btnAcceptPayBalance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAcceptAndPayDialog();
                }
            });
        }
    }

    private void showBaselineInspectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ImageView fullImageView = new ImageView(this);
        fullImageView.setImageResource(R.drawable.img_cargo_evidence);
        fullImageView.setAdjustViewBounds(true);
        fullImageView.setPadding(24, 24, 24, 24);

        builder.setView(fullImageView)
                .setTitle("Pre-Transit Baseline Inspection")
                .setMessage("GPS: Dambulla Farm | Departure Time: Today, 10:45 AM\nVerify that current tomato quality matches this baseline.")
                .setPositiveButton("Close", null)
                .show();
    }

    private void showTransitDamageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Report Transit Damage?")
                .setMessage("Rejecting this shipment will freeze the remaining 80% balance (Rs. 9,476) in Smart Escrow and prompt you to submit proof photos of the damaged crates.")
                .setNegativeButton("Keep Inspecting", null)
                .setPositiveButton("Report Damage", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ArrivalInspectionActivity.this, "Transit Damage Claim Filed. Dispute Resolution Active.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ArrivalInspectionActivity.this, DisputeResolvedActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void showAcceptAndPayDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Release Final Settlement?")
                .setMessage("Confirm arrival and crop condition? Releasing Rs. 9,476 from Smart Escrow completes Order #HH-842.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm & Pay Balance", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(ArrivalInspectionActivity.this, "Arrival Confirmed! Proceeding to Delivery Handshake.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ArrivalInspectionActivity.this, DeliveryHandshakeActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }
}

