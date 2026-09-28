package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Page 5.2: Live Transit Tracking
 * Shows real-time GPS transit map with driver Sunil Perera,
 * delivery ETA, route progress, and direct access to Page 5.1 Loading Evidence.
 */
public class TransitTrackingActivity extends AppCompatActivity {

    private TextView tvOrderNumber;
    private TextView tvOrderDetails;
    private TextView tvArrivingTime;
    private TextView tvDistanceRemaining;
    private ProgressBar progressBarTransit;
    private TextView tvProgressPercent;
    private TextView tvOrigin;
    private TextView tvDestination;
    private TextView tvDriverName;
    private TextView tvDriverVehicle;
    private ImageButton btnCallDriver;
    private Button btnViewLoadingEvidence;
    private ImageView ivTransitMap;
    private String orderId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transit_tracking);

        // Light status bar with dark icons
        if (getWindow() != null) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        initializeViews();
        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null || orderId.isEmpty()) {
            orderId = getIntent().getStringExtra("order_id");
        }
        if (orderId == null || orderId.isEmpty()) {
            orderId = "ORD-892";
        }
        loadOrder();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrder();
    }

    private void loadOrder() {
        if (orderId == null || orderId.isEmpty()) return;
        ApiService apiService = ApiClient.getClient(new SessionManager(this)).create(ApiService.class);
        apiService.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                Order order = response.body();
                tvOrderNumber.setText("Order #" + order.getId());
                String crop = order.getCropName() != null ? order.getCropName() : (order.getCropType() != null ? order.getCropType() : "Fresh Tomatoes");
                tvOrderDetails.setText(crop + " (" + (int)order.getQuantity() + " KG)");
                if (order.getTransporterName() != null && !order.getTransporterName().isEmpty()) {
                    tvDriverName.setText(order.getTransporterName());
                }
                if (order.getVehicleTier() != null && !order.getVehicleTier().isEmpty()) {
                    tvDriverVehicle.setText(order.getVehicleTier());
                }
                if ("DELIVERED".equalsIgnoreCase(order.getStatus()) || "DELIVERED_PENDING_HANDSHAKE".equalsIgnoreCase(order.getStatus())) {
                    Intent intent = new Intent(TransitTrackingActivity.this, ArrivalInspectionActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable throwable) { }
        });
    }

    private void initializeViews() {
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvOrderDetails = findViewById(R.id.tvOrderDetails);
        tvArrivingTime = findViewById(R.id.tvArrivingTime);
        tvDistanceRemaining = findViewById(R.id.tvDistanceRemaining);
        progressBarTransit = findViewById(R.id.progressBarTransit);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvDestination = findViewById(R.id.tvDestination);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverVehicle = findViewById(R.id.tvDriverVehicle);
        btnCallDriver = findViewById(R.id.btnCallDriver);
        btnViewLoadingEvidence = findViewById(R.id.btnViewLoadingEvidence);
        ivTransitMap = findViewById(R.id.ivTransitMap);
    }

    private void setupListeners() {
        // Driver phone call action
        if (btnCallDriver != null) {
            btnCallDriver.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCallDriverDialog();
                }
            });
        }

        // View Loading Evidence button -> Opens Page 5.1 (VerifyCargoActivity)
        if (btnViewLoadingEvidence != null) {
            btnViewLoadingEvidence.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(TransitTrackingActivity.this, VerifyCargoActivity.class);
                    intent.putExtra("ORDER_ID", orderId);
                    startActivity(intent);
                }
            });
        }

        // Tap on map to show live telemetry
        if (ivTransitMap != null) {
            ivTransitMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(TransitTrackingActivity.this, "Live GPS Signal Active â€¢ Speed: 42 km/h â€¢ On Route", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showCallDriverDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Call Driver")
                .setMessage("Connect directly with driver Sunil Perera?\n\nVehicle: 1â€“Ton Lorry (WP-LO492)\nPhone: +94 77 458 9201")
                .setPositiveButton("Call Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent callIntent = new Intent(Intent.ACTION_DIAL);
                            callIntent.setData(Uri.parse("tel:+94774589201"));
                            startActivity(callIntent);
                        } catch (Exception e) {
                            Toast.makeText(TransitTrackingActivity.this, "Calling Sunil Perera (+94 77 458 9201)", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

