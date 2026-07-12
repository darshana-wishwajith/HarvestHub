package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.NotificationItem;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationActionListener {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private TextView tvMarkAllRead, tvEmptyState;
    private ApiService apiService;
    private String farmerId = "FARMER_123"; // Should be from session

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        // Clear list initially to show empty state while loading
        notificationList.clear();
        updateEmptyState();
        
        fetchNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());

        findViewById(R.id.ivNotificationTop).setOnClickListener(v -> finish());

        // Bottom nav listeners
        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
    }

    private void fetchNotifications() {
        apiService.getNotifications(farmerId).enqueue(new Callback<List<NotificationItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationItem>> call, @NonNull Response<List<NotificationItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    notificationList.clear();
                    notificationList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    // If API is empty/failed, you can optionally call addMockData() 
                    // or just leave it empty to see the "No notifications" message.
                    // addMockData(); 
                }
                updateEmptyState();
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationItem>> call, @NonNull Throwable t) {
                // For development, show mock data on network error so you can see the cards
                addMockData();
                updateEmptyState();
            }
        });
    }

    private void addMockData() {
        notificationList.clear();
        notificationList.add(new NotificationItem("1", "DELIVERY_SUCCESS", "Delivery Successful – Funds Transferred", 
                "LKR 140,000 has been credited to your wallet after successful verification of the paddy delivery at the Ampara Depot.", "2 mins ago", false));
        notificationList.add(new NotificationItem("2", "PENALTY", "Penalty Alert: Empty Trip Fee", 
                "A fee of LKR 1,500 was applied due to a late cancellation on the logistics request #TRK-8821.", "Yesterday", true));
        notificationList.add(new NotificationItem("3", "LISTING_APPROVED", "Listing Approved", 
                "Your requested listing for 500 KG of Organic Keeri Samba has been verified and is now live.", "Oct 12", true));
        notificationList.add(new NotificationItem("4", "ACCOUNT_VERIFIED", "Account Verified", 
                "Your farmer certification has been successfully processed. You now have access to premium buyer listings.", "Oct 10", true));
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        tvEmptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onMarkAsRead(NotificationItem notification, int position) {
        apiService.markNotificationRead(notification.getId()).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                notification.setRead(true);
                adapter.notifyItemChanged(position);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                // For demo, just update UI
                notification.setRead(true);
                adapter.notifyItemChanged(position);
            }
        });
    }

    private void markAllAsRead() {
        apiService.markAllNotificationsRead(farmerId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                for (NotificationItem item : notificationList) {
                    item.setRead(true);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                // For demo, just update UI
                for (NotificationItem item : notificationList) {
                    item.setRead(true);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
