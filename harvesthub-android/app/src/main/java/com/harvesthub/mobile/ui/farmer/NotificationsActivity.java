package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.NotificationItem;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;
import com.harvesthub.mobile.utils.SessionManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationActionListener {

    public static final String EXTRA_HIDE_NAV = "extra_hide_nav";

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList = new ArrayList<>();
    private ApiService apiService;
    private ProgressBar progressBar;
    private String farmerId = "FARMER_ID_HERE";
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        sessionManager = new SessionManager(this);
        if (sessionManager.getUser() != null) {
            farmerId = sessionManager.getUser().getId();
        }
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        initViews();
        handleNavigationVisibility();
        setupRecyclerView();
        fetchNotifications();

        findViewById(R.id.tvMarkAllRead).setOnClickListener(v -> markAllAsRead());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleNavigationVisibility() {
        boolean hideNav = getIntent().getBooleanExtra(EXTRA_HIDE_NAV, false);
        if (hideNav) {
            View topNav = findViewById(R.id.top_nav_bar);
            View bottomNav = findViewById(R.id.bottom_nav_bar);
            ImageView btnBack = findViewById(R.id.btnBack);
            ConstraintLayout contentArea = findViewById(R.id.contentArea);

            if (topNav != null) topNav.setVisibility(View.GONE);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            if (btnBack != null) btnBack.setVisibility(View.VISIBLE);

            // Adjust constraints to fill screen
            if (contentArea != null) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) contentArea.getLayoutParams();
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                contentArea.setLayoutParams(params);
            }
        }
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);
    }

    private void fetchNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getNotifications(farmerId).enqueue(new Callback<List<NotificationItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<NotificationItem>> call, @NonNull Response<List<NotificationItem>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    notificationList.clear();
                    notificationList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<NotificationItem>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                // Load dummy data for demonstration if API fails
                loadDummyData();
            }
        });
    }

    private void loadDummyData() {
        notificationList.add(new NotificationItem("1", "DELIVERY_SUCCESS", "Delivery Successful", "LKR 140,000 credited to your wallet.", "2 mins ago", false));
        notificationList.add(new NotificationItem("2", "PENALTY", "Penalty Notice", "Small deduction due to moisture levels.", "1 hour ago", true));
        notificationList.add(new NotificationItem("3", "LISTING_APPROVED", "Listing Approved", "Your Carrot listing is now live.", "3 hours ago", true));
        adapter.notifyDataSetChanged();
    }

    private void markAllAsRead() {
        apiService.markAllNotificationsRead(farmerId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    for (NotificationItem item : notificationList) {
                        item.setRead(true);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMarkAsRead(NotificationItem notification, int position) {
        apiService.markNotificationRead(notification.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    notification.setRead(true);
                    adapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(NotificationsActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
