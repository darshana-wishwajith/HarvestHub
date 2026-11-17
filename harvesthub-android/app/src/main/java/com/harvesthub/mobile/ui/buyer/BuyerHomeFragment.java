package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
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
import com.harvesthub.mobile.ui.transporter.TUi;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Buyer Home (Figma buyer 564:330): greeting, wallet + escrow hero, stat tiles, quick actions, orders holding money
 * in escrow and fresh verified harvests. Everything comes from GET /dashboard/buyer/{id}; built with the shared
 * {@link TUi} kit so the buyer app has the same structure and look as the transporter screens.
 */
public class BuyerHomeFragment extends Fragment {

    private static final int GREEN = Color.parseColor("#15803D");

    private LinearLayout content;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        content = v.findViewById(R.id.homeContent);
        ((SwipeRefreshLayout) v.findViewById(R.id.swipeBuyerHome)).setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private BuyerDashboardActivity host() {
        return (BuyerDashboardActivity) requireActivity();
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
                render(r.body());
            }

            @Override
            public void onFailure(@NonNull Call<BuyerDashboard> call, @NonNull Throwable t) {
                if (!isAdded() || getView() == null) return;
                ((SwipeRefreshLayout) getView().findViewById(R.id.swipeBuyerHome)).setRefreshing(false);
                Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(BuyerDashboard d) {
        content.removeAllViews();

        // greeting + avatar
        LinearLayout gr = TUi.hrow(content, 8);
        LinearLayout gt = TUi.col(gr, 1f);
        TUi.text(gt, greeting(), 13, TUi.MUTED, false).setTypeface(TUi.semi(requireContext()));
        TextView name = TUi.text(gt, d.buyerName == null ? "Buyer" : d.buyerName, 22, TUi.DARK, true);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        TextView av = new TextView(requireContext());
        av.setText(initials(d.buyerName));
        av.setGravity(Gravity.CENTER);
        av.setTextSize(16);
        av.setTypeface(TUi.bold(requireContext()));
        av.setTextColor(Color.WHITE);
        av.setBackground(TUi.round(TUi.DARK, dp(30)));
        av.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.ACCOUNT));
        gr.addView(av, new LinearLayout.LayoutParams(dp(48), dp(48)));

        if (!d.hasDeliveryLocation) {
            LinearLayout n = TUi.notice(content, R.drawable.ic_location_pin, TUi.AMBER_BG, Color.parseColor("#D97706"),
                    "Set your delivery point", "Transport cost and vehicle are calculated from it. You can't order without it.");
            ((View) n.getParent().getParent()).setOnClickListener(x -> startActivity(new Intent(requireContext(), SetDeliveryAddressActivity.class)));
        }

        // wallet + escrow hero
        LinearLayout hero = TUi.card(content, TUi.DARK, TUi.DARK);
        LinearLayout hr = TUi.hrow(hero, 0);
        TUi.fill(hr, "AVAILABLE WALLET BALANCE", 11, Color.parseColor("#A7F3D0"), true).setLetterSpacing(0.05f);
        TUi.tag(hr, "✓ Trust " + d.trustPercent + "%", TUi.MINT, TUi.MINT_INK);
        TUi.text(hero, BaseBuyerOrderActivity.rs(d.walletBalance), 30, Color.WHITE, true);
        LinearLayout er = TUi.hrow(hero, 2);
        TUi.icon(er, R.drawable.ic_lock, 14, Color.parseColor("#A7F3D0"));
        TUi.fill(er, "  " + BaseBuyerOrderActivity.rs(d.escrowHeld) + " locked in escrow"
                + (d.ratingAverage == null ? "" : String.format(Locale.US, "  •  %.1f★ (%d)", d.ratingAverage, d.ratingCount)),
                12, Color.parseColor("#D1FAE5"), false);
        LinearLayout hb = TUi.hrow(hero, 6);
        half(TUi.smallButton(hb, "Wallet", R.drawable.ic_wallet, TUi.MINT, TUi.MINT_INK,
                x -> startActivity(new Intent(requireContext(), BuyerWalletActivity.class))), false);
        half(TUi.smallButton(hb, "Post Demand", R.drawable.ic_add_circle, Color.parseColor("#2D6A4F"), Color.WHITE,
                x -> startActivity(new Intent(requireContext(), PostDemandRequestActivity.class))), true);

        // stat tiles
        LinearLayout tiles = TUi.tiles(content);
        tiles.setPadding(0, dp(4), 0, 0);
        View t1 = TUi.tile(tiles, 0, "Active Orders", String.valueOf(d.activeOrders), null);
        View t2 = TUi.tile(tiles, 0, "Need Action", String.valueOf(d.actionRequired), d.actionRequired > 0 ? "Tap to review" : "All clear");
        View t3 = TUi.tile(tiles, 0, "Offers", String.valueOf(d.pendingOffers),
                d.offersExpiringToday > 0 ? d.offersExpiringToday + " expire today" : d.openWantedAds + " open ads");
        for (View t : new View[]{t1, t2, t3}) t.setBackground(TUi.round(Color.WHITE, dp(14)));
        t1.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.ORDERS));
        t2.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.ORDERS));
        t3.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.OFFERS));

        // quick actions
        TUi.sectionTitle(content, "Quick Actions", null);
        LinearLayout q1 = TUi.hrow(content, 10);
        action(q1, R.drawable.ic_shopping_bag, "Browse Market", "Officer-verified harvests", x -> host().selectTab(BuyerDashboardActivity.Tab.MARKET));
        action(q1, R.drawable.ic_document_ad, "My Requests", d.openWantedAds + " open wanted ads", x -> host().selectTab(BuyerDashboardActivity.Tab.DEMANDS));
        LinearLayout q2 = TUi.hrow(content, 10);
        action(q2, R.drawable.ic_truck, "Track Orders", d.activeOrders + " in progress", x -> host().selectTab(BuyerDashboardActivity.Tab.ORDERS));
        action(q2, R.drawable.ic_handshake, "Supply Offers", d.pendingOffers + " waiting for you", x -> host().selectTab(BuyerDashboardActivity.Tab.OFFERS));

        // orders holding money in escrow
        TextView all = TUi.sectionTitle(content, "Escrow Protected Orders", "See all ›");
        all.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.ORDERS));
        if (d.escrowOrders == null || d.escrowOrders.isEmpty()) {
            LinearLayout e = TUi.card(content);
            TUi.text(e, "No money is locked in escrow right now.", 13, TUi.MUTED, false);
        } else {
            for (BuyerDashboard.EscrowOrder e : d.escrowOrders) escrowCard(e);
        }

        // fresh verified harvests
        TextView browse = TUi.sectionTitle(content, "Fresh Verified Harvests", "Browse ›");
        browse.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.MARKET));
        if (d.highlights == null || d.highlights.isEmpty()) {
            LinearLayout e = TUi.card(content);
            TUi.text(e, "No verified harvests are listed right now.", 13, TUi.MUTED, false);
        } else {
            HorizontalScrollView hs = new HorizontalScrollView(requireContext());
            hs.setHorizontalScrollBarEnabled(false);
            hs.setClipToPadding(false);
            LinearLayout strip = new LinearLayout(requireContext());
            strip.setPadding(0, 0, 0, dp(6));
            hs.addView(strip);
            for (BuyerDashboard.Highlight h : d.highlights) harvestCard(strip, h);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            content.addView(hs, lp);
        }
    }

    /** Half-width button inside a row. */
    private void half(View b, boolean second) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.topMargin = dp(10);
        if (second) lp.leftMargin = dp(8);
        b.setLayoutParams(lp);
    }

    /** One of the 2×2 quick-action cards. */
    private void action(LinearLayout row, int iconRes, String title, String sub, View.OnClickListener click) {
        LinearLayout c = new LinearLayout(requireContext());
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(TUi.round(Color.WHITE, dp(16)));
        c.setElevation(dp(1.5f));
        int p = dp(14);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (row.getChildCount() > 0) lp.leftMargin = dp(10);
        row.addView(c, lp);
        TUi.iconTile(c, iconRes, 40, TUi.SURFACE, TUi.DARK, false);
        TUi.text(c, title, 14, TUi.INK, true).setPadding(0, dp(10), 0, 0);
        TUi.text(c, sub, 12, TUi.MUTED, false);
        c.setOnClickListener(click);
    }

    private void escrowCard(BuyerDashboard.EscrowOrder e) {
        LinearLayout c = TUi.card(content);
        ((LinearLayout.LayoutParams) ((View) c.getParent()).getLayoutParams()).topMargin = dp(10);
        ((View) c.getParent()).setOnClickListener(x -> BuyerOrderRouter.open(requireContext(), e.orderId));
        LinearLayout r = TUi.hrow(c, 0);
        TUi.iconTile(r, R.drawable.ic_lock, 42, TUi.SURFACE, TUi.DARK, false);
        LinearLayout t = TUi.col(r, 1f);
        t.setPadding(dp(12), 0, dp(8), 0);
        TextView crop = TUi.text(t, e.cropName == null ? "Order" : e.cropName, 15, TUi.INK, true);
        crop.setMaxLines(1);
        crop.setEllipsize(TextUtils.TruncateAt.END);
        TUi.text(t, "Order #" + shortId(e.orderId) + " • Farmer: " + (e.farmerName == null ? "—" : e.farmerName), 12, TUi.MUTED, false);
        TUi.pill(t, pretty(e.status), Color.parseColor("#EEF4FF"), Color.parseColor("#1E3A8A")).setTextSize(10);
        LinearLayout v = TUi.col(r, 0);
        v.setGravity(Gravity.END);
        TUi.label(v, BaseBuyerOrderActivity.rs(e.amountHeld), 15, TUi.DARK, true);
        TUi.label(v, "held", 11, TUi.MUTED, false);
    }

    private void harvestCard(LinearLayout strip, BuyerDashboard.Highlight h) {
        LinearLayout c = new LinearLayout(requireContext());
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(TUi.round(Color.WHITE, dp(16)));
        c.setElevation(dp(1.5f));
        c.setPadding(dp(10), dp(10), dp(10), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(210), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(12);
        lp.bottomMargin = dp(4);
        strip.addView(c, lp);
        TUi.thumb(c, h.imageUrl, 190, 110);
        String g = h.grade == null ? "" : h.grade.replace("Grade ", "");
        int gBg = "A".equals(g) ? Color.parseColor("#16A34A") : "B".equals(g) ? Color.parseColor("#D97706") : Color.parseColor("#64748B");
        TUi.pill(c, "✓ Grade " + (g.isEmpty() ? "—" : g) + " Verified", gBg, Color.WHITE).setTextSize(10);
        TextView n = TUi.text(c, h.cropName, 16, TUi.INK, true);
        n.setMaxLines(1);
        n.setEllipsize(TextUtils.TruncateAt.END);
        TUi.text(c, String.format(Locale.US, "%,.0f KG • %s", h.quantityKg, h.location == null ? "" : h.location), 12, TUi.MUTED, false)
                .setMaxLines(1);
        LinearLayout pr = TUi.hrow(c, 6);
        TUi.tag(pr, String.format(Locale.US, "Rs. %,.0f /KG", h.pricePerKg), TUi.SURFACE, TUi.DARK);
        TextView when = TUi.fill(pr, BaseBuyerOrderActivity.time(h.listedAt), 10, TUi.MUTED, false);
        when.setGravity(Gravity.END);
        c.setOnClickListener(x -> host().selectTab(BuyerDashboardActivity.Tab.MARKET));
    }

    private static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h < 12 ? "Good morning," : h < 17 ? "Good afternoon," : "Good evening,";
    }

    static String pretty(String status) {
        if (status == null || status.isEmpty()) return "";
        String t = status.replace('_', ' ').toLowerCase(Locale.US);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String shortId(String id) {
        if (id == null) return "";
        return id.substring(Math.max(0, id.length() - 6)).toUpperCase(Locale.US);
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "B";
        String[] parts = name.trim().split("\\s+");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase(Locale.US);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
