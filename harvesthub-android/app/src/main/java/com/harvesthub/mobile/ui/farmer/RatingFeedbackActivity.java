package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class RatingFeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_feedback);

        setupNavigation();

        MaterialButton btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
        btnSubmitFeedback.setOnClickListener(v -> {
            Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.Tab.LISTINGS.name());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }, 1000);
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
