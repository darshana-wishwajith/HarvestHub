package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.FragmentMarketBinding;
import com.harvesthub.mobile.databinding.ItemCenterHighlightBinding;

import java.util.ArrayList;
import java.util.List;

public class MarketFragment extends Fragment {

    private FragmentMarketBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMarketBinding.inflate(inflater, container, false);
        
        setupLineChart();
        setupRecyclerView();
        setupTabListeners();
        
        return binding.getRoot();
    }

    private void setupTabListeners() {
        binding.cvTab24h.setOnClickListener(v -> updateDurationTab(24));
        binding.cvTab7d.setOnClickListener(v -> updateDurationTab(7));
        binding.cvTab30d.setOnClickListener(v -> updateDurationTab(30));
    }

    private void updateDurationTab(int hours) {
        resetTabUI(binding.cvTab24h, binding.tvTab24h);
        resetTabUI(binding.cvTab7d, binding.tvTab7d);
        resetTabUI(binding.cvTab30d, binding.tvTab30d);

        if (hours == 24) {
            setActiveTabUI(binding.cvTab24h, binding.tvTab24h);
        } else if (hours == 7) {
            setActiveTabUI(binding.cvTab7d, binding.tvTab7d);
        } else {
            setActiveTabUI(binding.cvTab30d, binding.tvTab30d);
        }

        binding.lineChart.animateY(800);
    }

    private void resetTabUI(MaterialCardView card, TextView text) {
        card.setCardBackgroundColor(Color.TRANSPARENT);
        card.setCardElevation(0f);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.market_tab_item_text));
    }

    private void setActiveTabUI(MaterialCardView card, TextView text) {
        card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.market_tab_active_bg));
        card.setCardElevation(2f);
        text.setTextColor(ContextCompat.getColor(requireContext(), R.color.market_tab_active_text));
    }

    private void setupLineChart() {
        LineChart lineChart = binding.lineChart;
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawLabels(false);
        lineChart.getAxisLeft().setDrawAxisLine(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setDrawAxisLine(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setTextColor(ContextCompat.getColor(requireContext(), R.color.market_chart_axis));
        lineChart.getXAxis().setTextSize(10f);

        final String[] labels = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 160));
        entries.add(new Entry(1, 165));
        entries.add(new Entry(2, 185));
        entries.add(new Entry(3, 172));
        entries.add(new Entry(4, 188));
        entries.add(new Entry(5, 180));
        entries.add(new Entry(6, 195));

        LineDataSet dataSet = new LineDataSet(entries, "Market Price");
        int accentGreen = ContextCompat.getColor(requireContext(), R.color.accent_green);
        dataSet.setColor(accentGreen);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(accentGreen);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.market_chart_card_bg));
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);

        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_gradient);
        if (drawable != null) dataSet.setFillDrawable(drawable);

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
        lineChart.animateY(1000);
    }

    private void setupRecyclerView() {
        binding.rvCenterHighlights.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<MarketCenter> list = new ArrayList<>();
        list.add(new MarketCenter("Dambulla Market", "Central Province", "HARVEST NOW", "Low", 30, "High", 85, "15 mins ago", true));
        list.add(new MarketCenter("Keppetipola Center", "Uva Province", "DELAY HARVEST", "High", 80, "Low", 25, "1 hour ago", false));

        binding.rvCenterHighlights.setAdapter(new MarketAdapter(list));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class MarketCenter {
        String name, province, badge, supplyLevel, demandLevel, updateTime;
        int supplyProgress, demandProgress;
        boolean isPositive;

        MarketCenter(String name, String province, String badge, String supplyLevel, int supplyProgress, String demandLevel, int demandProgress, String updateTime, boolean isPositive) {
            this.name = name;
            this.province = province;
            this.badge = badge;
            this.supplyLevel = supplyLevel;
            this.supplyProgress = supplyProgress;
            this.demandLevel = demandLevel;
            this.demandProgress = demandProgress;
            this.updateTime = updateTime;
            this.isPositive = isPositive;
        }
    }

    static class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {
        private final List<MarketCenter> items;

        MarketAdapter(List<MarketCenter> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCenterHighlightBinding binding = ItemCenterHighlightBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MarketCenter item = items.get(position);
            ItemCenterHighlightBinding b = holder.binding;
            
            b.tvCenterName.setText(item.name);
            b.tvLocation.setText(item.province);
            b.tvBadgeText.setText(item.badge);
            b.tvUpdateTime.setText("Update: " + item.updateTime);

            b.tvSupplyLevel.setText(item.supplyLevel);
            b.pbSupply.setProgress(item.supplyProgress);
            b.tvDemandLevel.setText(item.demandLevel);
            b.pbDemand.setProgress(item.demandProgress);

            int lowColor = Color.parseColor("#F5A623");
            int highColor = ContextCompat.getColor(b.getRoot().getContext(), R.color.accent_green);

            if (item.supplyLevel.equalsIgnoreCase("Low")) {
                b.tvSupplyLevel.setTextColor(lowColor);
                b.pbSupply.setProgressTintList(ColorStateList.valueOf(lowColor));
            } else {
                b.tvSupplyLevel.setTextColor(highColor);
                b.pbSupply.setProgressTintList(ColorStateList.valueOf(highColor));
            }

            if (item.demandLevel.equalsIgnoreCase("High")) {
                b.tvDemandLevel.setTextColor(highColor);
                b.pbDemand.setProgressTintList(ColorStateList.valueOf(highColor));
            } else {
                b.tvDemandLevel.setTextColor(lowColor);
                b.pbDemand.setProgressTintList(ColorStateList.valueOf(lowColor));
            }

            if (item.isPositive) {
                b.cvBadge.setCardBackgroundColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.badge_harvest_bg));
                b.tvBadgeText.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.badge_harvest_text));
            } else {
                b.cvBadge.setCardBackgroundColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.badge_delay_bg));
                b.tvBadgeText.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), R.color.badge_delay_text));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemCenterHighlightBinding binding;
            ViewHolder(ItemCenterHighlightBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
