package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;

public class OrderInTransitActivity extends AppCompatActivity {

    private TextView tvOrderId, tvDriverName, tvVehicleInfo;
    private MaterialCardView btnCall;
    private MaterialButton btnTrackOrder;
    private LinearLayout llNavHome, llNavMarket, llNavListings, llNavWallet, llNavSettings;

    private String driverPhoneNumber = "0712345678"; // Default placeholder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_in_transit);

        initViews();
        setupData();
        setupListeners();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        btnCall = findViewById(R.id.btnCall);
        btnTrackOrder = findViewById(R.id.btnTrackOrder);
        
        llNavHome = findViewById(R.id.llNavHome);
        llNavMarket = findViewById(R.id.llNavMarket);
        llNavListings = findViewById(R.id.llNavListings);
        llNavWallet = findViewById(R.id.llNavWallet);
        llNavSettings = findViewById(R.id.llNavSettings);
    }

    private void setupData() {
        String orderId = getIntent().getStringExtra("ORDER_ID");
        String driverName = getIntent().getStringExtra("DRIVER_NAME");
        String vehicleInfo = getIntent().getStringExtra("VEHICLE_INFO");
        String phone = getIntent().getStringExtra("DRIVER_PHONE");

        if (orderId != null) tvOrderId.setText("Order #" + orderId);
        if (driverName != null) tvDriverName.setText("Driver: " + driverName);
        if (vehicleInfo != null) tvVehicleInfo.setText("Vehicle: " + vehicleInfo);
        if (phone != null) driverPhoneNumber = phone;
    }

    private void setupListeners() {
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + driverPhoneNumber));
            startActivity(intent);
        });

        btnTrackOrder.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Live Tracking Map...", Toast.LENGTH_SHORT).show();
        });

        llNavHome.setOnClickListener(v -> {
            // startActivity(new Intent(this, HomeActivity.class));
            Toast.makeText(this, "Home Clicked", Toast.LENGTH_SHORT).show();
        });

        llNavMarket.setOnClickListener(v -> {
             Toast.makeText(this, "Market Clicked", Toast.LENGTH_SHORT).show();
        });

        llNavListings.setOnClickListener(v -> {
             // Already on Listings
        });

        llNavWallet.setOnClickListener(v -> {
             Toast.makeText(this, "Wallet Clicked", Toast.LENGTH_SHORT).show();
        });

        llNavSettings.setOnClickListener(v -> {
             Toast.makeText(this, "Settings Clicked", Toast.LENGTH_SHORT).show();
        });
    }
}
