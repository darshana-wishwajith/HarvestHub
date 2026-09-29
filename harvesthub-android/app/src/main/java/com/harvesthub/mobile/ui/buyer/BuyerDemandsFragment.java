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
            switch (action) {
                case "view_offers":
                    if (getActivity() instanceof BuyerDashboardActivity) {
                        ((BuyerDashboardActivity) getActivity()).selectTab(BuyerDashboardActivity.Tab.OFFERS,
                                "Responding to your ad: " + item.getTitle());
                    } else {
                        Intent intentOffers = new Intent(requireActivity(), SupplyOffersActivity.class);
                        intentOffers.putExtra("ad_title", "Responding to your ad: " + item.getTitle());
                        startActivity(intentOffers);
                    }
                    break;
                case "edit_ad":
                    Intent intentEdit = new Intent(requireActivity(), PostDemandRequestActivity.class);
                    startActivity(intentEdit);
                    break;
                case "republish":
                    Toast.makeText(requireContext(), "Republishing demand request for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
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
            tabActiveAds.setTypeface(Typeface.DEFAULT_BOLD);

            tabClosed.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_unselected));
            tabClosed.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tabClosed.setTypeface(Typeface.DEFAULT);
        } else {
            tabClosed.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_selected));
            tabClosed.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tabClosed.setTypeface(Typeface.DEFAULT_BOLD);

            tabActiveAds.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_unselected));
            tabActiveAds.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            tabActiveAds.setTypeface(Typeface.DEFAULT);
        }
        filterList();
    }

    public void loadDemandsFromDatabase() {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String buyerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                ? sessionManager.getUser().getId() : "user_buyer_keells";

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyDemands != null) tvEmptyDemands.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerWantedAds(buyerId).enqueue(new Callback<List<WantedAd>>() {
            @Override
            public void onResponse(Call<List<WantedAd>> call, Response<List<WantedAd>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                allDemandList.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (WantedAd ad : response.body()) {
                        DemandStatus status = "CLOSED".equalsIgnoreCase(ad.getStatus()) ? DemandStatus.CLOSED : DemandStatus.ACTIVE;
                        String timeAgo = (ad.getCreatedDate() != null && !ad.getCreatedDate().isEmpty())
                                ? "Posted " + ad.getCreatedDate() : "Active Ad";
                        String title = "Need " + ((int) ad.getRequiredQuantityKg()) + "kg of " + (ad.getCropName() != null ? ad.getCropName() : "Produce");
                        String deadline = (ad.getDeadline() != null && !ad.getDeadline().isEmpty())
                                ? "Deadline: " + ad.getDeadline() : "Target: Rs. " + ((int) ad.getTargetPricePerKg()) + "/KG";
                        String targetGrade = (ad.getTargetGrade() != null && !ad.getTargetGrade().isEmpty())
                                ? "Target: " + ad.getTargetGrade() : "Target: Grade A";

                        allDemandList.add(new DemandRequestItem(
                                ad.getRequestId(),
                                status,
                                timeAgo,
                                title,
                                deadline,
                                targetGrade
                        ));
                    }
                }

                if (allDemandList.isEmpty()) {
                    // Fallback to sample items if database is freshly cleared
                    allDemandList.add(new DemandRequestItem("1", DemandStatus.ACTIVE, "Posted 1d ago", "Need 200kg of Tomatoes", "Deadline: Friday", "Target: Grade A"));
                    allDemandList.add(new DemandRequestItem("2", DemandStatus.ACTIVE, "Posted 30 minutes ago", "Need 200kg of Tomatoes", "Deadline: Friday", "Target: Grade B"));
                    allDemandList.add(new DemandRequestItem("3", DemandStatus.CLOSED, "Fulfilled 4d ago", "500kg Big Onions", null, "Target: Grade B"));
                }

                filterList();
            }

            @Override
            public void onFailure(Call<List<WantedAd>> call, Throwable throwable) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (allDemandList.isEmpty()) {
                    allDemandList.add(new DemandRequestItem("1", DemandStatus.ACTIVE, "Posted 1d ago", "Need 200kg of Tomatoes", "Deadline: Friday", "Target: Grade A"));
                    allDemandList.add(new DemandRequestItem("2", DemandStatus.ACTIVE, "Posted 30 minutes ago", "Need 200kg of Tomatoes", "Deadline: Friday", "Target: Grade B"));
                    allDemandList.add(new DemandRequestItem("3", DemandStatus.CLOSED, "Fulfilled 4d ago", "500kg Big Onions", null, "Target: Grade B"));
                }
                filterList();
            }
        });
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
