package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class FarmerDashboardActivity extends AppCompatActivity {

    private TextView tvGreetingTitle;
    private TextView tvTotalRevenue;
    private TextView tvActiveListings;
    private TextView tvCompletedOrders;
    private TextView tvTrustScore;
    private Button btnViewMarketInsights;
    private Button btnCreateListing;
    private LinearLayout bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        initViews();
        setupClickListeners();

        // Mock data loading
        loadDashboardData();
    }

    private void initViews() {
        tvGreetingTitle = findViewById(R.id.tvGreetingTitle);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvActiveListings = findViewById(R.id.tvActiveListings);
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvTrustScore = findViewById(R.id.tvTrustScore);

        btnViewMarketInsights = findViewById(R.id.btnViewMarketInsights);
        btnCreateListing = findViewById(R.id.btnCreateListing);
        bottomNav = findViewById(R.id.bottomNav);
    }

    private void setupClickListeners() {
        btnCreateListing.setOnClickListener(v -> {
            Toast.makeText(this, "Create New Listing clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Create Listing Activity
        });

        btnViewMarketInsights.setOnClickListener(v -> {
            Toast.makeText(this, "View Market Insights clicked", Toast.LENGTH_SHORT).show();
            // TODO: Navigate to Market Insights Activity
        });

        // Set listeners for mocked Bottom Navigation items
        if (bottomNav != null) {
            for (int i = 0; i < bottomNav.getChildCount(); i++) {
                final int index = i;
                bottomNav.getChildAt(i).setOnClickListener(v -> {
                    String tabName = ((TextView) v).getText().toString();
                    Toast.makeText(this, "Navigating to: " + tabName, Toast.LENGTH_SHORT).show();
                    // TODO: Handle navigation based on tabName
                });
            }
        }
    }

    private void loadDashboardData() {
        // Placeholder for Retrofit API call
        /*
         * ApiService apiService =
         * RetrofitClient.getInstance().create(ApiService.class);
         * apiService.getDashboardData(userId).enqueue(new Callback<DashboardResponse>()
         * {
         *
         * @Override
         * public void onResponse(Call<DashboardResponse> call,
         * Response<DashboardResponse> response) {
         * if (response.isSuccessful() && response.body() != null) {
         * updateUI(response.body());
         * }
         * }
         *
         * @Override
         * public void onFailure(Call<DashboardResponse> call, Throwable t) {
         * Toast.makeText(FarmerDashboardActivity.this, "Failed to load dashboard data",
         * Toast.LENGTH_SHORT).show();
         * }
         * });
         */

        // For now, populate with mock static data
        tvGreetingTitle.setText("Good morning, Kamal!");
        tvTotalRevenue.setText("LKR 125,000");
        tvActiveListings.setText("02");
        tvCompletedOrders.setText("05");
        tvTrustScore.setText("94%");
    }
}
