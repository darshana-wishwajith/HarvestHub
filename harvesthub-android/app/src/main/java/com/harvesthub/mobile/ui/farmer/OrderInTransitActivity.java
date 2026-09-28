package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderInTransitActivity extends AppCompatActivity {

    private TextView tvOrderId, tvDriverName, tvVehicleInfo;
    private MaterialCardView btnCall;
    private MaterialButton btnTrackOrder;

    private String driverPhoneNumber = "0712345678"; // Default placeholder
    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;
    private Handler statusHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_in_transit);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) orderId = "demo-order";

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);

        initViews();
        setupData();
        setupListeners();
        setupNavigation();
        startPolling();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicleInfo = findViewById(R.id.tvVehicleInfo);
        btnCall = findViewById(R.id.btnCall);
        btnTrackOrder = findViewById(R.id.btnTrackOrder);
    }

    private void setupData() {
        tvOrderId.setText("Order #" + orderId);
        // We'll load the rest from the API
    }

    private void startPolling() {
        statusHandler = new Handler(Looper.getMainLooper());
        Runnable pollRunnable = new Runnable() {
            @Override
            public void run() {
                apiService.getOrder(orderId).enqueue(new Callback<Order>() {
                    @Override
                    public void onResponse(Call<Order> call, Response<Order> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Order order = response.body();
                            // If order driver info is available, display it (assuming driver details are part of Order object)
                            // We don't have driver properties in Order model explicitly in standard Order class yet, but just checking status
                            if ("DELIVERED".equals(order.getStatus())) {
                                navigateNext();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<Order> call, Throwable t) { }
                });
                statusHandler.postDelayed(this, 10000);
            }
        };
        statusHandler.post(pollRunnable);
    }

    private void navigateNext() {
        Intent intent = new Intent(OrderInTransitActivity.this, RatingFeedbackActivity.class);
        intent.putExtra("ORDER_ID", orderId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusHandler != null) statusHandler.removeCallbacksAndMessages(null);
    }

    private void setupListeners() {
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + driverPhoneNumber));
            startActivity(intent);
        });

        btnTrackOrder.setOnClickListener(v -> {
            Toast.makeText(this, "Opening Live Tracking Map...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ActiveOrderTrackerActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
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
