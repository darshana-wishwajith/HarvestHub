package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class WaitingForAdvancePaymentActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_for_advance_payment);

        setupNavigation();
        setupGestureDetector();

        getWindow().getDecorView().setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
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

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                startActivity(new Intent(WaitingForAdvancePaymentActivity.this, StartHarvestingActivity.class));
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
