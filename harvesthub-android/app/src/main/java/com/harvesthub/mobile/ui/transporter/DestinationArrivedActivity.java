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
import androidx.core.content.ContextCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DestinationArrivedActivity extends AppCompatActivity {

    private LinearLayout btnDeliveryCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_destination_arrived);

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
                SessionManager session = new SessionManager(this);
                String orderId = getIntent().getStringExtra("ORDER_ID");
                TransporterEventRequest event = new TransporterEventRequest(session.getUser().getId(),
                        "DELIVERY_COMPLETED", "Transporter confirmed delivery completion");
                ApiClient.getClient(session).create(ApiService.class).applyTransporterEvent(orderId, event)
                        .enqueue(new Callback<Order>() {
                            @Override public void onResponse(Call<Order> call, Response<Order> response) {
                                if (!response.isSuccessful()) return;
                                Toast.makeText(DestinationArrivedActivity.this,
                                        "Delivery completed and recorded.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(DestinationArrivedActivity.this, DeliveryDashboardActivity.class));
                                finish();
                            }
                            @Override public void onFailure(Call<Order> call, Throwable throwable) {
                                Toast.makeText(DestinationArrivedActivity.this,
                                        "Unable to record delivery completion.", Toast.LENGTH_LONG).show();
                            }
                        });
            });
        }
    }
}

