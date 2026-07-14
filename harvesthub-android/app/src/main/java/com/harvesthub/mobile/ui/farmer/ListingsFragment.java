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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerSummaryResponse;
import com.harvesthub.mobile.model.HistoryListing;
import com.harvesthub.mobile.model.ListingItem;
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
    private RecyclerView rvListings;
    private ListingAdapter adapter;
    private final List<ListingItem> allItems = new ArrayList<>();
    private final List<ListingItem> filteredItems = new ArrayList<>();
    private ApiService apiService;
    private final String farmerId = "FARMER_123";
    private String currentTab = "Active";
    private ListingItem.Status selectedFilter = null;

    private TextView filterAll, filterDraft, filterPending, filterListed, filterOngoing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_listings, container, false);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        setupTabs(view);
        setupSearch();
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
        etSearch = v.findViewById(R.id.etSearch);
        rvListings = v.findViewById(R.id.rvListings);

        filterAll = v.findViewById(R.id.filterAll);
        filterDraft = v.findViewById(R.id.filterDraft);
        filterPending = v.findViewById(R.id.filterPending);
        filterListed = v.findViewById(R.id.filterListed);
        filterOngoing = v.findViewById(R.id.filterOngoing);

        rvListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListingAdapter(filteredItems, new ListingAdapter.OnListingActionListener() {
            @Override public void onAction(ListingItem item) {}
            @Override public void onChat(ListingItem item) { startChat(item); }
            @Override public void onTrack(ListingItem item) { startTracking(item); }
            @Override public void onEdit(ListingItem item) { editListing(item); }
            @Override public void onSupply(ListingItem item) { supplyWantedAd(item); }
        });
        rvListings.setAdapter(adapter);

        v.findViewById(R.id.fabCreateListing).setOnClickListener(view -> {
            startActivity(new Intent(requireContext(), CreateListingActivity.class));
        });

        filterAll.setOnClickListener(view -> filterByStatus(null));
        filterDraft.setOnClickListener(view -> filterByStatus(ListingItem.Status.DRAFT));
        filterPending.setOnClickListener(view -> filterByStatus(ListingItem.Status.PENDING));
        filterListed.setOnClickListener(view -> filterByStatus(ListingItem.Status.LISTED));
        filterOngoing.setOnClickListener(view -> filterByStatus(ListingItem.Status.ONGOING));
    }

    private void setupTabs(View v) {
        v.findViewById(R.id.tabActive).setOnClickListener(view -> selectTab("Active"));
        v.findViewById(R.id.tabHistory).setOnClickListener(view -> selectTab("History"));
        v.findViewById(R.id.tabWantedAds).setOnClickListener(view -> selectTab("Wanted Ads"));
    }

    private void selectTab(String tabName) {
        currentTab = tabName;
        resetTabStyles();

        if (tabName.equals("Active")) {
            tvTabActive.setTypeface(null, Typeface.BOLD);
            tvTabActive.setTextColor(getResources().getColor(R.color.nav_active_green));
            indicatorActive.setVisibility(View.VISIBLE);
            fetchActiveListings();
        } else if (tabName.equals("History")) {
            tvTabHistory.setTypeface(null, Typeface.BOLD);
            tvTabHistory.setTextColor(getResources().getColor(R.color.nav_active_green));
            indicatorHistory.setVisibility(View.VISIBLE);
            fetchHistory();
        } else {
            tvTabWantedAds.setTypeface(null, Typeface.BOLD);
            tvTabWantedAds.setTextColor(getResources().getColor(R.color.nav_active_green));
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
        // Mocking for now because API might fail
        tvMonthlyRev.setText("LKR 42.5k");
        tvTrustScore.setText("94%");

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
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (WantedAd ad : response.body()) {
                        allItems.add(new ListingItem(ad.getRequestId(), ad.getCropName(), ad.getLocation(),
                                String.valueOf(ad.getRequiredQuantityKg()), String.valueOf(ad.getTargetPricePerKg()), "Wanted", "Urgent", ListingItem.Status.WANTED, R.drawable.ic_leaf));
                    }
                } else {
                    addMockWantedAds();
                }
                updateFilteredList(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<WantedAd>> call, @NonNull Throwable t) {
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
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (HistoryListing h : response.body()) {
                        allItems.add(new ListingItem(h.getListingId(), h.getCropName(), h.getBuyerName(),
                                h.getQuantityKg(), h.getTotalValue(), "Oct 20", "Completed", ListingItem.Status.COMPLETED, R.drawable.ic_leaf));
                    }
                } else {
                    addMockHistory();
                }
                updateFilteredList(etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                addMockHistory();
            }
        });
    }

    private void addMockWantedAds() {
        allItems.add(new ListingItem("W1", "Big Onions", "Dambulla Sector", "1,000 KG", "LKR 280/kg", "Wanted", "Urgent", ListingItem.Status.WANTED, R.drawable.ic_leaf));
        allItems.add(new ListingItem("W2", "Tomatoes", "Colombo Sector", "500 KG", "LKR 150/kg", "Wanted", "Normal", ListingItem.Status.WANTED, R.drawable.ic_leaf));
        allItems.add(new ListingItem("W3", "Potatoes", "Nuwara Eliya Sector", "2,500 KG", "LKR 190/kg", "Wanted", "Urgent", ListingItem.Status.WANTED, R.drawable.ic_leaf));
        updateFilteredList(etSearch.getText().toString());
    }

    private void addMockActiveListings() {
        allItems.add(new ListingItem("A1", "Red Onions", "Jaffna", "2,000 KG", "LKR 260/kg", "Oct 24", "Yesterday", ListingItem.Status.LISTED, R.drawable.ic_leaf));
        allItems.add(new ListingItem("A2", "Carrots", "Nuwara Eliya", "800 KG", "LKR 180/kg", "Oct 25", "1h ago", ListingItem.Status.ONGOING, R.drawable.ic_leaf));
        allItems.add(new ListingItem("A3", "Leeks", "Badulla", "400 KG", "LKR 210/kg", "Oct 22", "Draft", ListingItem.Status.DRAFT, R.drawable.ic_leaf));
        allItems.add(new ListingItem("A4", "Potatoes", "Jaffna", "600 KG", "LKR 240/kg", "Oct 21", "2 days ago", ListingItem.Status.PENDING, R.drawable.ic_leaf));
        updateFilteredList(etSearch.getText().toString());
    }

    private void addMockHistory() {
        allItems.add(new ListingItem("H1", "Keeri Samba", "Ampara", "5,000 KG", "LKR 125/kg", "Sep 15", "Completed", ListingItem.Status.COMPLETED, R.drawable.ic_leaf));
        allItems.add(new ListingItem("H2", "Green Chillies", "Puttalam", "300 KG", "LKR 450/kg", "Sep 10", "Cancelled", ListingItem.Status.CANCELLED, R.drawable.ic_leaf));
        updateFilteredList(etSearch.getText().toString());
    }

    private void updateFilteredList(String query) {
        filteredItems.clear();
        String lowerQuery = query.toLowerCase();
        for (ListingItem item : allItems) {
            boolean matchesSearch = item.getCropName().toLowerCase().contains(lowerQuery) ||
                                   item.getLocation().toLowerCase().contains(lowerQuery);
            boolean matchesFilter = (selectedFilter == null) || (item.getStatus() == selectedFilter);

            if (matchesSearch && matchesFilter) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredItems.isEmpty()) {
            tvEmptyState.setText("No " + currentTab.toLowerCase() + " found");
        }
    }

    private void filterByStatus(ListingItem.Status status) {
        selectedFilter = status;
        updateFilterUI();
        updateFilteredList(etSearch.getText().toString());
    }

    private void updateFilterUI() {
        resetFilterStyles();
        if (selectedFilter == null) {
            setActiveFilterStyle(filterAll);
        } else {
            switch (selectedFilter) {
                case DRAFT: setActiveFilterStyle(filterDraft); break;
                case PENDING: setActiveFilterStyle(filterPending); break;
                case LISTED: setActiveFilterStyle(filterListed); break;
                case ONGOING: setActiveFilterStyle(filterOngoing); break;
            }
        }
    }

    private void resetFilterStyles() {
        int grayText = getResources().getColor(R.color.text_gray_medium);
        filterAll.setBackgroundResource(R.drawable.bg_outlined_button_chat);
        filterAll.setBackgroundTintList(null);
        filterAll.setTextColor(grayText);
        
        filterDraft.setBackgroundResource(R.drawable.bg_outlined_button_chat);
        filterDraft.setBackgroundTintList(null);
        filterDraft.setTextColor(grayText);
        
        filterPending.setBackgroundResource(R.drawable.bg_outlined_button_chat);
        filterPending.setBackgroundTintList(null);
        filterPending.setTextColor(grayText);
        
        filterListed.setBackgroundResource(R.drawable.bg_outlined_button_chat);
        filterListed.setBackgroundTintList(null);
        filterListed.setTextColor(grayText);
        
        filterOngoing.setBackgroundResource(R.drawable.bg_outlined_button_chat);
        filterOngoing.setBackgroundTintList(null);
        filterOngoing.setTextColor(grayText);
    }

    private void setActiveFilterStyle(TextView view) {
        view.setBackgroundResource(R.drawable.bg_pill_badge);
        view.setBackgroundTintList(getResources().getColorStateList(R.color.nav_active_green));
        view.setTextColor(getResources().getColor(R.color.white));
    }

    private void onListingAction(ListingItem item) {
        // Handled via inner interface in adapter for specific buttons
    }

    // Inside adapter we will use these methods via interface or direct access if fragment is passed
    public void startChat(ListingItem item) {
        startActivity(new Intent(requireContext(), ChatActivity.class));
    }

    public void startTracking(ListingItem item) {
        startActivity(new Intent(requireContext(), ActiveOrderTrackerActivity.class));
    }

    public void editListing(ListingItem item) {
        Intent intent = new Intent(requireContext(), CreateListingActivity.class);
        intent.putExtra("listingId", item.getId());
        startActivity(intent);
    }

    public void supplyWantedAd(ListingItem item) {
        Intent intent = new Intent(requireContext(), CreateSupplyOfferActivity.class);
        intent.putExtra("requestId", item.getId());
        intent.putExtra("cropName", item.getCropName());
        startActivity(intent);
    }
}
