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
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NavigationTrackingActivity extends AppCompatActivity {

    private LinearLayout btnContinueNavigation;
    private LinearLayout btnReportTollDelay;
    private LinearLayout btnDispatchDesk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_navigation_tracking);

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
                SessionManager session = new SessionManager(this);
                TransporterEventRequest event = new TransporterEventRequest(session.getUser().getId(),
                        "ARRIVED_DESTINATION", "Transporter arrived at destination");
                ApiClient.getClient(session).create(ApiService.class)
                        .applyTransporterEvent(getIntent().getStringExtra("ORDER_ID"), event)
                        .enqueue(new Callback<Order>() {
                            @Override public void onResponse(Call<Order> call, Response<Order> response) {
                                if (!response.isSuccessful()) return;
                                Intent intent = new Intent(NavigationTrackingActivity.this, DestinationArrivedActivity.class);
                                intent.putExtra("ORDER_ID", getIntent().getStringExtra("ORDER_ID"));
                                startActivity(intent);
                            }
                            @Override public void onFailure(Call<Order> call, Throwable throwable) {
                                Toast.makeText(NavigationTrackingActivity.this, "Unable to record destination arrival.", Toast.LENGTH_LONG).show();
                            }
                        });
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

