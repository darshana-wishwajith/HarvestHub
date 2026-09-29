package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ListingItem;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerMarketplaceFragment extends Fragment {

    private RecyclerView rvHarvestList;
    private EditText etSearch;
    private TextView chipAll, chipGradeA, chipGradeB, chipGradeC, chipBulk;
    private String currentGrade = "";
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvHarvestList = view.findViewById(R.id.rvHarvestList);
        rvHarvestList.setLayoutManager(new LinearLayoutManager(requireContext()));

        etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString();
                loadMarketplaceListings(currentSearch, currentGrade);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        chipAll = view.findViewById(R.id.chipAll);
        chipGradeA = view.findViewById(R.id.chipGradeA);
        chipGradeB = view.findViewById(R.id.chipGradeB);
        chipGradeC = view.findViewById(R.id.chipGradeC);
        chipBulk = view.findViewById(R.id.chipBulk);

        wireGradeChip(chipAll, "");
        wireGradeChip(chipGradeA, "A");
        wireGradeChip(chipGradeB, "B");
        wireGradeChip(chipGradeC, "C");
        wireGradeChip(chipBulk, "");

        ImageButton btnFilter = view.findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterMarketplaceBottomSheet filterBottomSheet = new FilterMarketplaceBottomSheet();
            filterBottomSheet.show(getChildFragmentManager(), FilterMarketplaceBottomSheet.TAG);
        });

        ExtendedFloatingActionButton fabPostDemand = view.findViewById(R.id.fabPostDemand);
        if (fabPostDemand != null) {
            fabPostDemand.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), PostDemandRequestActivity.class);
                startActivity(intent);
            });
        }

        loadMarketplaceListings("", "");
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMarketplaceListings(currentSearch, currentGrade);
    }

    private void wireGradeChip(TextView chip, String grade) {
        chip.setOnClickListener(v -> {
            currentGrade = grade;
            highlightChip(chip);
            loadMarketplaceListings(currentSearch, currentGrade);
        });
    }

    private void highlightChip(TextView selected) {
        TextView[] chips = {chipAll, chipGradeA, chipGradeB, chipGradeC, chipBulk};
        for (TextView c : chips) {
            if (c == null) continue;
            boolean isSelected = (c == selected);
            c.setBackground(ContextCompat.getDrawable(requireContext(),
                    isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
            c.setTextColor(ContextCompat.getColor(requireContext(),
                    isSelected ? R.color.white : R.color.text_primary));
        }
    }

    public void loadMarketplaceListings(String search, String grade) {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerListings(search, grade).enqueue(new Callback<List<ListingItem>>() {
            @Override
            public void onResponse(Call<List<ListingItem>> call, Response<List<ListingItem>> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showLoadError();
                    return;
                }

                List<HarvestItem> harvestList = new ArrayList<>();
                for (ListingItem listing : response.body()) {
                    harvestList.add(HarvestItem.fromListing(listing));
                }
                rvHarvestList.setAdapter(new HarvestAdapter(harvestList,
                        BuyerMarketplaceFragment.this::createPurchaseRequest));
            }

            @Override
            public void onFailure(Call<List<ListingItem>> call, Throwable throwable) {
                if (!isAdded()) return;
                showLoadError();
            }
        });
    }

    private void createPurchaseRequest(HarvestItem item) {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        if (sessionManager.getUser() == null || item.getListingId() == null) {
            Toast.makeText(requireContext(), "Please sign in again before requesting a purchase.", Toast.LENGTH_LONG).show();
            return;
        }

        Order order = new Order();
        order.setBuyerId(sessionManager.getUser().getId());
        order.setFarmerId(item.getFarmerId());
        order.setListingId(item.getListingId());
        order.setCropName(item.getCropName());
        order.setFarmerName(item.getFarmerName());
        order.setQuantity(parseNumber(item.getQuantity()));
        order.setPricePerKg(parseNumber(item.getUnitPrice()));

        ApiClient.getClient(sessionManager).create(ApiService.class).createOrder(order)
                .enqueue(new Callback<Order>() {
                    @Override
                    public void onResponse(Call<Order> call, Response<Order> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    "Unable to create the purchase request.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Order created = response.body();
                        Intent intent = new Intent(requireActivity(), NegotiationChatActivity.class);
                        intent.putExtra("order_id", created.getId());
                        intent.putExtra("crop_name", created.getCropName());
                        intent.putExtra("farmer_name", created.getFarmerName());
                        intent.putExtra("quantity", String.valueOf(created.getQuantity()));
                        intent.putExtra("offered_price", "Rs. " + created.getPricePerKg() + "/kg");
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<Order> call, Throwable throwable) {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(),
                                "Network error while creating the purchase request.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private double parseNumber(String value) {
        if (value == null) return 0;
        String normalized = value.replaceAll("[^0-9.]", "");
        try {
            return normalized.isEmpty() ? 0 : Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showLoadError() {
        if (isAdded()) {
            Toast.makeText(requireContext(), "Unable to load available harvests.", Toast.LENGTH_LONG).show();
        }
    }
}
