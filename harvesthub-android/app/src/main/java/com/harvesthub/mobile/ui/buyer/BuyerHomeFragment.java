package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.BuyerDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.RemoteImage;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer "Buyer Home Dashboard" (564:330): active orders and escrow held, pending offers, quick actions,
 * three newest verified lots and the escrow ledger — all from GET /dashboard/buyer/{id}, nothing hard-coded.
 */
public class BuyerHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        BuyerDashboardActivity host = (BuyerDashboardActivity) requireActivity();
        v.findViewById(R.id.btnHomePostAd).setOnClickListener(x -> startActivity(new Intent(requireContext(), PostDemandRequestActivity.class)));
        v.findViewById(R.id.btnHomeMarket).setOnClickListener(x -> host.selectTab(BuyerDashboardActivity.Tab.MARKET));
        v.findViewById(R.id.btnHomeSeeAll).setOnClickListener(x -> host.selectTab(BuyerDashboardActivity.Tab.MARKET));
        v.findViewById(R.id.btnHomeWallet).setOnClickListener(x -> startActivity(new Intent(requireContext(), BuyerWalletActivity.class)));
        v.findViewById(R.id.btnHomeProfile).setOnClickListener(x -> startActivity(new Intent(requireContext(), BuyerProfileActivity.class)));
        v.findViewById(R.id.cardActiveOrders).setOnClickListener(x -> host.selectTab(BuyerDashboardActivity.Tab.ORDERS));
        v.findViewById(R.id.cardPendingOffers).setOnClickListener(x -> host.selectTab(BuyerDashboardActivity.Tab.OFFERS));
        v.findViewById(R.id.tvHomeLocationWarning).setOnClickListener(x ->
                startActivity(new Intent(requireContext(), SetDeliveryAddressActivity.class)));
        ((SwipeRefreshLayout) v.findViewById(R.id.swipeBuyerHome)).setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String me = new SessionManager(requireContext()).getUserId();
        if (me == null) return;
        ApiClient.getApiService().getBuyerDashboard(me).enqueue(new Callback<BuyerDashboard>() {
            @Override
            public void onResponse(@NonNull Call<BuyerDashboard> call, @NonNull Response<BuyerDashboard> r) {
                if (!isAdded() || getView() == null) return;
                ((SwipeRefreshLayout) getView().findViewById(R.id.swipeBuyerHome)).setRefreshing(false);
                if (!r.isSuccessful() || r.body() == null) {
                    Toast.makeText(requireContext(), "Could not load your dashboard.", Toast.LENGTH_SHORT).show();
                    return;
                }
                bind(getView(), r.body());
            }

            @Override
            public void onFailure(@NonNull Call<BuyerDashboard> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                ((SwipeRefreshLayout) getView().findViewById(R.id.swipeBuyerHome)).setRefreshing(false);
                Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bind(View v, BuyerDashboard d) {
        text(v, R.id.tvHomeGreeting, d.greeting);
        String rating = d.ratingAverage == null ? "" : String.format(Locale.US, " • %.1f★", d.ratingAverage);
        text(v, R.id.tvHomeBadge, "✓ Escrow Protected • Trust " + d.trustPercent + "%" + rating);
        String initials = initials(d.buyerName);
        text(v, R.id.btnHomeProfile, initials);
        v.findViewById(R.id.tvHomeLocationWarning).setVisibility(d.hasDeliveryLocation ? View.GONE : View.VISIBLE);

        text(v, R.id.tvActiveOrders, String.valueOf(d.activeOrders));
        text(v, R.id.tvActiveOrdersSub, BaseBuyerOrderActivity.rs(d.escrowHeld) + " in escrow"
                + (d.actionRequired > 0 ? "\n" + d.actionRequired + " need your action" : ""));
        text(v, R.id.tvPendingOffers, String.valueOf(d.pendingOffers));
        text(v, R.id.tvPendingOffersSub, d.offersExpiringToday > 0 ? d.offersExpiringToday + " expiring today"
                : d.openWantedAds + (d.openWantedAds == 1 ? " open wanted ad" : " open wanted ads"));
        text(v, R.id.tvEscrowTotal, BaseBuyerOrderActivity.rs(d.escrowHeld));

        LinearLayout hl = v.findViewById(R.id.layoutHighlights);
        hl.removeAllViews();
        if (d.highlights == null || d.highlights.isEmpty()) {
            hl.addView(note("No verified harvests are listed right now."));
        } else {
            for (BuyerDashboard.Highlight h : d.highlights) hl.addView(highlight(h));
        }

        LinearLayout esc = v.findViewById(R.id.layoutEscrowOrders);
        esc.removeAllViews();
        if (d.escrowOrders == null || d.escrowOrders.isEmpty()) {
            esc.addView(note("No money is locked in escrow."));
        } else {
            for (BuyerDashboard.EscrowOrder e : d.escrowOrders) {
                TextView row = new TextView(requireContext());
                String id = e.orderId == null ? "" : e.orderId.substring(Math.max(0, e.orderId.length() - 6)).toUpperCase(Locale.US);
                row.setText(String.format(Locale.US, "🔒 Order #%s • %s\n%s • Farmer: %s", id, BaseBuyerOrderActivity.rs(e.amountHeld),
                        e.cropName, e.farmerName));
                row.setTextColor(0xFF334155);
                row.setTextSize(13f);
                row.setBackgroundColor(0xFFF1F5F9);
                int p = dp(10);
                row.setPadding(p, p, p, p);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.bottomMargin = dp(8);
                row.setOnClickListener(x -> BuyerOrderRouter.open(requireContext(), e.orderId));
                esc.addView(row, lp);
            }
        }
    }

    private View highlight(BuyerDashboard.Highlight h) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        ImageView img = new ImageView(requireContext());
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackgroundColor(0xFFE2E8F0);
        row.addView(img, new LinearLayout.LayoutParams(dp(56), dp(56)));
        if (h.imageUrl != null) RemoteImage.load(img, BaseBuyerOrderActivity.mediaUrl(h.imageUrl));

        LinearLayout text = new LinearLayout(requireContext());
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(12), 0, 0, 0);
        TextView t = new TextView(requireContext());
        t.setText(String.format(Locale.US, "Grade %s %s – Rs %,.0f/kg", h.grade == null ? "-" : h.grade, h.cropName, h.pricePerKg));
        t.setTextColor(0xFF111827);
        t.setTextSize(15f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        TextView s = new TextView(requireContext());
        s.setText(String.format(Locale.US, "%s • %s • %,.0f KG\n✓ Officer verified • listed %s",
                h.farmerName == null ? "" : h.farmerName, h.location == null ? "" : h.location, h.quantityKg,
                BaseBuyerOrderActivity.time(h.listedAt)));
        s.setTextColor(0xFF64748B);
        s.setTextSize(12f);
        text.addView(t);
        text.addView(s);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setOnClickListener(x -> ((BuyerDashboardActivity) requireActivity()).selectTab(BuyerDashboardActivity.Tab.MARKET));
        return row;
    }

    private TextView note(String s) {
        TextView t = new TextView(requireContext());
        t.setText(s);
        t.setTextColor(0xFF64748B);
        t.setPadding(dp(4), dp(8), dp(4), dp(8));
        return t;
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "B";
        String[] parts = name.trim().split("\\s+");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.US);
    }

    private void text(View root, int id, String s) {
        TextView t = root.findViewById(id);
        if (t != null) t.setText(s == null ? "" : s);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
