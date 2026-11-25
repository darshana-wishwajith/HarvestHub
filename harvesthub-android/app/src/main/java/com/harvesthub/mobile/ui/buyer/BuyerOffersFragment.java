package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.SupplyOffer;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Received Supply Offers" screen. Real offers from farmers on the buyer's wanted ads (optionally one ad).
 * Accepting an offer creates the order in negotiation and closes the ad, then opens the chat.
 */
public class BuyerOffersFragment extends Fragment {

    private static final String ARG_AD_TITLE = "arg_ad_title";
    private static final String ARG_AD_ID = "arg_ad_id";

    private RecyclerView rvSupplyOffers;
    private TextView tvRespondingToAd, tvEmptyOffers;
    private ProgressBar progressBar;
    private String adTitle, adId;
    private final Map<String, SupplyOffer> byId = new HashMap<>();

    public static BuyerOffersFragment newInstance(String adTitle) {
        return newInstance(adTitle, null);
    }

    public static BuyerOffersFragment newInstance(String adTitle, String adId) {
        BuyerOffersFragment f = new BuyerOffersFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AD_TITLE, adTitle);
        args.putString(ARG_AD_ID, adId);
        f.setArguments(args);
        return f;
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
        if (getArguments() != null) {
            adTitle = getArguments().getString(ARG_AD_TITLE);
            adId = getArguments().getString(ARG_AD_ID);
        }
        tvRespondingToAd.setText(adTitle != null && !adTitle.isEmpty() ? "Responding to your ad: " + adTitle
                : "Offers on all your wanted ads");
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (!isAdded()) return;
        SessionManager session = new SessionManager(requireContext());
        ApiService api = ApiClient.getApiService();
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyOffers.setVisibility(View.GONE);
        Call<List<SupplyOffer>> call = adId != null ? api.getOffersForRequest(adId) : api.getOffersForBuyer(session.getUserId());
        call.enqueue(new Callback<List<SupplyOffer>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupplyOffer>> c, @NonNull Response<List<SupplyOffer>> r) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                List<SupplyOffer> offers = r.isSuccessful() && r.body() != null ? new ArrayList<>(r.body()) : new ArrayList<>();
                // pending first, newest first
                offers.sort((a, b) -> {
                    int pa = "PENDING".equals(a.getStatus()) ? 0 : 1, pb = "PENDING".equals(b.getStatus()) ? 0 : 1;
                    if (pa != pb) return pa - pb;
                    return String.valueOf(b.getCreatedDate()).compareTo(String.valueOf(a.getCreatedDate()));
                });
                byId.clear();
                List<SupplyOfferItem> items = new ArrayList<>();
                for (SupplyOffer o : offers) {
                    byId.put(o.getId(), o);
                    String grade = o.getGrade() == null ? "-" : o.getGrade().replace("Grade ", "");
                    String status = o.getStatus() == null ? "PENDING" : o.getStatus();
                    items.add(new SupplyOfferItem(o.getId(),
                            "Grade " + grade + ("A".equalsIgnoreCase(grade) ? " Verified" : " Standard"),
                            "B".equalsIgnoreCase(grade) ? R.drawable.bg_badge_grade_b : R.drawable.bg_badge_grade_a,
                            "A".equalsIgnoreCase(grade),
                            BaseBuyerOrderActivity.time(o.getCreatedDate()) + " • " + statusLabel(status),
                            "Farmer: " + (o.getFarmerName() == null ? "-" : o.getFarmerName()),
                            "Location: " + (o.getLocation() == null ? "-" : o.getLocation()),
                            String.format(Locale.US, "Estimated: %,.0f KG %s", o.getQuantity(), o.getCropName() == null ? "" : o.getCropName()),
                            String.format(Locale.US, "Rs. %,.0f / KG", o.getPricePerKg())));
                }
                tvEmptyOffers.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                if (items.isEmpty()) {
                    tvEmptyOffers.setText(r.isSuccessful() ? "No supply offers yet. Farmers see your wanted ads and reply here."
                            : "Could not load offers.");
                }
                rvSupplyOffers.setAdapter(new SupplyOfferAdapter(items, item -> onOffer(byId.get(item.getId()))));
            }

            @Override
            public void onFailure(@NonNull Call<List<SupplyOffer>> c, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                tvEmptyOffers.setVisibility(View.VISIBLE);
                tvEmptyOffers.setText("Can't reach the server. Pull down or reopen to retry.");
            }
        });
    }

    private static String statusLabel(String s) {
        switch (s) {
            case "ACCEPTED": return "Accepted";
            case "DECLINED": return "Not selected";
            case "REJECTED": return "Rejected";
            default: return "Awaiting your decision";
        }
    }

    private void onOffer(SupplyOffer o) {
        if (o == null) return;
        if ("ACCEPTED".equals(o.getStatus())) {
            openOrderForOffer(o);
            return;
        }
        if (!"PENDING".equals(o.getStatus())) {
            Toast.makeText(requireContext(), "This offer is closed.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Accept this offer?")
                .setMessage(String.format(Locale.US, "%s offers %,.0f kg of %s at Rs. %,.0f/kg.\n\nAccepting starts the order "
                                + "and opens the chat to agree the final quantity, price and pickup date. Your wanted ad closes "
                                + "and the other offers are declined.",
                        o.getFarmerName(), o.getQuantity(), o.getCropName(), o.getPricePerKg()))
                .setPositiveButton("Accept & Chat", (d, w) -> accept(o))
                .setNegativeButton("Cancel", null).show();
    }

    private void accept(SupplyOffer o) {
        ApiClient.getApiService().acceptSupplyOffer(o.getId()).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> r) {
                if (!isAdded()) return;
                if (!r.isSuccessful() || r.body() == null) {
                    Toast.makeText(requireContext(), AuthInputUtils.errorMessage(r, "Could not accept the offer."), Toast.LENGTH_LONG).show();
                    load();
                    return;
                }
                openChat(r.body().getId());
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openOrderForOffer(SupplyOffer offer) {
        SessionManager session = new SessionManager(requireContext());
        ApiClient.getApiService().getBuyerOrders(session.getUserId()).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> r) {
                if (!isAdded() || r.body() == null) return;
                for (Order o : r.body()) {
                    if (offer.getId().equals(o.getSupplyOfferId())) {
                        startActivity(BuyerOrderRouter.intentFor(requireContext(), o, session.getUserId()));
                        return;
                    }
                }
                Toast.makeText(requireContext(), "No order found for this offer.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) { }
        });
    }

    private void openChat(String orderId) {
        Intent i = new Intent(requireContext(), NegotiationChatActivity.class);
        i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, orderId);
        startActivity(i);
    }
}
