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
        String buyerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                ? sessionManager.getUser().getId() : "user_buyer_keells";

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
                for (Order order : response.body()) {
                    addOrderRow(order);
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable throwable) {
                if (!isAdded()) return;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                showMessage("Unable to load buyer orders from database.");
            }
        });
    }

    private void addOrderRow(Order order) {
        if (!isAdded()) return;
        TextView row = new TextView(requireContext());
        String cropTitle = order.getCropName() != null ? order.getCropName() : (order.getCropType() != null ? order.getCropType() : "Harvest Order");
        String orderRef = order.getId() != null ? "Order #" + order.getId() : "Pending reference";
        String status = order.getStatus() != null ? order.getStatus() : "PENDING";
        double total = order.getTotalAmount() > 0 ? order.getTotalAmount() : (order.getCropTotal() > 0 ? order.getCropTotal() : 37000.0);

        row.setText(String.format("%s • %s\nStatus: %s • Amount: Rs. %,.2f\n[Tap to View Details & Tracking]",
                cropTitle, orderRef, status, total));
        row.setTextColor(Color.parseColor("#1A1C1A"));
        row.setTextSize(14f);
        row.setPadding(24, 24, 24, 24);
        row.setBackgroundResource(R.drawable.bg_buyer_order_card);
        row.setOnClickListener(v -> {
            if ("DELIVERED_PENDING_HANDSHAKE".equalsIgnoreCase(status)) {
                Intent intent = new Intent(requireActivity(), ArrivalInspectionActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                startActivity(intent);
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                Intent intent = new Intent(requireActivity(), DeliveryHandshakeActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(requireActivity(), TransitTrackingActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                startActivity(intent);
            }
        });

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
