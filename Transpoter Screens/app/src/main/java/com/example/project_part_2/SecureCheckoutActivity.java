package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class SecureCheckoutActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvAdvanceAmount;
    private TextView tvContractPill;
    private TextView pillCropCost;
    private TextView pillLogisticsCost;
    private TextView pillEscrowFee;
    private EditText etCardNumber;
    private EditText etExpiry;
    private EditText etCvv;
    private EditText etCardholderName;
    private ImageView btnCvvHelp;
    private TextView tvBatchDescription;
    private MaterialButton btnPayNow;

    private boolean isPaymentCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secure_checkout);

        initViews();
        loadIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvAdvanceAmount = findViewById(R.id.tvAdvanceAmount);
        tvContractPill = findViewById(R.id.tvContractPill);
        pillCropCost = findViewById(R.id.pillCropCost);
        pillLogisticsCost = findViewById(R.id.pillLogisticsCost);
        pillEscrowFee = findViewById(R.id.pillEscrowFee);
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiry = findViewById(R.id.etExpiry);
        etCvv = findViewById(R.id.etCvv);
        etCardholderName = findViewById(R.id.etCardholderName);
        btnCvvHelp = findViewById(R.id.btnCvvHelp);
        tvBatchDescription = findViewById(R.id.tvBatchDescription);
        btnPayNow = findViewById(R.id.btnPayNow);
    }

    private void loadIntentData() {
        String totalAmount = getIntent().getStringExtra("total_amount");
        String contractRef = getIntent().getStringExtra("contract_ref");
        String cropCost = getIntent().getStringExtra("crop_cost");
        String logisticsCost = getIntent().getStringExtra("logistics_cost");
        String escrowFee = getIntent().getStringExtra("escrow_fee");
        String batchDescription = getIntent().getStringExtra("batch_description");

        if (totalAmount != null && !totalAmount.isEmpty()) {
            tvAdvanceAmount.setText(totalAmount);
            btnPayNow.setText("Pay " + totalAmount);
        }
        if (contractRef != null && !contractRef.isEmpty()) {
            tvContractPill.setText(contractRef.startsWith("Contract") ? contractRef : "Contract " + contractRef);
        }
        if (cropCost != null && !cropCost.isEmpty()) {
            pillCropCost.setText(cropCost.startsWith("Crop:") ? cropCost : "Crop: " + cropCost);
        }
        if (logisticsCost != null && !logisticsCost.isEmpty()) {
            pillLogisticsCost.setText(logisticsCost.startsWith("Logistics:") ? logisticsCost : "Logistics: " + logisticsCost);
        }
        if (escrowFee != null && !escrowFee.isEmpty()) {
            pillEscrowFee.setText(escrowFee.startsWith("Escrow Fee:") ? escrowFee : "Escrow Fee: " + escrowFee);
        }
        if (batchDescription != null && !batchDescription.isEmpty()) {
            tvBatchDescription.setText(batchDescription);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnCvvHelp != null) {
            btnCvvHelp.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("CVV Verification")
                    .setMessage("The 3-digit security code located on the signature strip on the back of your card.")
                    .setPositiveButton("Got it", null)
                    .show()
            );
        }

        btnPayNow.setOnClickListener(v -> {
            if (isPaymentCompleted) return;

            String cardNumber = etCardNumber.getText().toString().trim();
            String expiry = etExpiry.getText().toString().trim();
            String cvv = etCvv.getText().toString().trim();

            if (cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
                Toast.makeText(this, "Please verify payment details", Toast.LENGTH_SHORT).show();
                return;
            }

            btnPayNow.setEnabled(false);
            btnPayNow.setText("Processing Encrypted Payment...");

            // Simulate quick payment processing delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                isPaymentCompleted = true;
                btnPayNow.setText("Payment Completed ✓");
                btnPayNow.setBackgroundColor(0xFF16A34A);

                new AlertDialog.Builder(this)
                        .setTitle("Payment Successful! 🎉")
                        .setMessage(tvAdvanceAmount.getText().toString() + " has been successfully held in HarvestHub Escrow.\n\nAutomated logistics dispatch is now active.")
                        .setPositiveButton("View Transaction Ledger →", (dialog, which) -> {
                            Intent intentLedger = new Intent(this, TransactionLedgerActivity.class);
                            intentLedger.putExtra("secured_amount", tvAdvanceAmount.getText().toString());
                            intentLedger.putExtra("txn_ref", "Txn #TX-8942-AP");
                            intentLedger.putExtra("system_status", "System Status: Payment Processing / Dispatching");
                            startActivity(intentLedger);
                            finish();
                        })
                        .setNegativeButton("Return to Home", (dialog, which) -> {
                            Intent intentHome = new Intent(this, VerifiedHarvestsActivity.class);
                            intentHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intentHome);
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }, 1200);
        });
    }
}
