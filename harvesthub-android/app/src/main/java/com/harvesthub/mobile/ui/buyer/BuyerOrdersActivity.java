package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.BuyerNavigationHelper;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BuyerOrdersActivity extends AppCompatActivity {
    private LinearLayout orderListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_orders);

        orderListContainer = findViewById(R.id.orderListContainer);
        ViewGroup topNav = findViewById(R.id.buyerTopNav);
        ViewGroup bottomNav = findViewById(R.id.buyerBottomNav);
        BuyerNavigationHelper.setup(this, bottomNav, BuyerNavigationHelper.Tab.ORDERS);
        BuyerNavigationHelper.setupNotifications(this, topNav);
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BuyerNavigationHelper.setupNotifications(this, findViewById(R.id.buyerTopNav));
        loadOrders();
    }

    private void loadOrders() {
        SessionManager sessionManager = new SessionManager(this);
        String buyerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                ? sessionManager.getUser().getId() : "user_buyer_perera";

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerOrders(buyerId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    showMessage("No buyer orders yet.");
                    return;
                }
                orderListContainer.removeAllViews();
                for (Order order : response.body()) addOrderRow(order);
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable throwable) {
                showMessage("Unable to load buyer orders.");
            }
        });
    }

    private void addOrderRow(Order order) {
        TextView row = new TextView(this);
        String cropTitle = order.getCropName() != null ? order.getCropName() : (order.getCropType() != null ? order.getCropType() : "Harvest Order");
        String orderRef = order.getId() != null ? "Order #" + order.getId() : "Pending reference";
        String status = order.getStatus() != null ? order.getStatus() : "PENDING";
        double total = order.getCropTotal() > 0 ? order.getCropTotal() : 37000.0;

        row.setText(String.format("%s • %s\nStatus: %s • Amount: Rs. %,.2f\n[Tap to View Details & Tracking]",
                cropTitle, orderRef, status, total));
        row.setTextColor(Color.parseColor("#1A1C1A"));
        row.setTextSize(14f);
        row.setPadding(24, 24, 24, 24);
        row.setBackgroundResource(R.drawable.bg_buyer_order_card);
        row.setOnClickListener(v -> {
            if ("DELIVERED_PENDING_HANDSHAKE".equalsIgnoreCase(status)) {
                Intent intent = new Intent(this, ArrivalInspectionActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                startActivity(intent);
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                Intent intent = new Intent(this, DeliveryHandshakeActivity.class);
                intent.putExtra("ORDER_ID", order.getId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, TransitTrackingActivity.class);
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
        orderListContainer.removeAllViews();
        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextColor(Color.parseColor("#757575"));
        empty.setTextSize(16f);
        empty.setPadding(8, 32, 8, 32);
        orderListContainer.addView(empty);
    }
}