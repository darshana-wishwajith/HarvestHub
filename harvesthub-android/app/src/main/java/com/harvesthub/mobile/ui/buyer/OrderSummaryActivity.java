package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class OrderSummaryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvOrderStatus;
    private TextView tvOrderRefBadge;
    private TextView tvHarvestCropName;
    private TextView tvHarvestFarmerName;
    private TextView tvHarvestPriceTotal;
    private TextView tvHarvestPricePerKg;
    private TextView tvVehicleTier;
    private TextView tvDistance;
    private TextView tvCalculatedTransitCost;
    private TextView tvPartialCropCost;
    private TextView tvTransportFee;
    private TextView tvConvenienceFee;
    private TextView tvTotalAdvancePayment;
    private MaterialButton btnSecurePayment;

    private boolean isPaymentSecured = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_summary);

        initViews();
        loadIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvOrderStatus = findViewById(R.id.tvOrderStatus);
        tvOrderRefBadge = findViewById(R.id.tvOrderRefBadge);
        tvHarvestCropName = findViewById(R.id.tvHarvestCropName);
        tvHarvestFarmerName = findViewById(R.id.tvHarvestFarmerName);
        tvHarvestPriceTotal = findViewById(R.id.tvHarvestPriceTotal);
        tvHarvestPricePerKg = findViewById(R.id.tvHarvestPricePerKg);
        tvVehicleTier = findViewById(R.id.tvVehicleTier);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalculatedTransitCost = findViewById(R.id.tvCalculatedTransitCost);
        tvPartialCropCost = findViewById(R.id.tvPartialCropCost);
        tvTransportFee = findViewById(R.id.tvTransportFee);
        tvConvenienceFee = findViewById(R.id.tvConvenienceFee);
        tvTotalAdvancePayment = findViewById(R.id.tvTotalAdvancePayment);
        btnSecurePayment = findViewById(R.id.btnSecurePayment);
    }

    private void loadIntentData() {
        String cropName = getIntent().getStringExtra("crop_name");
        String farmerName = getIntent().getStringExtra("farmer_name");
        String orderRef = getIntent().getStringExtra("order_ref");
        String cropCost = getIntent().getStringExtra("crop_cost");
        String transitCost = getIntent().getStringExtra("transit_cost");
        String totalAdvance = getIntent().getStringExtra("total_advance");

        if (cropName != null && !cropName.isEmpty()) {
            tvHarvestCropName.setText(cropName);
        }
        if (farmerName != null && !farmerName.isEmpty()) {
            tvHarvestFarmerName.setText(farmerName.startsWith("Farmer:") ? farmerName : "Farmer: " + farmerName);
        }
        if (orderRef != null && !orderRef.isEmpty()) {
            tvOrderRefBadge.setText("Ref " + orderRef);
        }
        if (cropCost != null && !cropCost.isEmpty()) {
            tvHarvestPriceTotal.setText(cropCost);
            tvPartialCropCost.setText(cropCost);
        }
        if (transitCost != null && !transitCost.isEmpty()) {
            tvCalculatedTransitCost.setText(transitCost);
            tvTransportFee.setText(transitCost);
        }
        if (totalAdvance != null && !totalAdvance.isEmpty()) {
            tvTotalAdvancePayment.setText("Total Advance Payment " + totalAdvance);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnSecurePayment.setOnClickListener(v -> {
            Intent intentCheckout = new Intent(this, SecureCheckoutActivity.class);
            String orderId = getIntent().getStringExtra("order_id");
            if (orderId == null || orderId.isEmpty()) orderId = "order_001";
            intentCheckout.putExtra("order_id", orderId);
            String advanceStr = tvTotalAdvancePayment.getText().toString().replace("Total Advance Payment ", "").trim();
            if (advanceStr.isEmpty()) advanceStr = "Rs. 11,845";
            intentCheckout.putExtra("total_amount", advanceStr);
            intentCheckout.putExtra("contract_ref", tvOrderRefBadge.getText().toString().replace("Ref ", ""));
            intentCheckout.putExtra("crop_cost", "Crop: " + tvPartialCropCost.getText().toString());
            intentCheckout.putExtra("logistics_cost", "Logistics: " + tvTransportFee.getText().toString());
            intentCheckout.putExtra("escrow_fee", "Escrow Fee: " + tvConvenienceFee.getText().toString());
            intentCheckout.putExtra("batch_description", tvHarvestCropName.getText().toString() + " • " + tvHarvestFarmerName.getText().toString());
            startActivity(intentCheckout);
        });
    }
}

