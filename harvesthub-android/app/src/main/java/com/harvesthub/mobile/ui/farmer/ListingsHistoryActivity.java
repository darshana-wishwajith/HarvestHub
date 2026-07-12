package com.harvesthub.mobile.ui.farmer;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.HistoryListing;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsHistoryActivity extends AppCompatActivity {

    private TextView tvMonthlyRev, tvTrustScore, tvEmptyState;
    private TextView tvTabActive, tvTabHistory, tvTabWantedAds;
    private View indicatorActive, indicatorHistory, indicatorWantedAds;
    private View notificationDot;
    private EditText etSearch;
    private RecyclerView rvListings;
    
    private HistoryListingAdapter adapter;
    private final List<HistoryListing> allItems = new ArrayList<>();
    private final List<HistoryListing> filteredItems = new ArrayList<>();
    
    private ApiService apiService;
    private final String farmerId = "FARMER_123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_listings);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        setupSearch();
        setupTabs();
        
        // Mocking unread notifications
        setNotificationDot(true);
        
        fetchHistory();
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
        notificationDot = findViewById(R.id.notificationDot);
        etSearch = findViewById(R.id.etSearch);
        rvListings = findViewById(R.id.rvListings);

        rvListings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryListingAdapter(filteredItems);
        rvListings.setAdapter(adapter);
    }

    private void setNotificationDot(boolean hasUnread) {
        notificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    private void setupTabs() {
        // Since we are in History screen, History is default.
        // Clicking others would normally navigate elsewhere.
        findViewById(R.id.tabActive).setOnClickListener(v -> finish());
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchHistory() {
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    allItems.addAll(response.body());
                } else {
                    addMockData();
                }
                filter(etSearch.getText().toString());
            }

            @Override
            public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                addMockData();
                filter(etSearch.getText().toString());
            }
        });
    }

    private void addMockData() {
        allItems.add(new HistoryListing("1", "Big Onions", "DELIVERED", "Oct 12, 2023", "", "850", "LKR 238,000", ""));
        allItems.add(new HistoryListing("2", "Potatoes", "CLOSED", "Oct 08, 2023", "GreenMart Ltd", "", "", "LKR 185/kg"));
        allItems.add(new HistoryListing("3", "Carrots", "DELIVERED", "Sep 28, 2023", "", "1,200", "LKR 156,000", ""));
    }

    private void filter(String query) {
        filteredItems.clear();
        String lowerQuery = query.toLowerCase();
        for (HistoryListing item : allItems) {
            if (item.getCropName().toLowerCase().contains(lowerQuery) || 
                item.getBuyerName().toLowerCase().contains(lowerQuery)) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
