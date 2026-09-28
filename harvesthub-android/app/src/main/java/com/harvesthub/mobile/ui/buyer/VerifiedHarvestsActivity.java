package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ListingItem;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;
import com.harvesthub.mobile.utils.BuyerNavigationHelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifiedHarvestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verified_harvests);

        RecyclerView rvHarvestList = findViewById(R.id.rvHarvestList);
        rvHarvestList.setLayoutManager(new LinearLayoutManager(this));

        View buyerTopNav = findViewById(R.id.buyerTopNav);
        View buyerBottomNav = findViewById(R.id.buyerBottomNav);
        BuyerNavigationHelper.setup(this, buyerBottomNav, BuyerNavigationHelper.Tab.HOME);
        BuyerNavigationHelper.setupNotifications(this, buyerTopNav);

        loadMarketplaceListings(rvHarvestList, "", "");

        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadMarketplaceListings(rvHarvestList, s.toString(), "");
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        wireGradeChip(findViewById(R.id.chipAll), rvHarvestList, "");
        wireGradeChip(findViewById(R.id.chipGradeA), rvHarvestList, "A");
        wireGradeChip(findViewById(R.id.chipGradeB), rvHarvestList, "B");
        wireGradeChip(findViewById(R.id.chipGradeC), rvHarvestList, "C");

        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterMarketplaceBottomSheet filterBottomSheet = new FilterMarketplaceBottomSheet();
            filterBottomSheet.show(getSupportFragmentManager(), FilterMarketplaceBottomSheet.TAG);
        });

        ExtendedFloatingActionButton fabPostDemand = findViewById(R.id.fabPostDemand);
        if (fabPostDemand != null) {
            fabPostDemand.setOnClickListener(v -> {
                Intent intent = new Intent(this, PostDemandRequestActivity.class);
                startActivity(intent);
            });
        }
    }

    private void loadMarketplaceListings(RecyclerView recyclerView, String search, String grade) {
        SessionManager sessionManager = new SessionManager(this);
        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerListings(search, grade).enqueue(new Callback<List<ListingItem>>() {
            @Override
            public void onResponse(Call<List<ListingItem>> call, Response<List<ListingItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showLoadError();
                    return;
                }

                List<HarvestItem> harvestList = new ArrayList<>();
                for (ListingItem listing : response.body()) {
                    harvestList.add(HarvestItem.fromListing(listing));
                }
                recyclerView.setAdapter(new HarvestAdapter(harvestList,
                    VerifiedHarvestsActivity.this::createPurchaseRequest));
            }

            @Override
            public void onFailure(Call<List<ListingItem>> call, Throwable throwable) {
                showLoadError();
            }
        });
    }

    private void wireGradeChip(TextView chip, RecyclerView recyclerView, String grade) {
        chip.setOnClickListener(v -> loadMarketplaceListings(recyclerView, "", grade));
    }

    private void createPurchaseRequest(HarvestItem item) {
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUser() == null || item.getListingId() == null) {
            Toast.makeText(this, "Please sign in again before requesting a purchase.", Toast.LENGTH_LONG).show();
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
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(VerifiedHarvestsActivity.this,
                                    "Unable to create the purchase request.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Order created = response.body();
                        Intent intent = new Intent(VerifiedHarvestsActivity.this, NegotiationChatActivity.class);
                        intent.putExtra("order_id", created.getId());
                        intent.putExtra("crop_name", created.getCropName());
                        intent.putExtra("farmer_name", created.getFarmerName());
                        intent.putExtra("quantity", String.valueOf(created.getQuantity()));
                        intent.putExtra("offered_price", "Rs. " + created.getPricePerKg() + "/kg");
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(Call<Order> call, Throwable throwable) {
                        Toast.makeText(VerifiedHarvestsActivity.this,
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
        Toast.makeText(this, "Unable to load available harvests.", Toast.LENGTH_LONG).show();
    }
}

