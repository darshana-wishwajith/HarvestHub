package com.example.project_part_2;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NavigationTrackingActivity extends AppCompatActivity {

    private LinearLayout btnContinueNavigation;
    private LinearLayout btnReportTollDelay;
    private LinearLayout btnDispatchDesk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_tracking);

        setupStatusBar();
        initViews();
        setupListeners();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0x00000000); // Transparent over map

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void initViews() {
        btnContinueNavigation = findViewById(R.id.btnContinueNavigation);
        btnReportTollDelay = findViewById(R.id.btnReportTollDelay);
        btnDispatchDesk = findViewById(R.id.btnDispatchDesk);
    }

    private void setupListeners() {
        if (btnContinueNavigation != null) {
            btnContinueNavigation.setOnClickListener(v -> {
                Toast.makeText(this, "Navigation Active: Heading towards Colombo Central Terminal", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnReportTollDelay != null) {
            btnReportTollDelay.setOnClickListener(v -> {
                Toast.makeText(this, "Report Toll / Delay: Incident report opened", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnDispatchDesk != null) {
            btnDispatchDesk.setOnClickListener(v -> {
                Toast.makeText(this, "Dispatch Desk: Connecting to Logistics Support...", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
