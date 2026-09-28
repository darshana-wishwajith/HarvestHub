package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartHarvestingActivity extends AppCompatActivity {

    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_harvesting);

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null) orderId = "demo-order";

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);

        setupNavigation();

        Button btnStartHarvesting = findViewById(R.id.btnStartHarvesting);

        btnStartHarvesting.setOnClickListener(v -> {
            Map<String, String> statusMap = new HashMap<>();
            statusMap.put("status", "HARVEST_READY");
            
            apiService.updateOrderStatus(orderId, statusMap).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(StartHarvestingActivity.this, "Harvesting started.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(StartHarvestingActivity.this, HarvestReadyActivity.class);
                        intent.putExtra("ORDER_ID", orderId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(StartHarvestingActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<GenericResponse> call, Throwable t) {
                    Toast.makeText(StartHarvestingActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
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
