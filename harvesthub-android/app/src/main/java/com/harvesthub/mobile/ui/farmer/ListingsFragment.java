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
        farmerId = sessionManager.getUserId();
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
            @Override public void onAction(ListingItem item) {
                // Action Required → retake: back to the farm, location lock + guided camera (workflow 1.5)
                Intent intent = new Intent(requireContext(), LocationVerificationActivity.class);
                intent.putExtra(LocationVerificationActivity.EXTRA_RETAKE_LISTING_ID, item.getId());
                intent.putExtra(LocationVerificationActivity.EXTRA_CROP, item.getCropName());
                startActivity(intent);
            }
            @Override public void onChat(ListingItem item) {
                OrderFlowRouter.open(requireContext(), item.getActiveOrderId(), false);
            }
            @Override public void onTrack(ListingItem item) {
                if (item.getActiveOrderId() == null) {
                    Toast.makeText(requireContext(), "No order is linked to this listing.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(requireContext(), ActiveOrderTrackerActivity.class);
                intent.putExtra(OrderFlowRouter.EXTRA_ORDER_ID, item.getActiveOrderId());
                startActivity(intent);
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
            @Override public void onDeactivate(ListingItem item) {}
            @Override public void onReactivate(ListingItem item) {}
            @Override public void onSupply(ListingItem item) {
                Intent intent = new Intent(requireContext(), CreateSupplyOfferActivity.class);
                intent.putExtra("requestId", item.getId());
                intent.putExtra("cropName", item.getCropName());
                intent.putExtra("location", item.getLocation());
                intent.putExtra("requestedQuantity", number(item.getQuantity()));
                intent.putExtra("targetPrice", number(item.getPrice()));
                startActivity(intent);
            }
        });
        binding.rvListings.setAdapter(adapter);
    }

    private static double number(String s) {
        if (s == null) return 0;
        String d = s.replaceAll("[^0-9.]", "");
        try {
            return d.isEmpty() ? 0 : Double.parseDouble(d);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        binding.filterUnderReview.setOnClickListener(v -> filterByStatus(ListingItem.Status.UNDER_REVIEW));
        binding.filterListed.setOnClickListener(v -> filterByStatus(ListingItem.Status.LISTED));
        binding.filterActionRequired.setOnClickListener(v -> filterByStatus(ListingItem.Status.RETAKE_REQUESTED));
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
        TextView[] filters = {binding.filterAll, binding.filterDraft, binding.filterPending, binding.filterUnderReview,
                binding.filterListed, binding.filterActionRequired, binding.filterOngoing};
        ListingItem.Status[] statuses = {null, ListingItem.Status.DRAFT, ListingItem.Status.PENDING, ListingItem.Status.UNDER_REVIEW,
                ListingItem.Status.LISTED, ListingItem.Status.RETAKE_REQUESTED, ListingItem.Status.ONGOING};

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
                if (!isAdded() || binding == null) return;
                FarmerSummaryResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    binding.tvMonthlyRev.setText(body.getMonthlyRevenue() != null ? body.getMonthlyRevenue() : "LKR 0");
                    String trust = body.getTrustPercent() + "%";
                    if (body.getRatingAverage() != null) {
                        trust += String.format(java.util.Locale.US, " · %.1f★", body.getRatingAverage());
                    }
                    binding.tvTrustScore.setText(trust);
                } else {
                    binding.tvMonthlyRev.setText("—");
                    binding.tvTrustScore.setText("—");
                }
            }
            @Override public void onFailure(@NonNull Call<FarmerSummaryResponse> call, @NonNull Throwable t) {
                if (isAdded() && binding != null) {
                    binding.tvMonthlyRev.setText("—");
                    binding.tvTrustScore.setText("—");
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
            } else if (selectedFilter == ListingItem.Status.RETAKE_REQUESTED) { // "Action Required"
                matchesFilter = (item.getStatus() == ListingItem.Status.RETAKE_REQUESTED
                        || item.getStatus() == ListingItem.Status.REJECTED);
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
                if (binding == null) return;
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) allItems.addAll(response.body());
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<ListingItem>> call, @NonNull Throwable t) {
                showLoadError();
            }
        });
    }

    private void fetchHistory() {
        binding.swipeRefreshLayout.setRefreshing(true);
        apiService.getListingHistory(farmerId, null).enqueue(new Callback<List<HistoryListing>>() {
            @Override
            public void onResponse(@NonNull Call<List<HistoryListing>> call, @NonNull Response<List<HistoryListing>> response) {
                if (binding == null) return;
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (HistoryListing h : response.body()) {
                        String value = h.getTotalValue() != null ? "LKR " + h.getTotalValue()
                                : (h.getFinalPricePerKg() != null ? "LKR " + h.getFinalPricePerKg() + "/kg" : "—");
                        allItems.add(new ListingItem(h.getListingId(), h.getCropName(),
                                h.getBuyerName() != null ? "Buyer: " + h.getBuyerName() : "",
                                h.getQuantityKg(), value, h.getDate(), h.getDate(), ListingItem.Status.COMPLETED, 0));
                    }
                }
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<HistoryListing>> call, @NonNull Throwable t) {
                showLoadError();
            }
        });
    }

    private void fetchWantedAds() {
        binding.swipeRefreshLayout.setRefreshing(true);
        apiService.getWantedAds(null, null).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(@NonNull Call<List<WantedAd>> call, @NonNull Response<List<WantedAd>> response) {
                if (binding == null) return;
                binding.swipeRefreshLayout.setRefreshing(false);
                allItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (WantedAd ad : response.body()) {
                        ListingItem item = new ListingItem(ad.getRequestId(), ad.getCropName(), ad.getLocation(),
                                String.format(java.util.Locale.US, "%,.0f KG", ad.getRequiredQuantityKg()),
                                String.format(java.util.Locale.US, "LKR %,.0f/kg", ad.getTargetPricePerKg()),
                                ad.getCreatedDate(), ad.getCreatedDate(), ListingItem.Status.WANTED, 0);
                        item.setUrgent(ad.isUrgent());
                        allItems.add(item);
                    }
                }
                updateFilteredList(binding.etSearch.getText().toString());
            }
            @Override public void onFailure(@NonNull Call<List<WantedAd>> call, @NonNull Throwable t) {
                showLoadError();
            }
        });
    }

    private void showLoadError() {
        if (binding == null) return;
        binding.swipeRefreshLayout.setRefreshing(false);
        allItems.clear();
        updateFilteredList(binding.etSearch.getText().toString());
        binding.tvEmptyState.setText("Can't reach the server. Pull down to retry.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}