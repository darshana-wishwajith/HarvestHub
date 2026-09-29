package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.SupplyOffer;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerOffersFragment extends Fragment {

    private static final String ARG_AD_TITLE = "arg_ad_title";

    private RecyclerView rvSupplyOffers;
    private TextView tvRespondingToAd;
    private TextView tvEmptyOffers;
    private ProgressBar progressBar;
    private String customAdTitle;

    public static BuyerOffersFragment newInstance(String adTitle) {
        BuyerOffersFragment fragment = new BuyerOffersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AD_TITLE, adTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_offers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvSupplyOffers = view.findViewById(R.id.rvSupplyOffers);
        rvSupplyOffers.setLayoutManager(new LinearLayoutManager(requireContext()));

        tvRespondingToAd = view.findViewById(R.id.tvRespondingToAd);
        tvEmptyOffers = view.findViewById(R.id.tvEmptyOffers);
        progressBar = view.findViewById(R.id.progressBarOffers);

        if (getArguments() != null && getArguments().containsKey(ARG_AD_TITLE)) {
            customAdTitle = getArguments().getString(ARG_AD_TITLE);
        }

        updateAdTitleHeader();
        loadOffersFromDatabase();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOffersFromDatabase();
    }

    public void setAdTitle(String adTitle) {
        this.customAdTitle = adTitle;
        updateAdTitleHeader();
    }

    private void updateAdTitleHeader() {
        if (tvRespondingToAd != null) {
            String title = (customAdTitle != null && !customAdTitle.isEmpty())
                    ? customAdTitle : "Responding to your active demand requests";
            tvRespondingToAd.setText(title);
        }
    }

    public void loadOffersFromDatabase() {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String buyerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                ? sessionManager.getUser().getId() : "user_buyer_keells";

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvEmptyOffers != null) tvEmptyOffers.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getOffersForBuyer(buyerId).enqueue(new Callback<List<SupplyOffer>>() {
            @Override
            public void onResponse(Call<List<SupplyOffer>> call, Response<List<SupplyOffer>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                List<SupplyOfferItem> offerList = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (SupplyOffer offer : response.body()) {
                        String grade = (offer.getGrade() != null && !offer.getGrade().isEmpty()) ? offer.getGrade() : "Grade A";
                        int badgeRes = "Grade B".equalsIgnoreCase(grade) ? R.drawable.bg_badge_grade_b : R.drawable.bg_badge_grade_a;
                        boolean hasCheck = "Grade A".equalsIgnoreCase(grade) || "ACCEPTED".equalsIgnoreCase(offer.getStatus());
                        String timeAgo = (offer.getCreatedDate() != null && !offer.getCreatedDate().isEmpty())
                                ? "Offer sent " + offer.getCreatedDate() : "Offer sent recently";
                        String farmer = (offer.getFarmerName() != null && !offer.getFarmerName().isEmpty())
                                ? "Farmer: " + offer.getFarmerName() : "Farmer: W.B.D.W. Bandara";
                        String loc = (offer.getLocation() != null && !offer.getLocation().isEmpty())
                                ? "Location: " + offer.getLocation() : "Location: Dambulla";
                        String qty = offer.getQuantity() > 0 ? "Estimated: " + ((int) offer.getQuantity()) + " KG" : "Estimated: 210 KG";
                        String price = offer.getPricePerKg() > 0 ? "Rs. " + ((int) offer.getPricePerKg()) + " / KG" : "Rs. 175 / KG";

                        offerList.add(new SupplyOfferItem(
                                offer.getId() != null ? offer.getId() : "1",
                                grade + " Verified",
                                badgeRes,
                                hasCheck,
                                timeAgo,
                                farmer,
                                loc,
                                qty,
                                price
                        ));
                    }
                }

                if (offerList.isEmpty()) {
                    // Fallback to sample items if no offers yet
                    offerList.add(new SupplyOfferItem("1", "Grade A Verified", R.drawable.bg_badge_grade_a, true,
                            "Offer sent 10m ago", "Farmer: W.B.D.W. Bandara", "Location: Dambulla", "Estimated: 210 KG", "Rs. 175 / KG"));
                    offerList.add(new SupplyOfferItem("2", "Grade B Standard", R.drawable.bg_badge_grade_b, false,
                            "Offer sent 1h ago", "Farmer: Sunil Perera", "Location: Kandy", "Estimated: 200 KG", "Rs. 160 / KG"));
                }

                if (tvEmptyOffers != null) {
                    tvEmptyOffers.setVisibility(offerList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                rvSupplyOffers.setAdapter(new SupplyOfferAdapter(offerList, selectedOffer -> {
                    Intent intentChat = new Intent(requireActivity(), NegotiationChatActivity.class);
                    intentChat.putExtra("crop_name", "200kg Tomatoes");
                    intentChat.putExtra("farmer_name", selectedOffer.getFarmerName().replace("Farmer: ", ""));
                    intentChat.putExtra("offered_price", selectedOffer.getOfferedPrice());
                    intentChat.putExtra("quantity", "200kg");
                    intentChat.putExtra("pickup_date", "Friday");
                    intentChat.putExtra("order_ref", "#HH-842");
                    startActivity(intentChat);
                }));
            }

            @Override
            public void onFailure(Call<List<SupplyOffer>> call, Throwable throwable) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                List<SupplyOfferItem> offerList = new ArrayList<>();
                offerList.add(new SupplyOfferItem("1", "Grade A Verified", R.drawable.bg_badge_grade_a, true,
                        "Offer sent 10m ago", "Farmer: W.B.D.W. Bandara", "Location: Dambulla", "Estimated: 210 KG", "Rs. 175 / KG"));
                offerList.add(new SupplyOfferItem("2", "Grade B Standard", R.drawable.bg_badge_grade_b, false,
                        "Offer sent 1h ago", "Farmer: Sunil Perera", "Location: Kandy", "Estimated: 200 KG", "Rs. 160 / KG"));

                rvSupplyOffers.setAdapter(new SupplyOfferAdapter(offerList, selectedOffer -> {
                    Intent intentChat = new Intent(requireActivity(), NegotiationChatActivity.class);
                    intentChat.putExtra("crop_name", "200kg Tomatoes");
                    intentChat.putExtra("farmer_name", selectedOffer.getFarmerName().replace("Farmer: ", ""));
                    intentChat.putExtra("offered_price", selectedOffer.getOfferedPrice());
                    intentChat.putExtra("quantity", "200kg");
                    intentChat.putExtra("pickup_date", "Friday");
                    intentChat.putExtra("order_ref", "#HH-842");
                    startActivity(intentChat);
                }));
            }
        });
    }
}
