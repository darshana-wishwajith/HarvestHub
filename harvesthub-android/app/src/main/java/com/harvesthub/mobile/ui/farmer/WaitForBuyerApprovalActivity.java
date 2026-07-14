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

public class WaitForBuyerApprovalActivity extends AppCompatActivity {

    private GestureDetector gestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wait_for_buyer_approval);

        setupNavigation();
        setupGestureDetector();

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
            startActivity(intent);
        });

        btnViewPost.setOnClickListener(v -> {
            Toast.makeText(this, "Opening original listing...", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(WaitForBuyerApprovalActivity.this, WaitingForAdvancePaymentActivity.class));
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
