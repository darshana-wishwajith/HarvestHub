package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.utils.BottomNavHelper;

import android.os.Handler;
import android.os.Looper;

import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;
import com.harvesthub.mobile.model.Order;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WaitForBuyerApprovalActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;
    private Handler handler;
    private Runnable pollRunnable;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_buyer_approval);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) orderId = "demo-order";

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);

        setupNavigation();
        setupGestureDetector();
        startPolling();

        // Use the window's decor view to capture all touches before they reach children
        getWindow().getDecorView().setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Let the event propagate to children
        });

        TextView tvProTip = findViewById(R.id.tvProTipContent);
        String proTipText = getString(R.string.pro_tip_content);
        SpannableString spannableString = new SpannableString(proTipText);
        
        int start = proTipText.indexOf("Do not harvest");
        if (start != -1) {
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, start + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvProTip.setText(spannableString);

        MaterialButton btnMessage = findViewById(R.id.btnMessage);
        MaterialButton btnViewPost = findViewById(R.id.btnViewPost);

        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
        });

        btnViewPost.setOnClickListener(v -> {
            Toast.makeText(this, "Opening original listing...", Toast.LENGTH_SHORT).show();
        });
    }

    private void startPolling() {
        handler = new Handler(Looper.getMainLooper());
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                apiService.getOrder(orderId).enqueue(new Callback<Order>() {
                    @Override
                    public void onResponse(Call<Order> call, Response<Order> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if ("ESCROW_SETUP".equals(response.body().getStatus())) {
                                Intent intent = new Intent(WaitForBuyerApprovalActivity.this, WaitingForAdvancePaymentActivity.class);
                                intent.putExtra("ORDER_ID", orderId);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Order> call, Throwable t) { }
                });
                handler.postDelayed(this, 10000);
            }
        };
        handler.postDelayed(pollRunnable, 10000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && pollRunnable != null) {
            handler.removeCallbacks(pollRunnable);
        }
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
                Intent intent = new Intent(WaitForBuyerApprovalActivity.this, WaitingForAdvancePaymentActivity.class);
                intent.putExtra("ORDER_ID", orderId);
                startActivity(intent);
                finish();
                return true;
            }

            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }
        });
    }
}
