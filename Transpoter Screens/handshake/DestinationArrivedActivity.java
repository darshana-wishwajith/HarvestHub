package com.example.project_part_2;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class DestinationArrivedActivity extends AppCompatActivity {

    private LinearLayout btnDeliveryCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_destination_arrived);

        setupStatusBar();
        initViews();
        setupListeners();
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
        btnDeliveryCompleted = findViewById(R.id.btnDeliveryCompleted);
    }

    private void setupListeners() {
        if (btnDeliveryCompleted != null) {
            btnDeliveryCompleted.setOnClickListener(v -> {
                Toast.makeText(this, "Delivery Completed: Escrow Release Clearance Confirmed!", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
