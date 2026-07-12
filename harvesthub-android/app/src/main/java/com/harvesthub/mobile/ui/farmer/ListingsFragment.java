package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerSummaryResponse;
import com.harvesthub.mobile.model.HistoryListing;
import com.harvesthub.mobile.model.WantedAd;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsFragment extends Fragment {

    private TextView tvMonthlyRev, tvTrustScore, tvEmptyState;
    private TextView tvTabActive, tvTabHistory, tvTabWantedAds;
    private View indicatorActive, indicatorHistory, indicatorWantedAds;
    private EditText etSearch;
    private RecyclerView rvWantedAds;
    private WantedAdAdapter wantedAdAdapter;
    private HistoryListingAdapter historyAdapter;
    private List<WantedAd> allWantedAds = new ArrayList<>();
    private List<HistoryListing> allHistory = new ArrayList<>();
    private List<WantedAd> filteredWantedAds = new ArrayList<>();
    private List<HistoryListing> filteredHistory = new ArrayList<>();
    private ApiService apiService;
    private String farmerId = "FARMER_123";
    private String currentTab = "Wanted Ads";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listings, container, false);
        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupTabs(view);
        setupSearch();
        fetchSummary();
        fetchWantedAds();
        return view;
    }

    private void initViews(View v) {
        tvMonthlyRev = v.findViewById(R.id.tvMonthlyRev);
        tvTrustScore = v.findViewById(R.id.tvTrustScore);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);
        tvTabActive = v.findViewById(R.id.tvTabActive);
        tvTabHistory = v.findViewById(R.id.tvTabHistory);
        tvTabWantedAds = v.findViewById(R.id.tvTabWantedAds);
        indicatorActive = v.findViewById(R.id.indicatorActive);
        indicatorHistory = v.findViewById(R.id.indicatorHistory);
        indicatorWantedAds = v.findViewById(R.id.indicatorWantedAds);
        etSearch = v.findViewById(R.id.etSearch);
        rvWantedAds = v.findViewById(R.id.rvWantedAds);

        rvWantedAds.setLayoutManager(new LinearLayoutManager(getContext()));
        wantedAdAdapter = new WantedAdAdapter(filteredWantedAds, this::onSendOfferClick);
        historyAdapter = new HistoryListingAdapter(filteredHistory, this::onHistoryItemClick);
        rvWantedAds.setAdapter(wantedAdAdapter);

        v.findViewById(R.id.ivNotificationTop).setOnClickListener(view ->
                startActivity(new Intent(getActivity(), NotificationsActivity.class)));
        
        // Bottom nav listeners (Home example)
        v.findViewById(R.id.navHomeItem).setOnClickListener(view -> {
             // In a real app, switch fragment or activity. For now, we are in Dashboard which holds HomeFragment by default.
             // Assume parent Activity handles this.
        });
    }

    private void setupTabs(View v) {
        v.findViewById(R.id.tabActive).setOnClickListener(view -> selectTab("Active"));
        v.findViewById(R.id.tabHistory).setOnClickListener(view -> selectTab("History"));
        v.findViewById(R.id.tabWantedAds).setOnClickListener(view -> selectTab("Wanted Ads"));
    }

    private void selectTab(String tabName) {
        // Reset all
        tvTabActive.setTypeface(null, Typeface.NORMAL);
        tvTabHistory.setTypeface(null, Typeface.NORMAL);
        tvTabWantedAds.setTypeface(null, Typeface.NORMAL);
        tvTabActive.setTextColor(getResources().getColor(R.color.text_gray_medium));
        tvTabHistory.setTextColor(getResources().getColor(R.color.text_gray_medium));
        tvTabWantedAds.setTextColor(getResources().getColor(R.color.text_gray_medium));
        indicatorActive.setVisibility(View.INVISIBLE);
        indicatorHistory.setVisibility(View.INVISIBLE);
        indicatorWantedAds.setVisibility(View.INVISIBLE);

        if (tabName.equals("Active")) {
            currentTab = "Active";
            tvTabActive.setTypeface(null, Typeface.BOLD);
            tvTabActive.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorActive.setVisibility(View.VISIBLE);
            rvWantedAds.setAdapter(wantedAdAdapter);
            showEmptyState("No active listings");
        } else if (tabName.equals("History")) {
            currentTab = "History";
            tvTabHistory.setTypeface(null, Typeface.BOLD);
            tvTabHistory.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorHistory.setVisibility(View.VISIBLE);
            rvWantedAds.setAdapter(historyAdapter);
            fetchHistory();
        } else {
            currentTab = "Wanted Ads";
            tvTabWantedAds.setTypeface(null, Typeface.BOLD);
            tvTabWantedAds.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorWantedAds.setVisibility(View.VISIBLE);
            rvWantedAds.setAdapter(wantedAdAdapter);
            fetchWantedAds();
        }
    }

    private void showEmptyState(String msg) {
        filteredWantedAds.clear();
        filteredHistory.clear();
        wantedAdAdapter.notifyDataSetChanged();
        historyAdapter.notifyDataSetChanged();
        tvEmptyState.setText(msg);
        tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentTab.equals("History")) {
                    updateHistoryFilter(s.toString());
                } else {
                    updateFilteredList(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchSummary() {
        apiService.getFarmerSummary(farmerId).enqueue(new Callback<FarmerSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<FarmerSummaryResponse> call, @NonNull Response<FarmerSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvMonthlyRev.setText(response.body().getMonthlyRevenue());
                    tvTrustScore.setText(response.body().getTrustScore());
                }
            }
            @Override public void onFailure(@NonNull Call<FarmerSummaryResponse> call, @NonNull Throwable t) {}
        });
    }

    private void fetchWantedAds() {
        apiService.getWantedAds(null, null).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(@NonNull Call<List<WantedAd>> call, @NonNull Response<List<WantedAd>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allWantedAds.clear();
                    allWantedAds.addAll(response.body());
                    updateFilteredList(etSearch.getText().toString());
                } else {
                    addMockData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<WantedAd>> call, @NonNull Throwable t) {
                addMockData();
            }
        });
    }

    private void addMockData() {
        allWantedAds.clear();
        allWantedAds.add(new WantedAd("1", "Big Onions", "Dambulla Sector", 1000, 280, true));
        allWantedAds.add(new WantedAd("2", "Tomatoes", "Colombo Sector", 500, 150, false));
        allWantedAds.add(new WantedAd("3", "Potatoes", "Nuwara Eliya Sector", 2500, 190, false));
        updateFilteredList(etSearch.getText().toString());
    }

    private void fetchHistory() {
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allHistory.clear();
                    allHistory.addAll(response.body());
                    updateHistoryFilter(etSearch.getText().toString());
                } else {
                    addMockHistory();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                addMockHistory();
            }
        });
    }

    private void addMockHistory() {
        allHistory.clear();
        allHistory.add(new HistoryListing("101", "Big Onions", "DELIVERED", "Oct 12, 2023", null, "850", "LKR 238,000", null));
        allHistory.add(new HistoryListing("102", "Potatoes", "CLOSED", "Oct 08, 2023", "GreenMart Ltd", null, null, "LKR 185"));
        allHistory.add(new HistoryListing("103", "Carrots", "DELIVERED", "Sep 28, 2023", null, "1,200", "LKR 156,000", null));
        updateHistoryFilter(etSearch.getText().toString());
    }

    private void updateHistoryFilter(String query) {
        filteredHistory.clear();
        if (query.isEmpty()) {
            filteredHistory.addAll(allHistory);
        } else {
            String lowerQuery = query.toLowerCase();
            for (HistoryListing h : allHistory) {
                if (h.getCropName().toLowerCase().contains(lowerQuery) ||
                        (h.getBuyerName() != null && h.getBuyerName().toLowerCase().contains(lowerQuery))) {
                    filteredHistory.add(h);
                }
            }
        }
        historyAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredHistory.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredHistory.isEmpty()) tvEmptyState.setText("No history found");
    }

    private void updateFilteredList(String query) {
        filteredWantedAds.clear();
        if (query.isEmpty()) {
            filteredWantedAds.addAll(allWantedAds);
        } else {
            String lowerQuery = query.toLowerCase();
            for (WantedAd ad : allWantedAds) {
                if (ad.getCropName().toLowerCase().contains(lowerQuery) || 
                    ad.getLocation().toLowerCase().contains(lowerQuery)) {
                    filteredWantedAds.add(ad);
                }
            }
        }
        wantedAdAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredWantedAds.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredWantedAds.isEmpty()) tvEmptyState.setText("No ads found");
    }

    private void onHistoryItemClick(HistoryListing item) {
        // Navigate to detail or receipt
    }

    private void onSendOfferClick(WantedAd ad) {
        Intent intent = new Intent(getActivity(), CreateSupplyOfferActivity.class);
        intent.putExtra("requestId", ad.getRequestId());
        intent.putExtra("cropName", ad.getCropName());
        intent.putExtra("requestedQuantity", ad.getRequiredQuantityKg());
        intent.putExtra("targetPrice", ad.getTargetPricePerKg());
        intent.putExtra("location", ad.getLocation());
        startActivity(intent);
    }
}
