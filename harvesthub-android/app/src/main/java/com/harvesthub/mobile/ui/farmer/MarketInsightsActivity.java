package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.harvesthub.mobile.utils.NotificationPanelHelper;

import java.util.ArrayList;
import java.util.List;

public class MarketInsightsActivity extends AppCompatActivity {

    private LineChart lineChart;
    private RecyclerView rvCenterHighlights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_market_insights);

        initViews();
        setupLineChart();
        setupRecyclerView();
        setupTopNav();
    }

    private void setupTopNav() {
        View btnNotification = findViewById(R.id.iconNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> NotificationPanelHelper.showNotificationPanel(this, v));
        }
    }

    private void initViews() {
        lineChart = findViewById(R.id.lineChart);
        rvCenterHighlights = findViewById(R.id.rvCenterHighlights);
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setTextColor(Color.GRAY);

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
        dataSet.setColor(Color.parseColor("#0FA968"));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#0FA968"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.bg_gradient);
        dataSet.setFillDrawable(drawable);

        lineChart.setData(new LineData(dataSet));
        lineChart.animateX(1000);
    }

    private void setupRecyclerView() {
        rvCenterHighlights.setLayoutManager(new LinearLayoutManager(this));
        List<MarketCenter> list = new ArrayList<>();
        // HIGHLIGHT CARD 1 - Dambulla
        list.add(new MarketCenter("Dambulla Market", "Central Province", "HARVEST NOW", "Low", 30, "High", 85, "15 mins ago", true));
        // HIGHLIGHT CARD 2 - Keppetipola
        list.add(new MarketCenter("Keppetipola Center", "Uva Province", "DELAY HARVEST", "High", 80, "Low", 25, "1 hour ago", false));

        rvCenterHighlights.setAdapter(new MarketAdapter(list));
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
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_center_highlight, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MarketCenter item = items.get(position);
            holder.tvName.setText(item.name);
            holder.tvLoc.setText(item.province);
            holder.tvBadge.setText(item.badge);
            holder.tvUpdate.setText("Update: " + item.updateTime);

            holder.tvSupply.setText(item.supplyLevel);
            holder.pbSupply.setProgress(item.supplyProgress);
            holder.tvDemand.setText(item.demandLevel);
            holder.pbDemand.setProgress(item.demandProgress);

            // Dynamic colors for Supply
            if (item.supplyLevel.equalsIgnoreCase("Low")) {
                holder.tvSupply.setTextColor(Color.parseColor("#F5A623")); // Orange
                holder.pbSupply.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F5A623")));
            } else {
                holder.tvSupply.setTextColor(Color.parseColor("#0FA968")); // Green
                holder.pbSupply.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#0FA968")));
            }

            // Dynamic colors for Demand
            if (item.demandLevel.equalsIgnoreCase("High")) {
                holder.tvDemand.setTextColor(Color.parseColor("#0FA968")); // Green
                holder.pbDemand.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#0FA968")));
            } else {
                holder.tvDemand.setTextColor(Color.parseColor("#F5A623")); // Orange
                holder.pbDemand.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F5A623")));
            }

            // Badge colors based on status
            if (item.isPositive) {
                holder.cvBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                holder.tvBadge.setTextColor(Color.parseColor("#0FA968"));
            } else {
                holder.cvBadge.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                holder.tvBadge.setTextColor(Color.parseColor("#D32F2F"));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvLoc, tvBadge, tvSupply, tvDemand, tvUpdate;
            ProgressBar pbSupply, pbDemand;
            MaterialCardView cvBadge;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvCenterName);
                tvLoc = v.findViewById(R.id.tvLocation);
                tvBadge = v.findViewById(R.id.tvBadgeText);
                tvSupply = v.findViewById(R.id.tvSupplyLevel);
                tvDemand = v.findViewById(R.id.tvDemandLevel);
                tvUpdate = v.findViewById(R.id.tvUpdateTime);
                pbSupply = v.findViewById(R.id.pbSupply);
                pbDemand = v.findViewById(R.id.pbDemand);
                cvBadge = v.findViewById(R.id.cvBadge);
            }
        }
    }
}
