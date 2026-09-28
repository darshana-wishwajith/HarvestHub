package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 6.3: Dispute Resolved
 * Displays transaction rollback confirmation, 100% Escrow refund to buyer's wallet,
 * transporter return-to-origin protocol, and platform zero-fee guarantee.
 */
public class DisputeResolvedActivity extends AppCompatActivity {

    private TextView tvRefundAmount;
    private TextView tvRefundId;
    private Button btnReturnDashboard;
    private TextView tvContactSupport;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dispute_resolved);

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
        tvRefundAmount = findViewById(R.id.tvRefundAmount);
        tvRefundId = findViewById(R.id.tvRefundId);
        btnReturnDashboard = findViewById(R.id.btnReturnDashboard);
        tvContactSupport = findViewById(R.id.tvContactSupport);
    }

    private void setupClickListeners() {
        // Return to Dashboard button
        if (btnReturnDashboard != null) {
            btnReturnDashboard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(DisputeResolvedActivity.this, VerifiedHarvestsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        }

        // Contact Support Agent link
        if (tvContactSupport != null) {
            tvContactSupport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSupportDialog();
                }
            });
        }
    }

    private void showSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Escrow Support Desk")
                .setMessage("Dispute Ref: REF-9482\nOrder: #HH-842\nStatus: 100% Refund Credited\n\nNeed assistance with your wallet balance or return journey?")
                .setPositiveButton("Call Support Hotline", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:+94112345678"));
                            startActivity(callIntent);
                        } catch (Exception e) {
                            Toast.makeText(DisputeResolvedActivity.this, "Support Hotline: +94 11 234 5678", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
