package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import com.harvesthub.mobile.model.WantedAd;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerDemandsFragment extends Fragment {

    private RecyclerView rvDemandRequests;
    private DemandRequestAdapter adapter;
    private TextView tabActiveAds;
    private TextView tabClosed;
    private TextView tvEmptyDemands;
    private ProgressBar progressBar;

    private List<DemandRequestItem> allDemandList = new ArrayList<>();
    private boolean isShowingActive = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_demands, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabActiveAds = view.findViewById(R.id.tabActiveAds);
        tabClosed = view.findViewById(R.id.tabClosed);
        rvDemandRequests = view.findViewById(R.id.rvDemandRequests);
        tvEmptyDemands = view.findViewById(R.id.tvEmptyDemands);
        progressBar = view.findViewById(R.id.progressBarDemands);

        setupRecyclerView();
        setupTabs();

        ExtendedFloatingActionButton fabCreateDemand = view.findViewById(R.id.fabCreateDemand);
        if (fabCreateDemand != null) {
            fabCreateDemand.setOnClickListener(v -> {
                Intent intent = new Intent(requireActivity(), PostDemandRequestActivity.class);
                startActivity(intent);
            });
        }

        loadDemandsFromDatabase();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDemandsFromDatabase();
    }

    private void setupRecyclerView() {
        rvDemandRequests.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DemandRequestAdapter(new ArrayList<>(), (item, action) -> {
            WantedAd ad = ads.get(item.getId());
            if (ad == null) return;
            switch (action) {
                case "view_offers":
                    if (getActivity() instanceof BuyerDashboardActivity) {
                        ((BuyerDashboardActivity) getActivity()).showOffersFor(
                                ad.getCropName() + " (" + (int) ad.getRequiredQuantityKg() + " kg)", ad.getRequestId());
                    }
                    break;
                case "edit_ad":
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(item.getTitle())
                            .setItems(new String[]{"Edit request", "Close request"}, (d, which) -> {
                                if (which == 0) {
                                    Intent intentEdit = new Intent(requireActivity(), PostDemandRequestActivity.class);
                                    intentEdit.putExtra(PostDemandRequestActivity.EXTRA_EDIT_AD, new com.google.gson.Gson().toJson(ad));
                                    startActivity(intentEdit);
                                } else {
                                    closeAd(ad);
                                }
                            }).show();
                    break;
                case "republish":
                    republish(ad);
                    break;
            }
        });

        rvDemandRequests.setAdapter(adapter);
    }

    private void setupTabs() {
        tabActiveAds.setOnClickListener(v -> selectTab(true));
        tabClosed.setOnClickListener(v -> selectTab(false));
    }

    private void selectTab(boolean isActiveTab) {
        isShowingActive = isActiveTab;
        if (isActiveTab) {
            tabActiveAds.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selected));
            tabActiveAds.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabActiveAds.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(tabActiveAds.getContext()));

            tabClosed.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_unselected));
            tabClosed.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tabClosed.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.regular(tabClosed.getContext()));
        } else {
            tabClosed.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selected));
            tabClosed.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabClosed.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(tabClosed.getContext()));

            tabActiveAds.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_unselected));
            tabActiveAds.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tabActiveAds.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.regular(tabActiveAds.getContext()));
        }
        filterList();
    }

    public void loadDemandsFromDatabase() {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String buyerId = sessionManager.getUserId();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyDemands != null) tvEmptyDemands.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerWantedAds(buyerId).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(Call<List<WantedAd>> call, Response<List<WantedAd>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                allDemandList.clear();
                ads.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (WantedAd ad : response.body()) {
                        ads.put(ad.getRequestId(), ad);
                        DemandStatus status = ad.isOpen() ? DemandStatus.ACTIVE : DemandStatus.CLOSED;
                        String when = ad.isOpen()
                                ? "Posted " + BaseBuyerOrderActivity.time(ad.getCreatedDate())
                                : closedLabel(ad);
                        String title = String.format(java.util.Locale.US, "Need %,.0f kg of %s", ad.getRequiredQuantityKg(),
                                ad.getCropName() == null ? "produce" : ad.getCropName());
                        String deadline = ad.getDeadline() == null || ad.getDeadline().isEmpty() ? null : "Deadline: " + ad.getDeadline();
                        String grade = "Target: " + (ad.getTargetGrade() == null || ad.getTargetGrade().isEmpty() ? "Any grade"
                                : "Grade " + ad.getTargetGrade().replace("Grade ", ""))
                                + (ad.getTargetPricePerKg() > 0 ? String.format(java.util.Locale.US, " • Rs. %,.0f/kg", ad.getTargetPricePerKg()) : "");
                        allDemandList.add(new DemandRequestItem(ad.getRequestId(), status, when, title, deadline, grade)
                                .offers(ad.getOfferCount(), ad.getPendingOfferCount()));
                    }
                }
                long open = 0;
                for (DemandRequestItem i : allDemandList) if (i.getStatus() == DemandStatus.ACTIVE) open++;
                tabActiveAds.setText("Active Ads (" + open + ")");
                if (tvEmptyDemands != null && !response.isSuccessful()) tvEmptyDemands.setText("Could not load your wanted ads.");
                filterList();
            }

            @Override
            public void onFailure(Call<List<WantedAd>> call, Throwable throwable) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (tvEmptyDemands != null) tvEmptyDemands.setText("Can't reach the server.");
                filterList();
            }
        });
    }

    private final java.util.Map<String, WantedAd> ads = new java.util.HashMap<>();

    private static String closedLabel(WantedAd ad) {
        String why = "OFFER_ACCEPTED".equals(ad.getClosedReason()) ? "Offer accepted"
                : "DEADLINE".equals(ad.getClosedReason()) ? "Deadline passed" : "Closed";
        return why + (ad.getClosedDate() == null ? "" : " " + BaseBuyerOrderActivity.time(ad.getClosedDate()));
    }

    private void closeAd(WantedAd ad) {
        ApiClient.getApiService().closeWantedAd(ad.getRequestId()).enqueue(new Callback<WantedAd>() {
            @Override
            public void onResponse(Call<WantedAd> call, Response<WantedAd> r) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), r.isSuccessful() ? "Request closed."
                        : com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(r, "Could not close it."), Toast.LENGTH_SHORT).show();
                loadDemandsFromDatabase();
            }

            @Override
            public void onFailure(Call<WantedAd> call, Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Republish = new open copy with a new deadline (decision 15 Nov). */
    private void republish(WantedAd ad) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.add(java.util.Calendar.DAY_OF_MONTH, 7);
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(requireContext(), (view, y, m, d) -> {
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("deadline", String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, d));
            ApiClient.getApiService().republishWantedAd(ad.getRequestId(), body).enqueue(new Callback<WantedAd>() {
                @Override
                public void onResponse(Call<WantedAd> call, Response<WantedAd> r) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), r.isSuccessful() ? "Request published again."
                            : com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(r, "Could not republish."), Toast.LENGTH_SHORT).show();
                    if (r.isSuccessful()) selectTab(true);
                    loadDemandsFromDatabase();
                }

                @Override
                public void onFailure(Call<WantedAd> call, Throwable t) {
                    if (isAdded()) Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                }
            });
        }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH));
        dp.setTitle("New deadline");
        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dp.show();
    }

    private void filterList() {
        if (!isAdded() || adapter == null) return;
        List<DemandRequestItem> filtered = new ArrayList<>();
        DemandStatus targetStatus = isShowingActive ? DemandStatus.ACTIVE : DemandStatus.CLOSED;

        for (DemandRequestItem item : allDemandList) {
            if (item.getStatus() == targetStatus) {
                filtered.add(item);
            }
        }

        adapter.updateList(filtered);
        if (tvEmptyDemands != null) {
            tvEmptyDemands.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
