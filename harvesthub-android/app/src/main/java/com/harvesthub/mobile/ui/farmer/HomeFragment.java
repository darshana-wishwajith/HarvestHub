package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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
    private MaterialCardView cvTab24h, cvTab7d, cvTab30d;
    private TextView tvTab24h, tvTab7d, tvTab30d;
    private LineChart marketChart;
    private MaterialButton btnCreateListing, btnViewMarketInsights;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupLineChart();
        setupTabListeners();

        renderChart(defaultTrendValues(), 7);

        fetchDashboardData("FARMER_ID_HERE");
        fetchWeatherData("Dambulla, LK");
        fetchMarketTrend("Carrot", 7);

        btnCreateListing.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ListingDetailsActivity.class)));

        btnViewMarketInsights.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Navigating to Market Insights...", Toast.LENGTH_SHORT).show());

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
        btnCreateListing = v.findViewById(R.id.btnCreateListing);
        btnViewMarketInsights = v.findViewById(R.id.btnViewMarketInsights);

        cvTab24h = v.findViewById(R.id.cvTab24h);
        cvTab7d = v.findViewById(R.id.cvTab7d);
        cvTab30d = v.findViewById(R.id.cvTab30d);
        tvTab24h = v.findViewById(R.id.tvTab24h);
        tvTab7d = v.findViewById(R.id.tvTab7d);
        tvTab30d = v.findViewById(R.id.tvTab30d);
    }

    private void setupTabListeners() {
        cvTab24h.setOnClickListener(v -> {
            updateTabUI(1);
            fetchMarketTrend("Carrot", 1);
        });
        cvTab7d.setOnClickListener(v -> {
            updateTabUI(7);
            fetchMarketTrend("Carrot", 7);
        });
        cvTab30d.setOnClickListener(v -> {
            updateTabUI(30);
            fetchMarketTrend("Carrot", 30);
        });
    }

    private void updateTabUI(int days) {
        // Reset all
        cvTab24h.setCardBackgroundColor(Color.TRANSPARENT);
        cvTab24h.setCardElevation(0);
        tvTab24h.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray));

        cvTab7d.setCardBackgroundColor(Color.TRANSPARENT);
        cvTab7d.setCardElevation(0);
        tvTab7d.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray));

        cvTab30d.setCardBackgroundColor(Color.TRANSPARENT);
        cvTab30d.setCardElevation(0);
        tvTab30d.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray));

        // Set active
        if (days == 1) {
            cvTab24h.setCardBackgroundColor(Color.WHITE);
            cvTab24h.setCardElevation(2);
            tvTab24h.setTextColor(Color.parseColor("#1C3529"));
        } else if (days == 7) {
            cvTab7d.setCardBackgroundColor(Color.WHITE);
            cvTab7d.setCardElevation(2);
            tvTab7d.setTextColor(Color.parseColor("#1C3529"));
        } else if (days == 30) {
            cvTab30d.setCardBackgroundColor(Color.WHITE);
            cvTab30d.setCardElevation(2);
            tvTab30d.setTextColor(Color.parseColor("#1C3529"));
        }
    }

    private void fetchDashboardData(String farmerId) {
        apiService.getFarmerDashboard(farmerId).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call, @NonNull Response<DashboardResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    updateDashboardUI(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
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
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    tvWeatherTemp.setText(response.body().getTemperature() + ", " + response.body().getCondition());
                    tvWeatherLocation.setText(response.body().getLocation());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
            }
        });
    }

    private void fetchMarketTrend(String crop, int days) {
        apiService.getMarketTrend(crop, days).enqueue(new Callback<MarketTrendResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketTrendResponse> call, @NonNull Response<MarketTrendResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    renderChart(response.body().getTrends(), days);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketTrendResponse> call, @NonNull Throwable t) {
                renderChart(defaultTrendValues(), days);
            }
        });
    }

    private void setupLineChart() {
        marketChart.getDescription().setEnabled(false);
        marketChart.getLegend().setEnabled(false);
        marketChart.getAxisRight().setEnabled(false);
        marketChart.getAxisLeft().setDrawGridLines(false);
        marketChart.getXAxis().setDrawGridLines(false);
        marketChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        marketChart.getXAxis().setGranularity(1f);
        marketChart.getXAxis().setTextColor(Color.GRAY);

        final String[] labels = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        marketChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
    }

    private void renderChart(List<Double> trends, int days) {
        if (trends == null || trends.isEmpty()) return;

        String[] labels;
        if (days == 1) {
            labels = new String[]{"00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:59"};
        } else if (days == 30) {
            labels = new String[]{"W1", "W2", "W3", "W4", "W5"};
        } else {
            labels = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        }
        marketChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        ArrayList<Entry> entries = new ArrayList<>();
        int count = Math.min(trends.size(), labels.length);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(i, trends.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Market Price");
        dataSet.setColor(Color.parseColor("#0FA968"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#0FA968"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);

        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gradient);
        if (drawable != null) dataSet.setFillDrawable(drawable);

        marketChart.setData(new LineData(dataSet));
        marketChart.invalidate();
        marketChart.animateX(1000);
    }

    private List<Double> defaultTrendValues() {
        return new ArrayList<>(Arrays.asList(
                122.0, 124.0, 121.0, 128.0, 152.0, 162.0, 184.0,
                174.0, 146.0, 154.0, 176.0, 168.0, 188.0, 214.0
        ));
    }
}
