package com.harvesthub.mobile.ui.farmer;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

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
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.FragmentHomeBinding;
import com.harvesthub.mobile.model.DashboardResponse;
import com.harvesthub.mobile.model.MarketTrendResponse;
import com.harvesthub.mobile.model.WeatherResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        
        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        setupLineChart();
        setupRefreshLayout();

        refreshData();

        binding.btnCreateListing.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CreateListingFragment())
                        .addToBackStack(null)
                        .commit());

        binding.btnMarketInsightsMenu.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MarketInsightsActivity.class)));

        binding.cardMarketTrends.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MarketInsightsActivity.class)));

        return binding.getRoot();
    }

    private void setupRefreshLayout() {
        binding.swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary_green));
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    private void refreshData() {
        if (binding != null) binding.swipeRefreshLayout.setRefreshing(true);
        renderChart(defaultTrendValues(), 7);
        
        final int[] pending = {2};
        Runnable checkDone = () -> {
            pending[0]--;
            if (pending[0] <= 0 && binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        };

        String farmerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null) ? sessionManager.getUser().getId() : "";
        String location = (sessionManager.getUser() != null && sessionManager.getUser().getLocation() != null) ? sessionManager.getUser().getLocation() : "Dambulla, LK";

        fetchDashboardData(farmerId, checkDone);
        fetchMarketTrend("Carrot", 7, checkDone);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchDashboardData(String farmerId, Runnable onDone) {
        apiService.getFarmerDashboard(farmerId).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(@NonNull Call<DashboardResponse> call, @NonNull Response<DashboardResponse> response) {
                if (onDone != null) onDone.run();
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    updateDashboardUI(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<DashboardResponse> call, @NonNull Throwable t) {
                if (onDone != null) onDone.run();
            }
        });
    }

    private void updateDashboardUI(DashboardResponse data) {
        if (binding == null) return;
        if (data.getFarmerName() != null) {
            String greeting = "Good morning, " + data.getFarmerName() + "!";
            if (!binding.tvGreetingTitle.getText().toString().equals(greeting)) {
                binding.tvGreetingTitle.setText(greeting);
                binding.tvGreetingTitle.setAlpha(0f);
                binding.tvGreetingTitle.animate().alpha(1f).setDuration(500).start();
            }
        }
        
        animateValue(binding.tvTotalRevenue, data.getTotalRevenue(), "LKR ", "");
        animateValue(binding.tvActiveListings, data.getActiveListings(), "", "");
        animateValue(binding.tvCompletedOrders, data.getCompletedOrders(), "", "");
    }

    private void animateValue(TextView textView, String valueStr, String prefix, String suffix) {
        if (valueStr == null) return;
        try {
            String numericValue = valueStr.replaceAll("[^0-9]", "");
            if (numericValue.isEmpty()) {
                textView.setText(prefix + valueStr + suffix);
                return;
            }
            int finalValue = Integer.parseInt(numericValue);
            
            ValueAnimator animator = ValueAnimator.ofInt(0, finalValue);
            animator.setDuration(1000);
            animator.addUpdateListener(animation -> {
                int val = (int) animation.getAnimatedValue();
                if (prefix.contains("LKR")) {
                    textView.setText(prefix + String.format(Locale.US, "%,d", val) + suffix);
                } else {
                    textView.setText(prefix + (val < 10 && suffix.isEmpty() && !valueStr.startsWith("0") ? val : String.format(Locale.US, "%02d", val)) + suffix);
                }
            });
            animator.start();
        } catch (Exception e) {
            textView.setText(prefix + valueStr + suffix);
        }
    }

    private void fetchMarketTrend(String crop, int days, Runnable onDone) {
        apiService.getMarketTrend(crop, days).enqueue(new Callback<MarketTrendResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketTrendResponse> call, @NonNull Response<MarketTrendResponse> response) {
                if (onDone != null) onDone.run();
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    renderChart(response.body().getTrends(), days);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketTrendResponse> call, @NonNull Throwable t) {
                if (onDone != null) onDone.run();
                renderChart(defaultTrendValues(), days);
            }
        });
    }

    private void setupLineChart() {
        LineChart marketChart = binding.marketChart;
        marketChart.getDescription().setEnabled(false);
        marketChart.getLegend().setEnabled(false);
        marketChart.getAxisRight().setEnabled(false);
        marketChart.getAxisLeft().setDrawGridLines(false);
        marketChart.getAxisLeft().setDrawLabels(false);
        marketChart.getAxisLeft().setDrawAxisLine(false);
        marketChart.getXAxis().setDrawGridLines(false);
        marketChart.getXAxis().setDrawAxisLine(false);
        marketChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        marketChart.getXAxis().setGranularity(1f);
        marketChart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.market_chart_axis));
        marketChart.getXAxis().setTextSize(10f);

        final String[] labels = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        marketChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
    }

    private void renderChart(List<Double> trends, int days) {
        if (binding == null || trends == null || trends.isEmpty()) return;

        String[] labels = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        binding.marketChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        ArrayList<Entry> entries = new ArrayList<>();
        int count = Math.min(trends.size(), labels.length);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(i, trends.get(i).floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Market Price");
        int accentGreen = ContextCompat.getColor(requireContext(), R.color.accent_green);
        dataSet.setColor(accentGreen);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(accentGreen);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.stat_card_bg));
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);

        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gradient);
        if (drawable != null) dataSet.setFillDrawable(drawable);

        binding.marketChart.setData(new LineData(dataSet));
        binding.marketChart.invalidate();
        binding.marketChart.animateY(1200);
    }

    private List<Double> defaultTrendValues() {
        return new ArrayList<>(Arrays.asList(
                122.0, 124.0, 150.0, 128.0, 152.0, 142.0, 184.0
        ));
    }
}
