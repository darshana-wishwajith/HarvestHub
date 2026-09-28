package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 6.2: Delivery Handshake
 * Synchronized 2-way verification to finalize order, release Farmer Escrow
 * and Driver Fee from the automated settlement ledger.
 */
public class DeliveryHandshakeActivity extends AppCompatActivity {

    private TextView tvOrderIdPill;
    private TextView tvDriverName;
    private TextView tvDriverVehicle;
    private TextView tvCropDetails;
    private TextView tvFarmerEscrow;
    private TextView tvDriverFee;
    private TextView tvTotalRelease;
    private Button btnConfirmGoodsReceived;
    private TextView tvReportIssue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_handshake);

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
        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverVehicle = findViewById(R.id.tvDriverVehicle);
        tvCropDetails = findViewById(R.id.tvCropDetails);
        tvFarmerEscrow = findViewById(R.id.tvFarmerEscrow);
        tvDriverFee = findViewById(R.id.tvDriverFee);
        tvTotalRelease = findViewById(R.id.tvTotalRelease);
        btnConfirmGoodsReceived = findViewById(R.id.btnConfirmGoodsReceived);
        tvReportIssue = findViewById(R.id.tvReportIssue);
    }

    private void setupClickListeners() {
        // Confirm Goods Received button
        if (btnConfirmGoodsReceived != null) {
            btnConfirmGoodsReceived.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showConfirmHandoverDialog();
                }
            });
        }

        // Report Issue with Handover
        if (tvReportIssue != null) {
            tvReportIssue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showReportDisputeDialog();
                }
            });
        }
    }

    private void showConfirmHandoverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Order Handover?")
                .setMessage("Disbursement Ledger will execute automatically:\n\n" +
                        "• Farmer Escrow: Rs. 8,200\n" +
                        "• Driver Fee: Rs. 1,276\n\n" +
                        "Total Release: Rs. 9,476\n\n" +
                        "This will finalize Order #HH-842 and cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Confirm & Disburse", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(DeliveryHandshakeActivity.this, "Order #HH-842 Completed! Funds Disbursed to Farmer & Transporter.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(DeliveryHandshakeActivity.this, RateExperienceActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .show();
    }

    private void showReportDisputeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Report Handover Issue")
                .setMessage("Describe any cargo discrepancy (crate count mismatch, offload damage, or delivery dispute). Escrow disbursement will be paused immediately.")
                .setNegativeButton("Back", null)
                .setPositiveButton("Open Dispute", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(DeliveryHandshakeActivity.this, "Dispute Filed. Escrow payout held for mediation.", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }
}
