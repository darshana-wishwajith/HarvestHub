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

        View.OnClickListener openMarket = v -> {
            Intent i = new Intent(requireContext(), com.harvesthub.mobile.MainActivity.class);
            i.putExtra(com.harvesthub.mobile.MainActivity.EXTRA_INITIAL_TAB, com.harvesthub.mobile.MainActivity.Tab.MARKET.name());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        };
        binding.btnMarketInsightsMenu.setOnClickListener(openMarket);
        binding.cardMarketTrends.setOnClickListener(openMarket);

        return binding.getRoot();
    }

    private void setupRefreshLayout() {
        binding.swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.primary_green));
        binding.swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    /** Crop shown on the Home price chart. */
    private static final String HOME_CROP = "Big Onions";

    private void refreshData() {
        if (binding != null) binding.swipeRefreshLayout.setRefreshing(true);

        final int[] pending = {3};
        Runnable checkDone = () -> {
            pending[0]--;
            if (pending[0] <= 0 && binding != null) {
                binding.swipeRefreshLayout.setRefreshing(false);
            }
        };

        String farmerId = sessionManager.getUserId();
        fetchDashboardData(farmerId, checkDone);
        fetchMarketTrend(HOME_CROP, 7, checkDone);
        fetchWeather(checkDone);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
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

    private void fetchWeather(Runnable onDone) {
        apiService.getWeather(null).enqueue(new Callback<com.harvesthub.mobile.model.WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<com.harvesthub.mobile.model.WeatherResponse> call,
                                   @NonNull Response<com.harvesthub.mobile.model.WeatherResponse> response) {
                if (onDone != null) onDone.run();
                if (binding == null) return;
                com.harvesthub.mobile.model.WeatherResponse w = response.body();
                if (response.isSuccessful() && w != null && w.getTemperature() != null) {
                    String place = w.getLocation() == null ? "" : "  |  " + w.getLocation();
                    binding.tvWeather.setText(w.getTemperature() + ", " + w.getCondition() + place);
                    binding.tvWeather.setVisibility(View.VISIBLE);
                } else {
                    binding.tvWeather.setVisibility(View.GONE); // no data: hide instead of guessing
                }
            }

            @Override
            public void onFailure(@NonNull Call<com.harvesthub.mobile.model.WeatherResponse> call, @NonNull Throwable t) {
                if (onDone != null) onDone.run();
                if (binding != null) binding.tvWeather.setVisibility(View.GONE);
            }
        });
    }

    private void updateDashboardUI(DashboardResponse data) {
        if (binding == null) return;
        if (data.getRevenueChange() != null) {
            binding.tvRevenueChange.setText(data.getRevenueChange() + " vs LKR " + data.getPreviousRevenue() + " last month");
            binding.tvRevenueChange.setVisibility(View.VISIBLE);
        } else {
            binding.tvRevenueChange.setVisibility(View.GONE);
        }
        binding.tvTrustPercent.setText(data.getTrustPercent() + "%");
        binding.tvRating.setText(data.getRatingAverage() == null ? "No ratings"
                : String.format(Locale.US, "%.1f / 5.0", data.getRatingAverage()));
        if (data.getFarmerName() != null) {
            String greeting = data.getGreeting() != null ? data.getGreeting() : "Hello, " + data.getFarmerName() + "!";
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
                    showTrend(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarketTrendResponse> call, @NonNull Throwable t) {
                if (onDone != null) onDone.run();
            }
        });
    }

    private void showTrend(MarketTrendResponse t) {
        if (binding == null) return;
        if (t.getCurrentAverage() == null || t.getTrends() == null || t.getTrends().isEmpty()) {
            binding.tvMarketAvg.setText(HOME_CROP + ": no price data yet");
            binding.tvMarketChange.setText("");
            binding.layoutMarketTrendAlert.setVisibility(View.GONE);
            binding.marketChart.clear();
            return;
        }
        binding.tvMarketAvg.setText(String.format(Locale.US, "%s  LKR %,.2f /kg", HOME_CROP, t.getCurrentAverage()));
        binding.tvMarketChange.setText(t.getChangePercent() == null ? "PAST 7 DAYS"
                : String.format(Locale.US, "%+.1f%%  PAST 7 DAYS", t.getChangePercent()));
        binding.tvMarketChange.setTextColor(t.getChangePercent() != null && t.getChangePercent() < 0
                ? 0xFFDC2626 : 0xFF4CAF50);
        if (t.getInsight() != null) {
            binding.tvMarketTrendAlert.setText(t.getInsight());
            binding.layoutMarketTrendAlert.setVisibility(View.VISIBLE);
        }
        renderChart(t.getTrends(), t.getLabels());
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

    private void renderChart(List<Double> trends, List<String> dates) {
        if (binding == null || trends == null || trends.isEmpty()) return;

        // x-axis: day names of the real dates returned by the backend
        String[] labels = new String[trends.size()];
        for (int i = 0; i < labels.length; i++) {
            String d = dates != null && i < dates.size() ? dates.get(i) : null;
            try {
                labels[i] = d == null ? "" : new java.text.SimpleDateFormat("EEE", Locale.US)
                        .format(new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(d)).toUpperCase(Locale.US);
            } catch (java.text.ParseException e) {
                labels[i] = "";
            }
        }
        binding.marketChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        ArrayList<Entry> entries = new ArrayList<>();
        int count = trends.size();
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

}
