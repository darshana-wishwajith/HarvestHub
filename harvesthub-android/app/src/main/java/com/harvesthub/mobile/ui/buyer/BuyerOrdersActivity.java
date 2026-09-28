package com.harvesthub.mobile.ui.buyer;

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

    private void loadOrders() {
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUser() == null || sessionManager.getUser().getId() == null) {
            showMessage("Sign in to view your buyer orders.");
            return;
        }
        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.getBuyerOrders(sessionManager.getUser().getId()).enqueue(new Callback<List<Order>>() {
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
        row.setText(String.format("%s\n%s\nStatus: %s",
                order.getCropName() == null ? "Harvest order" : order.getCropName(),
                order.getId() == null ? "Pending reference" : "Order " + order.getId(),
                order.getStatus() == null ? "PENDING" : order.getStatus()));
        row.setTextColor(Color.parseColor("#1A1C1A"));
        row.setTextSize(15f);
        row.setPadding(20, 20, 20, 20);
        row.setBackgroundResource(R.drawable.bg_buyer_order_card);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 12;
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