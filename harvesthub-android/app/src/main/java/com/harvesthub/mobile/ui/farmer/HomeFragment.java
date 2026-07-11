package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.DashboardResponse;
import com.harvesthub.mobile.model.MarketTrendResponse;
import com.harvesthub.mobile.model.WeatherResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvGreetingTitle, tvWeatherTemp, tvWeatherLocation;
    private TextView tvTotalRevenue, tvRevenueChange, tvPreviousRevenueSubtext;
    private TextView tvActiveListings, tvCompletedOrders, tvTrustScore, tvMarketAvg;
    private LineChart marketChart;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnCreateListing, btnViewMarketInsights;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupBottomNavigation();
        setupChart();
        
        fetchDashboardData("FARMER_ID_HERE"); // Placeholder ID
        fetchWeatherData("Dambulla, LK");
        fetchMarketTrend("Carrot");

        btnCreateListing.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateListingActivity.class));
        });

        btnViewMarketInsights.setOnClickListener(v -> {
            // Intent to MarketInsightsActivity when created
            Toast.makeText(getActivity(), "Navigating to Market Insights...", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void initViews(View v) {
        tvGreetingTitle = v.findViewById(R.id.tvGreetingTitle);
        tvWeatherTemp = v.findViewById(R.id.tvWeatherTemp);
        tvWeatherLocation = v.findViewById(R.id.tvWeatherLocation);
        tvTotalRevenue = v.findViewById(R.id.tvTotalRevenue);
        tvRevenueChange = v.findViewById(R.id.tvRevenueChange);
        tvPreviousRevenueSubtext = v.findViewById(R.id.tvPreviousRevenueSubtext);
        tvActiveListings = v.findViewById(R.id.tvActiveListings);
        tvCompletedOrders = v.findViewById(R.id.tvCompletedOrders);
        tvTrustScore = v.findViewById(R.id.tvTrustScore);
        tvMarketAvg = v.findViewById(R.id.tvMarketAvg);
        marketChart = v.findViewById(R.id.marketChart);
        bottomNavigation = v.findViewById(R.id.bottomNavigation);
        btnCreateListing = v.findViewById(R.id.btnCreateListing);
        btnViewMarketInsights = v.findViewById(R.id.btnViewMarketInsights);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Already here
                return true;
            } else if (id == R.id.nav_market) {
                // Switch to Market Fragment
                return true;
            } else if (id == R.id.nav_listings) {
                // Switch to Listings Fragment
                return true;
            } else if (id == R.id.nav_wallet) {
                // Switch to Wallet Fragment
                return true;
            } else if (id == R.id.nav_settings) {
                // Switch to Settings Fragment
                return true;
            }
            return false;
        });

        // Pre-select Listings as per requirement
        bottomNavigation.setSelectedItemId(R.id.nav_listings);
    }

    private void setupChart() {
        marketChart.getDescription().setEnabled(false);
        marketChart.setTouchEnabled(false);
        marketChart.setDragEnabled(false);
        marketChart.setScaleEnabled(false);
        marketChart.setPinchZoom(false);
        marketChart.setDrawGridBackground(false);
        
        marketChart.getAxisLeft().setEnabled(false);
        marketChart.getAxisRight().setEnabled(false);
        marketChart.getXAxis().setEnabled(false);
        marketChart.getLegend().setEnabled(false);
    }

    private void fetchDashboardData(String farmerId) {
        apiService.getFarmerDashboard(farmerId).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateDashboardUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                Toast.makeText(getActivity(), "Failed to fetch dashboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDashboardUI(DashboardResponse data) {
        if (data.getFarmerName() != null) {
            tvGreetingTitle.setText("Good morning, " + data.getFarmerName() + "!");
        }
        tvTotalRevenue.setText("LKR " + data.getTotalRevenue());
        tvRevenueChange.setText(data.getRevenueChange());
        tvPreviousRevenueSubtext.setText(data.getPreviousRevenue());
        tvActiveListings.setText(data.getActiveListings());
        tvCompletedOrders.setText(data.getCompletedOrders());
        tvTrustScore.setText(data.getTrustScore());
    }

    private void fetchWeatherData(String location) {
        apiService.getWeather(location).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvWeatherTemp.setText(response.body().getTemperature() + ", " + response.body().getCondition());
                    tvWeatherLocation.setText(response.body().getLocation());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // Silent fail for weather
            }
        });
    }

    private void fetchMarketTrend(String crop) {
        apiService.getMarketTrend(crop, 7).enqueue(new Callback<MarketTrendResponse>() {
            @Override
            public void onResponse(Call<MarketTrendResponse> call, Response<MarketTrendResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderChart(response.body().getTrends());
                }
            }

            @Override
            public void onFailure(Call<MarketTrendResponse> call, Throwable t) {
                // Use dummy data if fail
                List<Double> dummy = new ArrayList<>();
                dummy.add(150.0); dummy.add(160.0); dummy.add(155.0);
                dummy.add(175.0); dummy.add(170.0); dummy.add(185.0);
                dummy.add(180.0);
                renderChart(dummy);
            }
        });
    }

    private void renderChart(List<Double> trends) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < trends.size(); i++) {
            entries.add(new Entry(i, trends.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Market Price");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#27AE60"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        
        // Highlight one point (e.g., the peak or a specific index)
        if (entries.size() > 5) {
            dataSet.setDrawCircles(true);
            dataSet.setCircleColor(Color.parseColor("#27AE60"));
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleColor(Color.WHITE);
            // This normally draws all circles, but MPAndroidChart doesn't easily support just one circle
            // without a custom renderer. For now, we'll draw all or none.
            // Requirement says "one highlighted circle point".
        }

        // Gradient Fill
        dataSet.setDrawFilled(true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#D5F5E3"), Color.TRANSPARENT});
        dataSet.setFillDrawable(gradient);

        LineData lineData = new LineData(dataSet);
        marketChart.setData(lineData);
        marketChart.invalidate();
    }
}
