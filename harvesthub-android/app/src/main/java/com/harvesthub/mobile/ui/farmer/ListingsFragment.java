package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ActiveListing;
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

public class ListingsFragment extends Fragment implements ActiveListingAdapter.OnListingActionListener {

    private TextView tvMonthlyRev, tvTrustScore, tvEmptyState;
    private TextView tvTabActive, tvTabHistory, tvTabWantedAds;
    private View indicatorActive, indicatorHistory, indicatorWantedAds;
    private View scrollFilters;
    private ChipGroup chipGroupFilters;
    private EditText etSearch;
    private RecyclerView rvListings;
    private FloatingActionButton fabCreateListing;
    
    private WantedAdAdapter wantedAdAdapter;
    private HistoryListingAdapter historyAdapter;
    private ActiveListingAdapter activeAdapter;
    
    private final List<WantedAd> allWantedAds = new ArrayList<>();
    private final List<WantedAd> filteredWantedAds = new ArrayList<>();
    private final List<HistoryListing> allHistory = new ArrayList<>();
    private final List<HistoryListing> filteredHistory = new ArrayList<>();
    private final List<ActiveListing> allActive = new ArrayList<>();
    private final List<ActiveListing> filteredActive = new ArrayList<>();
    
    private ApiService apiService;
    private final String farmerId = "FARMER_123";
    private String currentTab = "Active";
    private String activeStatusFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listings, container, false);
        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupTabs(view);
        setupSearch();
        setupFilterChips();
        fetchSummary();
        
        selectTab("Active");
        
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
        scrollFilters = v.findViewById(R.id.scrollFilters);
        chipGroupFilters = v.findViewById(R.id.chipGroupFilters);
        etSearch = v.findViewById(R.id.etSearch);
        rvListings = v.findViewById(R.id.rvListings);
        fabCreateListing = v.findViewById(R.id.fabCreateListing);

        rvListings.setLayoutManager(new LinearLayoutManager(getContext()));
        
        wantedAdAdapter = new WantedAdAdapter(filteredWantedAds, this::onSendOfferClick);
        historyAdapter = new HistoryListingAdapter(filteredHistory);
        activeAdapter = new ActiveListingAdapter(filteredActive, this);

        v.findViewById(R.id.ivNotificationTop).setOnClickListener(view ->
                startActivity(new Intent(getActivity(), NotificationsActivity.class)));
        
        fabCreateListing.setOnClickListener(v1 -> {
            Toast.makeText(getContext(), "Create Listing", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabs(View v) {
        v.findViewById(R.id.tabActive).setOnClickListener(view -> selectTab("Active"));
        v.findViewById(R.id.tabHistory).setOnClickListener(view -> selectTab("History"));
        v.findViewById(R.id.tabWantedAds).setOnClickListener(view -> selectTab("Wanted Ads"));
    }

    private void selectTab(String tabName) {
        currentTab = tabName;
        resetTabStyles();

        scrollFilters.setVisibility(tabName.equals("Active") ? View.VISIBLE : View.GONE);
        fabCreateListing.setVisibility(tabName.equals("Active") ? View.VISIBLE : View.GONE);

        if (tabName.equals("Active")) {
            tvTabActive.setTypeface(null, Typeface.BOLD);
            tvTabActive.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorActive.setVisibility(View.VISIBLE);
            rvListings.setAdapter(activeAdapter);
            fetchActiveListings();
        } else if (tabName.equals("History")) {
            tvTabHistory.setTypeface(null, Typeface.BOLD);
            tvTabHistory.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorHistory.setVisibility(View.VISIBLE);
            rvListings.setAdapter(historyAdapter);
            fetchHistory();
        } else {
            tvTabWantedAds.setTypeface(null, Typeface.BOLD);
            tvTabWantedAds.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorWantedAds.setVisibility(View.VISIBLE);
            rvListings.setAdapter(wantedAdAdapter);
            fetchWantedAds();
        }
    }

    private void resetTabStyles() {
        tvTabActive.setTypeface(null, Typeface.NORMAL);
        tvTabHistory.setTypeface(null, Typeface.NORMAL);
        tvTabWantedAds.setTypeface(null, Typeface.NORMAL);
        tvTabActive.setTextColor(getResources().getColor(R.color.text_gray_medium));
        tvTabHistory.setTextColor(getResources().getColor(R.color.text_gray_medium));
        tvTabWantedAds.setTextColor(getResources().getColor(R.color.text_gray_medium));
        indicatorActive.setVisibility(View.INVISIBLE);
        indicatorHistory.setVisibility(View.INVISIBLE);
        indicatorWantedAds.setVisibility(View.INVISIBLE);
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    Chip c = (Chip) group.getChildAt(i);
                    if (c.getId() == checkedId) {
                        c.setChipBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.primary_green)));
                        c.setTextColor(Color.WHITE);
                        c.setChipStrokeWidth(0);
                    } else {
                        c.setChipBackgroundColor(ColorStateList.valueOf(Color.WHITE));
                        c.setTextColor(Color.parseColor("#828282"));
                        c.setChipStrokeWidth(1);
                    }
                }
                activeStatusFilter = chip.getText().toString().toUpperCase();
                updateActiveFilter(etSearch.getText().toString());
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (currentTab.equals("Active")) {
                    updateActiveFilter(s.toString());
                } else if (currentTab.equals("History")) {
                    updateHistoryFilter(s.toString());
                } else {
                    updateWantedAdsFilter(s.toString());
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

    private void fetchActiveListings() {
        apiService.getActiveListings(farmerId, "ALL", null).enqueue(new Callback<List<ActiveListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<ActiveListing>> call, @NonNull Response<List<ActiveListing>> response) {
                allActive.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allActive.addAll(response.body());
                } else {
                    addMockActive();
                }
                updateActiveFilter(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<ActiveListing>> call, @NonNull Throwable t) {
                addMockActive();
                updateActiveFilter(etSearch.getText().toString());
            }
        });
    }

    private void addMockActive() {
        allActive.clear();
        allActive.add(new ActiveListing("1", "#TM-2024", "Tomatoes", null, "DRAFT", "120 KG", "LKR 180/kg", "Oct 24", "Yesterday", "Dambulla Sector"));
        allActive.add(new ActiveListing("2", "#BO-2024", "Big Onions", null, "PENDING", "500 KG", "LKR 250/kg", "Oct 20", "2h ago", "Dambulla Sector"));
        allActive.add(new ActiveListing("3", "#CR-2024", "Carrots", null, "UNDER REVIEW", "300 KG", "LKR 220/kg", "Oct 25", "1h ago", "Nuwara Eliya"));
        allActive.add(new ActiveListing("4", "#PT-2024", "Potatoes", null, "LISTED", "450 KG", "LKR 190/kg", "Oct 15", "3h ago", "Jaffna Sector"));
        allActive.add(new ActiveListing("5", "#LK-2024", "Leeks", null, "ONGOING", "200 KG", "LKR 210/kg", "Oct 25", "15m ago", "Badulla Sector"));
        allActive.add(new ActiveListing("6", "#CB-2024", "Cabbage", null, "DEACTIVATED", "400 KG", "LKR 160/kg", "Oct 10", "3 days ago", "Kandy Sector"));
    }

    private void updateActiveFilter(String query) {
        filteredActive.clear();
        String q = query.toLowerCase();
        for (ActiveListing item : allActive) {
            boolean matchesStatus = activeStatusFilter.equals("ALL") || item.getStatus().equalsIgnoreCase(activeStatusFilter);
            boolean matchesSearch = item.getCropName().toLowerCase().contains(q) || item.getListingCode().toLowerCase().contains(q);
            if (matchesStatus && matchesSearch) {
                filteredActive.add(item);
            }
        }
        activeAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredActive.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyState.setText("No active listings found");
    }

    private void fetchWantedAds() {
        apiService.getWantedAds(null, null).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(@NonNull Call<List<WantedAd>> call, @NonNull Response<List<WantedAd>> response) {
                allWantedAds.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allWantedAds.addAll(response.body());
                } else {
                    addMockWantedAds();
                }
                updateWantedAdsFilter(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<WantedAd>> call, @NonNull Throwable t) {
                addMockWantedAds();
                updateWantedAdsFilter(etSearch.getText().toString());
            }
        });
    }

    private void addMockWantedAds() {
        allWantedAds.clear();
        allWantedAds.add(new WantedAd("1", "Big Onions", "Dambulla Sector", 1000, 280, true));
        allWantedAds.add(new WantedAd("2", "Tomatoes", "Colombo Sector", 500, 150, false));
    }

    private void updateWantedAdsFilter(String query) {
        filteredWantedAds.clear();
        String lowerQuery = query.toLowerCase();
        for (WantedAd ad : allWantedAds) {
            if (ad.getCropName().toLowerCase().contains(lowerQuery) || ad.getLocation().toLowerCase().contains(lowerQuery)) {
                filteredWantedAds.add(ad);
            }
        }
        wantedAdAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredWantedAds.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyState.setText("No ads found");
    }

    private void fetchHistory() {
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                allHistory.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allHistory.addAll(response.body());
                } else {
                    addMockHistory();
                }
                updateHistoryFilter(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                addMockHistory();
                updateHistoryFilter(etSearch.getText().toString());
            }
        });
    }

    private void addMockHistory() {
        allHistory.clear();
        allHistory.add(new HistoryListing("101", "Big Onions", "DELIVERED", "Oct 12, 2023", null, "850", "LKR 238,000", null));
        allHistory.add(new HistoryListing("102", "Potatoes", "CLOSED", "Oct 08, 2023", "GreenMart Ltd", null, null, "LKR 185"));
    }

    private void updateHistoryFilter(String query) {
        filteredHistory.clear();
        String lowerQuery = query.toLowerCase();
        for (HistoryListing h : allHistory) {
            if (h.getCropName().toLowerCase().contains(lowerQuery) || (h.getBuyerName() != null && h.getBuyerName().toLowerCase().contains(lowerQuery))) {
                filteredHistory.add(h);
            }
        }
        historyAdapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredHistory.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyState.setText("No history found");
    }

    private void onHistoryItemClick(HistoryListing item) {
        Toast.makeText(getActivity(), "Viewing " + item.getCropName(), Toast.LENGTH_SHORT).show();
    }

    private void onSendOfferClick(WantedAd ad) {
        Intent intent = new Intent(getActivity(), CreateSupplyOfferActivity.class);
        intent.putExtra("requestId", ad.getRequestId());
        intent.putExtra("cropName", ad.getCropName());
        startActivity(intent);
    }

    @Override public void onEdit(ActiveListing listing) { Toast.makeText(getContext(), "Edit " + listing.getListingCode(), Toast.LENGTH_SHORT).show(); }
    @Override public void onDeactivate(ActiveListing listing) { 
        new AlertDialog.Builder(requireContext())
                .setTitle("Deactivate Listing")
                .setMessage("Are you sure you want to deactivate this listing?")
                .setPositiveButton("Deactivate", (d, w) -> {
                    listing.setStatus("DEACTIVATED");
                    updateActiveFilter(etSearch.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    @Override public void onReactivate(ActiveListing listing) { 
        listing.setStatus("LISTED");
        updateActiveFilter(etSearch.getText().toString());
    }
    @Override public void onChat(ActiveListing listing) { /* startActivity(ChatActivity) */ }
    @Override public void onTrack(ActiveListing listing) { /* startActivity(TrackActivity) */ }
}
