package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.DashboardResponse;
import com.harvesthub.mobile.model.MarketTrendResponse;
import com.harvesthub.mobile.model.WeatherResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private TextView tvGreetingTitle, tvWeatherTemp, tvWeatherLocation;
    private TextView tvTotalRevenue, tvRevenueChange, tvPreviousRevenueSubtext;
    private TextView tvActiveListings, tvCompletedOrders, tvTrustScore, tvMarketAvg;
    private MarketTrendChartView marketChart;
    private LinearLayout bottomNavigation;
    private MaterialButton btnCreateListing, btnViewMarketInsights;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupBottomNavigation(view);
        renderChart(defaultTrendValues());

        fetchDashboardData("FARMER_ID_HERE");
        fetchWeatherData("Dambulla, LK");
        fetchMarketTrend("Carrot");

        btnCreateListing.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CreateListingActivity.class)));

        view.findViewById(R.id.ivNotification).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), NotificationsActivity.class)));

        btnViewMarketInsights.setOnClickListener(v ->
                Toast.makeText(getActivity(), "Navigating to Market Insights...", Toast.LENGTH_SHORT).show());

        return view;
    }

    private void initViews(View v) {
        tvGreetingTitle = v.findViewById(R.id.tvGreetingTitle);
        tvWeatherTemp = v.findViewById(R.id.tvWeatherTemp);
        tvWeatherLocation = v.findViewById(R.id.tvWeatherLocation);
        tvTotalRevenue = v.findViewById(R.id.tvTotalRevenue);
        tvRevenueChange = v.findViewById(R.id.tvRevenueChange);
        tvPreviousRevenueSubtext = v.findViewById(R.id.tvPreviousRevenueSubtext);
        tvMarketAvg = v.findViewById(R.id.tvMarketAvg);
        marketChart = v.findViewById(R.id.marketChart);
        bottomNavigation = v.findViewById(R.id.bottomNavigation);
        btnCreateListing = v.findViewById(R.id.btnCreateListing);
        btnViewMarketInsights = v.findViewById(R.id.btnViewMarketInsights);
    }

    private void setupBottomNavigation(View root) {
        root.findViewById(R.id.navListingsItem).setSelected(true);
        root.findViewById(R.id.navListingsItem).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ListingsActivity.class)));
        root.findViewById(R.id.navHomeItem).setOnClickListener(v ->
                Toast.makeText(getActivity(), "Home", Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.navMarketItem).setOnClickListener(v ->
                Toast.makeText(getActivity(), "Market", Toast.LENGTH_SHORT).show());
        root.findViewById(R.id.navWalletItem).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), WalletActivity.class)));
        root.findViewById(R.id.navSettingsItem).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SettingsActivity.class)));
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
                // Keep the reference-screen values visible when the API is unavailable.
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
                // Keep the reference-screen values visible when the API is unavailable.
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
                renderChart(defaultTrendValues());
            }
        });
    }

    private void renderChart(List<Double> trends) {
        marketChart.setTrendValues(trends);
    }

    private List<Double> defaultTrendValues() {
        return new ArrayList<>(Arrays.asList(
                122.0, 124.0, 121.0, 128.0, 152.0, 162.0, 184.0,
                174.0, 146.0, 154.0, 176.0, 168.0, 188.0, 214.0
        ));
    }
}
