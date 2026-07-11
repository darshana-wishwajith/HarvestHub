package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerSummaryResponse;
import com.harvesthub.mobile.model.ListingItem;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsActivity extends AppCompatActivity {

    private TextView tvMonthlyRev, tvTrustScore, tvEmptyState;
    private TextView tvTabActive, tvTabHistory, tvTabWantedAds;
    private View indicatorActive, indicatorHistory, indicatorWantedAds;
    private EditText etSearch;
    private RecyclerView rvListings;
    private ListingAdapter adapter;
    private final List<ListingItem> allItems = new ArrayList<>();
    private final List<ListingItem> filteredItems = new ArrayList<>();
    private ApiService apiService;
    private final String farmerId = "FARMER_123";
    private String currentTab = "Wanted Ads";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_listings);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        setupTabs();
        setupSearch();
        fetchSummary();
        selectTab("Wanted Ads"); // Default tab
    }

    private void initViews() {
        tvMonthlyRev = findViewById(R.id.tvMonthlyRev);
        tvTrustScore = findViewById(R.id.tvTrustScore);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvTabActive = findViewById(R.id.tvTabActive);
        tvTabHistory = findViewById(R.id.tvTabHistory);
        tvTabWantedAds = findViewById(R.id.tvTabWantedAds);
        indicatorActive = findViewById(R.id.indicatorActive);
        indicatorHistory = findViewById(R.id.indicatorHistory);
        indicatorWantedAds = findViewById(R.id.indicatorWantedAds);
        etSearch = findViewById(R.id.etSearch);
        rvListings = findViewById(R.id.rvWantedAds); // Reusing ID from layout

        rvListings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ListingAdapter(filteredItems, this::onListingAction);
        rvListings.setAdapter(adapter);

        findViewById(R.id.ivNotificationTop).setOnClickListener(view ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
    }

    private void setupTabs() {
        findViewById(R.id.tabActive).setOnClickListener(view -> selectTab("Active"));
        findViewById(R.id.tabHistory).setOnClickListener(view -> selectTab("History"));
        findViewById(R.id.tabWantedAds).setOnClickListener(view -> selectTab("Wanted Ads"));
    }

    private void selectTab(String tabName) {
        currentTab = tabName;
        // Reset styles
        resetTabStyles();

        if (tabName.equals("Active")) {
            tvTabActive.setTypeface(null, Typeface.BOLD);
            tvTabActive.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorActive.setVisibility(View.VISIBLE);
            fetchActiveListings();
        } else if (tabName.equals("History")) {
            tvTabHistory.setTypeface(null, Typeface.BOLD);
            tvTabHistory.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorHistory.setVisibility(View.VISIBLE);
            fetchHistory();
        } else {
            tvTabWantedAds.setTypeface(null, Typeface.BOLD);
            tvTabWantedAds.setTextColor(getResources().getColor(R.color.primary_green));
            indicatorWantedAds.setVisibility(View.VISIBLE);
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

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredList(s.toString());
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
        apiService.getWantedAds(null, null).enqueue(new Callback<List<com.harvesthub.mobile.model.WantedAd>>() {
            @Override
            public void onResponse(@NonNull Call<List<com.harvesthub.mobile.model.WantedAd>> call, @NonNull Response<List<com.harvesthub.mobile.model.WantedAd>> response) {
                allItems.clear();
                // Map to ListingItem or just add mock for demo
                addMockWantedAds();
                updateFilteredList(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<com.harvesthub.mobile.model.WantedAd>> call, @NonNull Throwable t) {
                addMockWantedAds();
            }
        });
    }

    private void fetchActiveListings() {
        apiService.getActiveListings(farmerId).enqueue(new Callback<List<ListingItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ListingItem>> call, @NonNull Response<List<ListingItem>> response) {
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allItems.addAll(response.body());
                } else {
                    addMockActiveListings();
                }
                updateFilteredList(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<ListingItem>> call, @NonNull Throwable t) {
                addMockActiveListings();
            }
        });
    }

    private void fetchHistory() {
        apiService.getListingHistory(farmerId).enqueue(new Callback<List<ListingItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ListingItem>> call, @NonNull Response<List<ListingItem>> response) {
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allItems.addAll(response.body());
                } else {
                    addMockHistory();
                }
                updateFilteredList(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<ListingItem>> call, @NonNull Throwable t) {
                addMockHistory();
            }
        });
    }

    private void addMockWantedAds() {
        allItems.add(new ListingItem("1", "Big Onions", "Dambulla Sector", "1,000 KG", "LKR 280/kg", "Wanted", true, "WANTED"));
        allItems.add(new ListingItem("2", "Tomatoes", "Colombo Sector", "500 KG", "LKR 150/kg", "Wanted", false, "WANTED"));
        allItems.add(new ListingItem("3", "Potatoes", "Nuwara Eliya Sector", "2,500 KG", "LKR 190/kg", "Wanted", false, "WANTED"));
        updateFilteredList(etSearch.getText().toString());
    }

    private void addMockActiveListings() {
        allItems.add(new ListingItem("101", "Red Onions", "Jaffna", "2,000 KG", "LKR 260/kg", "Active", false, "ACTIVE"));
        allItems.add(new ListingItem("102", "Carrots", "Nuwara Eliya", "800 KG", "LKR 180/kg", "Active", false, "ACTIVE"));
        updateFilteredList(etSearch.getText().toString());
    }

    private void addMockHistory() {
        allItems.add(new ListingItem("201", "Keeri Samba", "Ampara", "5,000 KG", "LKR 125/kg", "Completed", false, "HISTORY"));
        allItems.add(new ListingItem("202", "Green Chillies", "Puttalam", "300 KG", "LKR 450/kg", "Cancelled", false, "HISTORY"));
        updateFilteredList(etSearch.getText().toString());
    }

    private void updateFilteredList(String query) {
        filteredItems.clear();
        if (query.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String lowerQuery = query.toLowerCase();
            for (ListingItem item : allItems) {
                if (item.getCropName().toLowerCase().contains(lowerQuery) || 
                    item.getLocation().toLowerCase().contains(lowerQuery)) {
                    filteredItems.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredItems.isEmpty()) {
            tvEmptyState.setText("No " + currentTab.toLowerCase() + " found");
        }
    }

    private void onListingAction(ListingItem item) {
        if (item.getType().equals("WANTED")) {
            Intent intent = new Intent(this, CreateSupplyOfferActivity.class);
            intent.putExtra("requestId", item.getId());
            intent.putExtra("cropName", item.getCropName());
            intent.putExtra("requestedQuantity", 1000.0); // Mock parsing or fix model
            intent.putExtra("targetPrice", 280.0);
            intent.putExtra("location", item.getLocation());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Viewing " + item.getCropName() + " " + item.getType(), Toast.LENGTH_SHORT).show();
        }
    }
}
