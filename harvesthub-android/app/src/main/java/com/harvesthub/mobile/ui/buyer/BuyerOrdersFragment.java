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
                orderListContainer.removeAllViews();
                java.util.List<Order> active = new java.util.ArrayList<>(), done = new java.util.ArrayList<>();
                for (Order order : response.body()) {
                    if (FINAL.contains(order.getStatus())) done.add(order); else active.add(order);
                }
                addHeader("Active (" + active.size() + ")");
                for (Order order : active) addOrderRow(order);
                if (active.isEmpty()) addNote("No active orders. Send a purchase request from the marketplace.");
                addHeader("History (" + done.size() + ")");
                for (Order order : done) addOrderRow(order);
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

    /** What the buyer sees for each workflow status (Figma Order History 566:516). */
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

    private void addHeader(String text) {
        TextView h = new TextView(requireContext());
        h.setText(text);
        h.setTextColor(Color.parseColor("#1B4332"));
        h.setTextSize(16f);
        h.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        h.setPadding(8, 24, 8, 12);
        orderListContainer.addView(h);
    }

    private void addNote(String text) {
        TextView n = new TextView(requireContext());
        n.setText(text);
        n.setTextColor(Color.parseColor("#757575"));
        n.setPadding(8, 4, 8, 12);
        orderListContainer.addView(n);
    }

    private void addOrderRow(Order order) {
        if (!isAdded()) return;
        TextView row = new TextView(requireContext());
        Order.Payment p = order.getPayment();
        double total = p != null ? p.cropValue + p.transportFee + p.buyerFee : order.cropValue();
        String label = statusLabel(order);
        row.setText(String.format(java.util.Locale.US, "%s • %s\nOrder #%s • Farmer: %s\n%s\nTotal: %s",
                order.getCropName(), BaseBuyerOrderActivity.kg(order.getQuantity()), order.shortId(),
                order.getFarmerName() == null ? "-" : order.getFarmerName(), label, BaseBuyerOrderActivity.rs(total)));
        row.setTextColor(Color.parseColor(label.contains("action needed") ? "#B45309" : "#1A1C1A"));
        row.setTextSize(14f);
        row.setLineSpacing(4f, 1f);
        row.setPadding(28, 24, 28, 24);
        row.setBackgroundResource(R.drawable.bg_buyer_order_card);
        row.setOnClickListener(v -> startActivity(BuyerOrderRouter.intentFor(requireActivity(), order,
                new SessionManager(requireContext()).getUserId())));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 14;
        orderListContainer.addView(row, params);
    }

    private void showMessage(String message) {
        if (!isAdded()) return;
        orderListContainer.removeAllViews();
        TextView empty = new TextView(requireContext());
        empty.setText(message);
        empty.setTextColor(Color.parseColor("#757575"));
        empty.setTextSize(15f);
        empty.setPadding(8, 32, 8, 32);
        orderListContainer.addView(empty);
    }
}
