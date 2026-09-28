package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class CargoRejectedActivity extends AppCompatActivity {

    private TextView tvViewEvidence;
    private LinearLayout btnFarmerSupport;
    private LinearLayout btnPhotoEvidence;
    private LinearLayout btnReturnDispatchQueue;
    private androidx.core.widget.NestedScrollView nestedScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_cargo_rejected);

        setupStatusBar();
        initViews();
        setupListeners();

        if (nestedScrollView != null) {
            nestedScrollView.post(() -> nestedScrollView.scrollTo(0, 0));
        }
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void initViews() {
        nestedScrollView = findViewById(R.id.nestedScrollView);
        tvViewEvidence = findViewById(R.id.tvViewEvidence);
        btnFarmerSupport = findViewById(R.id.btnFarmerSupport);
        btnPhotoEvidence = findViewById(R.id.btnPhotoEvidence);
        btnReturnDispatchQueue = findViewById(R.id.btnReturnDispatchQueue);
    }

    private void setupListeners() {
        if (tvViewEvidence != null) {
            tvViewEvidence.setOnClickListener(v -> {
                Toast.makeText(this, "Viewing QA Inspection Evidence (Moisture 24.8% flagged)", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnFarmerSupport != null) {
            btnFarmerSupport.setOnClickListener(v -> {
                Toast.makeText(this, "Calling Farmer Support (Dambulla Farm #12)...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnPhotoEvidence != null) {
            btnPhotoEvidence.setOnClickListener(v -> {
                Toast.makeText(this, "Opening Consignment Photo Evidence Gallery...", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnReturnDispatchQueue != null) {
            btnReturnDispatchQueue.setOnClickListener(v -> {
                Toast.makeText(this, "Returned to Dispatch Queue: Next load prioritized (15 km range)", Toast.LENGTH_LONG).show();
            });
        }
    }
}

