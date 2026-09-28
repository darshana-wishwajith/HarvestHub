package com.example.project_part_2;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LiabilityCheckpointActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private ImageView ivBuyerSpinner;
    private TextView btnAddSecondary;
    private LinearLayout btnStartJourney;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liability_checkpoint);

        // Dark status bar icons on white background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(0xFFFFFFFF);
        }

        initViews();
        setupSpinnerAnimation();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        ivBuyerSpinner = findViewById(R.id.ivBuyerSpinner);
        btnAddSecondary = findViewById(R.id.btnAddSecondary);
        btnStartJourney = findViewById(R.id.btnStartJourney);
    }

    private void setupSpinnerAnimation() {
        if (ivBuyerSpinner != null) {
            RotateAnimation rotate = new RotateAnimation(
                    0f, 360f,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(1200);
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setInterpolator(new LinearInterpolator());
            ivBuyerSpinner.startAnimation(rotate);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnAddSecondary != null) {
            btnAddSecondary.setOnClickListener(v -> 
                Toast.makeText(this, "Camera launched: Capture secondary tailgate photo", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnStartJourney != null) {
            btnStartJourney.setOnClickListener(v -> 
                Toast.makeText(this, "Journey locked: Awaiting buyer verification clearance (~90s)", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
