package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WaitingForAdvancePaymentActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;
    private Handler statusHandler;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_for_advance_payment);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) orderId = "demo-order";

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);

        setupNavigation();
        setupGestureDetector();
        startPolling();

        getWindow().getDecorView().setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
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
                            if ("HARVEST_LOCKED".equals(response.body().getStatus())) {
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
        Intent intent = new Intent(WaitingForAdvancePaymentActivity.this, StartHarvestingActivity.class);
        intent.putExtra("ORDER_ID", orderId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusHandler != null) statusHandler.removeCallbacksAndMessages(null);
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

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                navigateNext();
                return true;
            }

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }
        });
    }
}
