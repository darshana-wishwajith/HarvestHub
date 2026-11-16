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

/** Figma buyer 2.1 marketplace feed + 2.2 filters + "Purchase Request Form" (563:105). Real ACTIVE (officer-verified) lots. */
public class BuyerMarketplaceFragment extends Fragment implements FilterMarketplaceBottomSheet.Listener {

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
        chipBulk.setOnClickListener(v -> { // "Bulk Lots" = 1,000 kg or more
            currentGrade = "";
            filters.grades.clear();
            filters.minQty = 1000.0;
            highlightChip(chipBulk);
            loadMarketplaceListings(currentSearch, currentGrade);
        });

        ImageButton btnFilter = view.findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterMarketplaceBottomSheet filterBottomSheet = FilterMarketplaceBottomSheet.with(filters);
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
            filters.grades.clear();
            filters.minQty = null;
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
        String grades = grade != null && !grade.isEmpty() ? grade : FilterMarketplaceBottomSheet.Filters.join(filters.grades);
        apiService.getBuyerListings(search == null || search.isEmpty() ? null : search, grades,
                FilterMarketplaceBottomSheet.Filters.join(filters.categories),
                filters.district == null || filters.district.isEmpty() ? null : filters.district,
                filters.maxPrice, filters.minQty).enqueue(new Callback<List<ListingItem>>() {
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
                        BuyerMarketplaceFragment.this::showPurchaseForm));
                if (harvestList.isEmpty()) {
                    Toast.makeText(requireContext(), filters.isEmpty() && (currentSearch == null || currentSearch.isEmpty())
                            ? "No verified harvests are listed right now." : "No harvests match these filters.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ListingItem>> call, Throwable throwable) {
                if (!isAdded()) return;
                showLoadError();
            }
        });
    }

    private final FilterMarketplaceBottomSheet.Filters filters = new FilterMarketplaceBottomSheet.Filters();

    @Override
    public void onFiltersApplied(FilterMarketplaceBottomSheet.Filters applied) {
        filters.categories.clear();
        filters.categories.addAll(applied.categories);
        filters.grades.clear();
        filters.grades.addAll(applied.grades);
        filters.district = applied.district;
        filters.maxPrice = applied.maxPrice;
        filters.minQty = applied.minQty;
        currentGrade = "";
        highlightChip(chipAll);
        loadMarketplaceListings(currentSearch, currentGrade);
    }

    /**
     * Figma "Purchase Request Form": the buyer proposes quantity, rate and pickup (logistics) date. The order starts in
     * negotiation with the buyer's confirmation on these terms; the farmer accepts or counters in the chat.
     */
    private void showPurchaseForm(HarvestItem item) {
        if (!isAdded()) return;
        if (item.getListingId() == null) return;
        double available = parseNumber(item.getQuantity());
        double listed = parseNumber(item.getUnitPrice());
        android.content.Context ctx = requireContext();
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.LinearLayout box = new android.widget.LinearLayout(ctx);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView lot = new TextView(ctx);
        lot.setText(String.format(java.util.Locale.US, "%s • %s\nAvailable: about %,.0f kg • Listed at Rs. %,.0f/kg",
                item.getCropName(), item.getLocation() == null ? "" : item.getLocation(), available, listed));
        lot.setTextColor(0xFF475569);
        box.addView(lot);

        EditText qty = new EditText(ctx);
        qty.setHint("Quantity (kg)");
        qty.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        qty.setText(String.format(java.util.Locale.US, "%.0f", available));
        box.addView(qty);

        EditText rate = new EditText(ctx);
        rate.setHint("Offered rate (Rs. per kg)");
        rate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rate.setText(String.format(java.util.Locale.US, "%.0f", listed));
        box.addView(rate);

        TextView date = new TextView(ctx);
        date.setPadding(0, pad / 2, 0, pad / 2);
        date.setTextColor(0xFF1B4332);
        date.setTextSize(15f);
        final String[] pickup = {null};
        date.setText("Logistics window: choose pickup date ▾");
        date.setOnClickListener(v -> {
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.add(java.util.Calendar.DAY_OF_MONTH, 2);
            android.app.DatePickerDialog dp = new android.app.DatePickerDialog(ctx, (view, y, m, d) -> {
                pickup[0] = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                date.setText("Pickup date: " + pickup[0]);
            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dp.show();
        });
        box.addView(date);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Initiate Purchase")
                .setMessage("Propose your terms to " + (item.getFarmerName() == null ? "the farmer" : item.getFarmerName()))
                .setView(box)
                .setPositiveButton("Send Request", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            double q = parseNumber(qty.getText().toString());
            double p = parseNumber(rate.getText().toString());
            if (q <= 0) { qty.setError("Enter the quantity"); return; }
            if (available > 0 && q > available * 1.05) { qty.setError("Only about " + (int) available + " kg is available"); return; }
            if (p <= 0) { rate.setError("Enter your rate"); return; }
            if (pickup[0] == null) { Toast.makeText(ctx, "Choose a pickup date.", Toast.LENGTH_SHORT).show(); return; }
            dialog.dismiss();
            sendPurchaseRequest(item, q, p, pickup[0]);
        }));
        dialog.show();
    }

    private void sendPurchaseRequest(HarvestItem item, double qty, double price, String pickupDate) {
        Order order = new Order();
        order.setListingId(item.getListingId());
        order.setQuantity(qty);
        order.setPricePerKg(price);
        ApiService api = ApiClient.getApiService();
        api.createOrder(order).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(response,
                            "Unable to create the purchase request."), Toast.LENGTH_LONG).show();
                    return;
                }
                String orderId = response.body().getId();
                // the buyer's proposal = buyer-confirmed terms incl. the pickup date; the farmer accepts or counters
                api.confirmDeal(orderId, new com.harvesthub.mobile.model.ConfirmDealRequest(qty, price, pickupDate))
                        .enqueue(new Callback<com.harvesthub.mobile.model.GenericResponse>() {
                            @Override
                            public void onResponse(Call<com.harvesthub.mobile.model.GenericResponse> c,
                                                   Response<com.harvesthub.mobile.model.GenericResponse> r) {
                                openChat(orderId);
                            }

                            @Override
                            public void onFailure(Call<com.harvesthub.mobile.model.GenericResponse> c, Throwable t) {
                                openChat(orderId);
                            }
                        });
            }

            @Override
            public void onFailure(Call<Order> call, Throwable throwable) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openChat(String orderId) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), "Purchase request sent. The farmer will reply in the chat.", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(requireActivity(), NegotiationChatActivity.class);
        intent.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, orderId);
        startActivity(intent);
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
