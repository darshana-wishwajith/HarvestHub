package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class OrderInTransitActivity extends AppCompatActivity {

    private TextView tvOrderId, tvDriverName, tvVehicleInfo;
    private MaterialCardView btnCall;
    private MaterialButton btnTrackOrder;

    private String driverPhoneNumber = "0712345678"; // Default placeholder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_in_transit);

        initViews();
        setupData();
        setupListeners();
        setupNavigation();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        btnCall = findViewById(R.id.btnCall);
        btnTrackOrder = findViewById(R.id.btnTrackOrder);
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
            startActivity(new Intent(this, ActiveOrderTrackerActivity.class));
        });
    }

    private void setupNavigation() {
        // Setup Top Navigation
        View topNav = findViewById(R.id.included_top_nav);
        View btnNotification = topNav.findViewById(R.id.btnNotification);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Setup Bottom Navigation
        View navBar = findViewById(R.id.included_nav);
        BottomNavHelper.setup(this, navBar, BottomNavHelper.Tab.LISTINGS);
    }
}
