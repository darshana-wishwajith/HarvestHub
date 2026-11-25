package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.transporter.TUi;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerOrdersFragment extends Fragment {

    private LinearLayout orderListContainer;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buyer_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        orderListContainer = view.findViewById(R.id.orderListContainer);
        progressBar = view.findViewById(R.id.progressBarOrders);

        loadOrders();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    public void loadOrders() {
        if (!isAdded()) return;
        SessionManager sessionManager = new SessionManager(requireContext());
        String buyerId = sessionManager.getUserId();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerOrders(buyerId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    showMessage("No buyer orders yet.");
                    return;
                }
                all = response.body();
                render();
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable throwable) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                showMessage("Unable to load buyer orders from database.");
            }
        });
    }

    static final java.util.Set<String> FINAL = new java.util.HashSet<>(java.util.Arrays.asList(
            "COMPLETED", "PRELOAD_REJECTED", "RETURN_JOURNEY", "CANCELLED"));

    /** What the buyer sees for each workflow status. */
    static String statusLabel(Order o) {
        String s = o.getStatus() == null ? "" : o.getStatus();
        switch (s) {
            case "NEGOTIATING": return o.isBuyerConfirmed() && !o.isFarmerConfirmed() ? "Waiting for the farmer" : "Negotiating – action needed";
            case "AWAITING_PAYMENT": return "Pay the advance – action needed";
            case "DISPATCHING": case "TRANSPORTER_ASSIGNED": return "Assigning a vehicle";
            case "PAUSED": return "Waiting for a free vehicle";
            case "TRANSPORTER_ACCEPTED": return "Vehicle assigned";
            case "HARVESTING": return "Farmer harvesting";
            case "HARVEST_READY": return "Ready for pickup";
            case "ARRIVED_AT_PICKUP": return "Vehicle at the farm";
            case "AWAITING_BUYER_LOADING_APPROVAL": return "Approve loading photos – action needed";
            case "LOADING_APPROVED": return "Loading";
            case "IN_TRANSIT": return "In transit";
            case "ARRIVED_AT_DESTINATION": return "Arrived – inspect & pay balance";
            case "BALANCE_PAID": case "DELIVERED":
                return o.isBuyerConfirmedReceipt() ? "Waiting for the driver" : "Confirm goods received – action needed";
            case "COMPLETED": return "Completed";
            case "PRELOAD_REJECTED": return "Rejected before loading – refunded";
            case "RETURN_JOURNEY": return "Rejected on delivery – refunded";
            case "CANCELLED": return "Cancelled";
            default: return s;
        }
    }

    private java.util.List<Order> all = new java.util.ArrayList<>();
    private int filter; // 0 active, 1 completed, 2 refunded / cancelled

    private static final String[] STAGES = {"NEGOTIATING", "AWAITING_PAYMENT", "DISPATCHING", "TRANSPORTER_ACCEPTED", "HARVESTING",
            "HARVEST_READY", "ARRIVED_AT_PICKUP", "AWAITING_BUYER_LOADING_APPROVAL", "LOADING_APPROVED", "IN_TRANSIT",
            "ARRIVED_AT_DESTINATION", "BALANCE_PAID", "COMPLETED"};

    private static boolean refunded(String s) {
        return "PRELOAD_REJECTED".equals(s) || "RETURN_JOURNEY".equals(s) || "RETURNED".equals(s) || "CANCELLED".equals(s);
    }

    private void render() {
        if (!isAdded()) return;
        orderListContainer.removeAllViews();
        java.util.List<Order> active = new java.util.ArrayList<>(), done = new java.util.ArrayList<>(), back = new java.util.ArrayList<>();
        for (Order o : all) {
            String s = o.getStatus() == null ? "" : o.getStatus();
            if ("COMPLETED".equals(s)) done.add(o); else if (refunded(s)) back.add(o); else active.add(o);
        }
        java.util.List<TextView> chips = TUi.chips(orderListContainer,
                new String[]{"Active (" + active.size() + ")", "Completed (" + done.size() + ")", "Refunded (" + back.size() + ")"}, false, null);
        for (int i = 0; i < chips.size(); i++) {
            final int f = i;
            TUi.setChip(chips.get(i), filter == i);
            chips.get(i).setOnClickListener(v -> {
                filter = f;
                render();
            });
        }
        java.util.List<Order> list = filter == 0 ? active : filter == 1 ? done : back;
        if (list.isEmpty()) {
            LinearLayout c = TUi.card(orderListContainer);
            LinearLayout r = TUi.hrow(c, 0);
            TUi.iconTile(r, R.drawable.ic_package, 44, TUi.SURFACE, TUi.DARK, false);
            LinearLayout t = TUi.col(r, 1f);
            t.setPadding(dp(12), 0, 0, 0);
            TUi.text(t, filter == 0 ? "No active orders" : filter == 1 ? "No completed orders yet" : "No refunded orders", 15, TUi.INK, true);
            TUi.text(t, filter == 0 ? "Send a purchase request from the Market or accept a supply offer." : "They will appear here.", 13, TUi.MUTED, false);
        }
        for (Order o : list) addOrderCard(o);
    }

    private void addOrderCard(Order order) {
        if (!isAdded()) return;
        String s = order.getStatus() == null ? "" : order.getStatus();
        String label = statusLabel(order);
        boolean action = label.contains("action needed");
        boolean ok = "COMPLETED".equals(s);
        boolean bad = refunded(s);
        Order.Payment p = order.getPayment();
        double total = p != null ? p.cropValue + p.transportFee + p.buyerFee : order.cropValue();

        LinearLayout c = TUi.card(orderListContainer);
        ((LinearLayout.LayoutParams) ((View) c.getParent()).getLayoutParams()).topMargin = dp(12);
        LinearLayout top = TUi.hrow(c, 0);
        TUi.tag(top, "#" + order.shortId(), TUi.GREY_BG, TUi.INK);
        TextView when = TUi.fill(top, "  " + BaseBuyerOrderActivity.time(order.getCreatedAt()), 12, TUi.MUTED, false);
        when.setMaxLines(1);
        TUi.tag(top, action ? "Action needed" : ok ? "✓ Completed" : bad ? "Refunded" : "In progress",
                action ? TUi.AMBER_BG : ok ? TUi.MINT : bad ? TUi.RED_BG : Color.parseColor("#EEF4FF"),
                action ? TUi.AMBER : ok ? TUi.MINT_INK : bad ? TUi.RED : Color.parseColor("#1E3A8A"));

        LinearLayout mid = TUi.hrow(c, 12);
        TUi.iconTile(mid, R.drawable.ic_package, 46, TUi.SURFACE, TUi.DARK, false);
        LinearLayout t = TUi.col(mid, 1f);
        t.setPadding(dp(12), 0, dp(8), 0);
        TextView crop = TUi.text(t, (order.getCropName() == null ? "Produce" : order.getCropName()) + " • "
                + BaseBuyerOrderActivity.kg(order.getQuantity()), 16, TUi.INK, true);
        crop.setMaxLines(1);
        crop.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.text(t, "Farmer: " + (order.getFarmerName() == null ? "—" : order.getFarmerName())
                + (order.getTransporterName() == null ? "" : " • Driver: " + order.getTransporterName()), 12, TUi.MUTED, false);
        LinearLayout v = TUi.col(mid, 0);
        v.setGravity(android.view.Gravity.END);
        TUi.label(v, BaseBuyerOrderActivity.rs(total), 15, TUi.DARK, true);
        TUi.label(v, "order total", 10, TUi.MUTED, false);

        LinearLayout st = TUi.box(c, action ? TUi.AMBER_BG : TUi.GREY_BG, 12);
        TUi.text(st, label, 13, action ? TUi.AMBER : TUi.INK, true);
        if (!bad) {
            int idx = java.util.Arrays.asList(STAGES).indexOf("DELIVERED".equals(s) ? "BALANCE_PAID"
                    : "TRANSPORTER_ASSIGNED".equals(s) || "PAUSED".equals(s) ? "DISPATCHING" : s);
            int pct = idx < 0 ? 0 : Math.round(idx * 100f / (STAGES.length - 1));
            android.widget.ProgressBar bar = TUi.progress(st, pct, 8);
            if (ok) bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#15803D")));
            TUi.text(st, "Step " + Math.max(1, idx + 1) + " of " + STAGES.length, 11, TUi.MUTED, false).setPadding(0, dp(4), 0, 0);
        }
        View.OnClickListener open = x -> startActivity(BuyerOrderRouter.intentFor(requireActivity(), order,
                new SessionManager(requireContext()).getUserId()));
        ((View) c.getParent()).setOnClickListener(open);
        if (action) {
            TUi.smallButton(c, nextStep(s) + " →", 0, TUi.DARK, Color.WHITE, open);
        } else {
            TextView more = TUi.text(c, (ok || bad ? "View details" : "Track order") + " →", 14, TUi.DARK, true);
            more.setGravity(android.view.Gravity.END);
            more.setPadding(0, dp(10), 0, 0);
        }
    }

    /** Button text for the step the buyer has to do. */
    private static String nextStep(String s) {
        switch (s) {
            case "NEGOTIATING": return "Open Negotiation";
            case "AWAITING_PAYMENT": return "Pay Advance";
            case "AWAITING_BUYER_LOADING_APPROVAL": return "Review Loading Photos";
            case "ARRIVED_AT_DESTINATION": return "Inspect Delivery";
            default: return "Confirm Goods Received";
        }
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void showMessage(String message) {
        if (!isAdded()) return;
        orderListContainer.removeAllViews();
        LinearLayout c = TUi.card(orderListContainer);
        TUi.text(c, message, 14, TUi.MUTED, false);
    }
}
