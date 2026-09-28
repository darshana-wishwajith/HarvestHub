package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.content.Intent;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.TransporterEventRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiabilityCheckpointActivity extends AppCompatActivity {

    private FrameLayout btnBack;
    private ImageView ivBuyerSpinner;
    private TextView btnAddSecondary;
    private LinearLayout btnStartJourney;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_liability_checkpoint);

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
            btnAddSecondary.setOnClickListener(v -> submitEvidence());
        }

        if (btnStartJourney != null) {
            btnStartJourney.setOnClickListener(v -> startJourney());
        }
    }

    private void submitEvidence() {
        SessionManager session = new SessionManager(this);
        TransporterEventRequest event = new TransporterEventRequest(session.getUser().getId(),
                "LOADING_EVIDENCE_SUBMITTED", "Pre-loading cargo evidence submitted");
        event.setPhotoCount(1);
        event.setBuyerVerified(true);
        applyEvent(event, false);
    }

    private void startJourney() {
        SessionManager session = new SessionManager(this);
        TransporterEventRequest event = new TransporterEventRequest(session.getUser().getId(),
                "START_JOURNEY", "Buyer loading clearance received; journey started");
        applyEvent(event, true);
    }

    private void applyEvent(TransporterEventRequest event, boolean navigate) {
        String orderId = getIntent().getStringExtra("ORDER_ID");
        SessionManager session = new SessionManager(this);
        ApiClient.getClient(session).create(ApiService.class).applyTransporterEvent(orderId, event)
                .enqueue(new Callback<Order>() {
                    @Override public void onResponse(Call<Order> call, Response<Order> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(LiabilityCheckpointActivity.this, "Transport event was rejected.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (navigate) {
                            Intent intent = new Intent(LiabilityCheckpointActivity.this, NavigationTrackingActivity.class);
                            intent.putExtra("ORDER_ID", orderId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(LiabilityCheckpointActivity.this, "Loading evidence recorded.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Order> call, Throwable throwable) {
                        Toast.makeText(LiabilityCheckpointActivity.this, "Network error recording evidence.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}

