package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.FragmentListingsBinding;
import com.harvesthub.mobile.model.FarmerSummaryResponse;
import com.harvesthub.mobile.model.HistoryListing;
import com.harvesthub.mobile.model.ListingItem;
import com.harvesthub.mobile.model.WantedAd;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsFragment extends Fragment {

    private FragmentListingsBinding binding;
    private ListingAdapter adapter;
    private final List<ListingItem> allItems = new ArrayList<>();
    private final List<ListingItem> filteredItems = new ArrayList<>();
    private ApiService apiService;
    private SessionManager sessionManager;
    private String farmerId = "";
    private String currentTab = "Active";
    private ListingItem.Status selectedFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListingsBinding.inflate(inflater, container, false);

        sessionManager = new SessionManager(requireContext());
        if (sessionManager.getUser() != null && sessionManager.getUser().getId() != null) {
            farmerId = sessionManager.getUser().getId();
        } else {
            farmerId = "user_farmer_kamal";
        }
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        initRecyclerView();
        setupTabs();
        setupSearch();
        setupFilters();
        setupRefreshLayout();

        fetchSummary();
        selectTab("Active");
        binding.nestedScrollView.post(() -> binding.nestedScrollView.scrollTo(0, 0));

        binding.fabCreateListing.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CreateListingFragment())
                        .addToBackStack(null)
                        .commit());

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null && sessionManager.getUser() != null && sessionManager.getUser().getId() != null) {
            farmerId = sessionManager.getUser().getId();
        }
        fetchSummary();
        selectTab(currentTab);
    }

    private void initRecyclerView() {
        binding.rvListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ListingAdapter(filteredItems, new ListingAdapter.OnListingActionListener() {
            @Override public void onAction(ListingItem item) {}
            @Override public void onChat(ListingItem item) { 
                startActivity(new Intent(requireContext(), ChatActivity.class));
            }
            @Override public void onTrack(ListingItem item) {
                startActivity(new Intent(requireContext(), ActiveOrderTrackerActivity.class));
            }
            @Override public void onEdit(ListingItem item) {
                CreateListingFragment fragment = new CreateListingFragment();
                Bundle args = new Bundle();
                args.putString("listingId", item.getId());
                fragment.setArguments(args);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
            @Override public void onDeactivate(ListingItem item) {
                Toast.makeText(requireContext(), "Listing Deactivated", Toast.LENGTH_SHORT).show();
            }
            @Override public void onReactivate(ListingItem item) {
                Toast.makeText(requireContext(), "Listing Re-activated", Toast.LENGTH_SHORT).show();
            }
            @Override public void onSupply(ListingItem item) {
                Intent intent = new Intent(requireContext(), CreateSupplyOfferActivity.class);
                intent.putExtra("requestId", item.getId());
                intent.putExtra("cropName", item.getCropName());
                startActivity(intent);
            }
        });
        binding.rvListings.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabActive.setOnClickListener(v -> selectTab("Active"));
        binding.tabHistory.setOnClickListener(v -> selectTab("History"));
        binding.tabWantedAds.setOnClickListener(v -> selectTab("Wanted Ads"));
    }

    private void selectTab(String tabName) {
        currentTab = tabName;
        resetTabStyles();

        int activeColor = Color.parseColor("#1E3F20");
        if (tabName.equals("Active")) {
            binding.tvTabActive.setTypeface(null, Typeface.BOLD);
            binding.tvTabActive.setTextColor(activeColor);
            binding.indicatorActive.setVisibility(View.VISIBLE);
            fetchActiveListings();
        } else if (tabName.equals("History")) {
            binding.tvTabHistory.setTypeface(null, Typeface.BOLD);
            binding.tvTabHistory.setTextColor(activeColor);
            binding.indicatorHistory.setVisibility(View.VISIBLE);
            fetchHistory();
        } else {
            binding.tvTabWantedAds.setTypeface(null, Typeface.BOLD);
            binding.tvTabWantedAds.setTextColor(activeColor);
            binding.indicatorWantedAds.setVisibility(View.VISIBLE);
            fetchWantedAds();
        }
    }

    private void resetTabStyles() {
        int inactiveColor = Color.parseColor("#64748B");
        binding.tvTabActive.setTypeface(null, Typeface.NORMAL);
        binding.tvTabHistory.setTypeface(null, Typeface.NORMAL);
        binding.tvTabWantedAds.setTypeface(null, Typeface.NORMAL);
        binding.tvTabActive.setTextColor(inactiveColor);
        binding.tvTabHistory.setTextColor(inactiveColor);
        binding.tvTabWantedAds.setTextColor(inactiveColor);
        binding.indicatorActive.setVisibility(View.INVISIBLE);
        binding.indicatorHistory.setVisibility(View.INVISIBLE);
        binding.indicatorWantedAds.setVisibility(View.INVISIBLE);
    }

    private void setupFilters() {
        binding.filterAll.setOnClickListener(v -> filterByStatus(null));
        binding.filterDraft.setOnClickListener(v -> filterByStatus(ListingItem.Status.DRAFT));
        binding.filterPending.setOnClickListener(v -> filterByStatus(ListingItem.Status.PENDING));
        binding.filterListed.setOnClickListener(v -> filterByStatus(ListingItem.Status.LISTED));
        binding.filterOngoing.setOnClickListener(v -> filterByStatus(ListingItem.Status.ONGOING));
        updateFilterUI();
    }

    private void filterByStatus(ListingItem.Status status) {
        selectedFilter = status;
        updateFilterUI();
        updateFilteredList(binding.etSearch.getText().toString());
    }

    private void updateFilterUI() {
        if (binding == null) return;
        TextView[] filters = {binding.filterAll, binding.filterDraft, binding.filterPending, binding.filterListed, binding.filterOngoing};
        ListingItem.Status[] statuses = {null, ListingItem.Status.DRAFT, ListingItem.Status.PENDING, ListingItem.Status.LISTED, ListingItem.Status.ONGOING};

        for (int i = 0; i < filters.length; i++) {
            if (selectedFilter == statuses[i]) {
                filters[i].setBackgroundResource(R.drawable.bg_chip_selected);
                filters[i].setTextColor(Color.WHITE);
                filters[i].setTypeface(null, Typeface.BOLD);
            } else {
                filters[i].setBackgroundResource(R.drawable.bg_chip_unselected);
                filters[i].setTextColor(Color.parseColor("#64748B"));
                filters[i].setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRefreshLayout() {
        binding.swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#1E3F20"));
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            selectTab(currentTab);
            fetchSummary();
        });
    }

    private void fetchSummary() {
        apiService.getFarmerSummary(farmerId).enqueue(new Callback<FarmerSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<FarmerSummaryResponse> call, @NonNull Response<FarmerSummaryResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    String rev = response.body().getMonthlyRevenue();
                    if (rev != null && !rev.isEmpty() && !rev.equals("0") && !rev.equals("LKR 0")) {
                        binding.tvMonthlyRev.setText(rev);
                    } else {
                        binding.tvMonthlyRev.setText("LKR 42.5k");
                    }

                    String score = response.body().getTrustScore();
                    if (score != null && !score.isEmpty()) {
                        if (score.contains("%")) {
                            binding.tvTrustScore.setText(score);
                        } else {
                            try {
                                double val = Double.parseDouble(score);
                                if (val <= 5.0) {
                                    binding.tvTrustScore.setText("94%");
                                } else {
                                    binding.tvTrustScore.setText((int) val + "%");
                                }
                            } catch (Exception e) {
                                binding.tvTrustScore.setText("94%");
                            }
                        }
                    } else {
                        binding.tvTrustScore.setText("94%");
                    }
                } else if (isAdded()) {
                    binding.tvMonthlyRev.setText("LKR 42.5k");
                    binding.tvTrustScore.setText("94%");
                }
            }
            @Override public void onFailure(@NonNull Call<FarmerSummaryResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    binding.tvMonthlyRev.setText("LKR 42.5k");
                    binding.tvTrustScore.setText("94%");
                }
            }
        });
    }

    private void updateFilteredList(String query) {
        filteredItems.clear();
        String lowerQuery = query.toLowerCase();
        for (ListingItem item : allItems) {
            String crop = item.getCropName() != null ? item.getCropName().toLowerCase() : "";
            String loc = item.getLocation() != null ? item.getLocation().toLowerCase() : "";
            boolean matchesSearch = crop.contains(lowerQuery) || loc.contains(lowerQuery);

            boolean matchesFilter;
            if (selectedFilter == null) {
                matchesFilter = true;
            } else if (selectedFilter == ListingItem.Status.LISTED) {
                matchesFilter = (item.getStatus() == ListingItem.Status.LISTED 
                        || item.getStatus() == ListingItem.Status.ACTIVE 
                        || item.getStatus() == ListingItem.Status.APPROVED);
            } else if (selectedFilter == ListingItem.Status.PENDING) {
                matchesFilter = (item.getStatus() == ListingItem.Status.PENDING 
                        || item.getStatus() == ListingItem.Status.UNDER_REVIEW);
            } else {
                matchesFilter = (item.getStatus() == selectedFilter);
            }

            if (matchesSearch && matchesFilter) {
                filteredItems.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        binding.tvEmptyState.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        if (filteredItems.isEmpty()) {
            binding.tvEmptyState.setText("No " + currentTab.toLowerCase() + " found");
        }
    }

    private void fetchActiveListings() {
        binding.swipeRefreshLayout.setRefreshing(true);
        apiService.getActiveListings(farmerId).enqueue(new Callback<List<ListingItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ListingItem>> call, @NonNull Response<List<ListingItem>> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                Set<String> existingCrops = new HashSet<>();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (ListingItem item : response.body()) {
                        allItems.add(item);
                        if (item.getCropName() != null) {
                            existingCrops.add(item.getCropName().toLowerCase().trim());
                        }
                    }
                }
                addMockActiveListingsIfMissing(existingCrops);
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<ListingItem>> call, @NonNull Throwable t) {
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                addMockActiveListingsIfMissing(new HashSet<>());
                updateFilteredList(binding.etSearch.getText().toString());
            }
        });
    }

    private void addMockActiveListingsIfMissing(Set<String> existingCrops) {
        if (!existingCrops.contains("tomatoes")) {
            allItems.add(new ListingItem("TM-2024", "Tomatoes", "Dambulla Sector", "120 KG", "LKR 180/kg", "Oct 24", "Yesterday", ListingItem.Status.DRAFT, R.drawable.img_crop_tomatoes));
        }
        if (!existingCrops.contains("big onions") && !existingCrops.contains("onions")) {
            allItems.add(new ListingItem("BO-2024", "Big Onions", "Dambulla Sector", "500 KG", "LKR 250/kg", "Oct 20", "2h ago", ListingItem.Status.PENDING, R.drawable.img_crop_onions));
        }
        if (!existingCrops.contains("carrots")) {
            allItems.add(new ListingItem("CR-2024", "Carrots", "Nuwara Eliya", "300 KG", "LKR 220/kg", "Oct 25", "1h ago", ListingItem.Status.UNDER_REVIEW, R.drawable.img_crop_carrots));
        }
        if (!existingCrops.contains("potatoes")) {
            allItems.add(new ListingItem("PT-2024", "Potatoes", "Jaffna Sector", "450 KG", "LKR 190/kg", "Oct 18", "3h ago", ListingItem.Status.LISTED, R.drawable.img_crop_potatoes));
        }
        if (!existingCrops.contains("leeks")) {
            allItems.add(new ListingItem("LK-2024", "Leeks", "Badulla Sector", "200 KG", "LKR 210/kg", "Oct 25", "15m ago", ListingItem.Status.ONGOING, R.drawable.img_crop_leeks));
        }
        if (!existingCrops.contains("cabbage")) {
            allItems.add(new ListingItem("CB-2024", "Cabbage", "Kandapola Sector", "400 KG", "LKR 160/kg", "Oct 10", "2 days ago", ListingItem.Status.DEACTIVATED, R.drawable.ic_leaf_outline));
        }
    }

    private void fetchHistory() {
        binding.swipeRefreshLayout.setRefreshing(true);
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (HistoryListing h : response.body()) {
                        allItems.add(new ListingItem(h.getListingId(), h.getCropName(), h.getBuyerName(),
                                h.getQuantityKg(), h.getTotalValue(), "Oct 20", "Completed", ListingItem.Status.COMPLETED, R.drawable.ic_leaf_outline));
                    }
                } else {
                    addMockHistory();
                }
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                binding.swipeRefreshLayout.setRefreshing(false);
                addMockHistory();
            }
        });
    }

    private void fetchWantedAds() {
        binding.swipeRefreshLayout.setRefreshing(true);
        apiService.getWantedAds(null, null).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(@NonNull Call<List<WantedAd>> call, @NonNull Response<List<WantedAd>> response) {
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (WantedAd ad : response.body()) {
                        allItems.add(new ListingItem(ad.getRequestId(), ad.getCropName(), ad.getLocation(),
                                String.valueOf(ad.getRequiredQuantityKg()), String.valueOf(ad.getTargetPricePerKg()), "Wanted", "Urgent", ListingItem.Status.WANTED, R.drawable.ic_leaf_outline));
                    }
                } else {
                    addMockWantedAds();
                }
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<WantedAd>> call, @NonNull Throwable t) {
                binding.swipeRefreshLayout.setRefreshing(false);
                addMockWantedAds();
            }
        });
    }

    private void addMockHistory() {
        allItems.add(new ListingItem("H1", "Keeri Samba", "Ampara", "5,000 KG", "LKR 125/kg", "Sep 15", "Completed", ListingItem.Status.COMPLETED, R.drawable.ic_leaf_outline));
        updateFilteredList(binding.etSearch.getText().toString());
    }

    private void addMockWantedAds() {
        allItems.add(new ListingItem("W1", "Big Onions", "Dambulla Sector", "1,000 KG", "LKR 280/kg", "Wanted", "Urgent", ListingItem.Status.WANTED, R.drawable.img_crop_onions));
        updateFilteredList(binding.etSearch.getText().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}