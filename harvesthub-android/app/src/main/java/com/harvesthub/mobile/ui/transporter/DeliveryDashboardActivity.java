package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.TransporterEventRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryDashboardActivity extends AppCompatActivity {
    private LinearLayout jobsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_delivery_dashboard);
        jobsContainer = findViewById(R.id.transporterJobsContainer);
        setupGreeting();
        loadJobs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupGreeting();
        loadJobs();
    }

    private void setupGreeting() {
        TextView tvGreeting = findViewById(R.id.tvTransporterGreeting);
        if (tvGreeting != null) {
            SessionManager session = new SessionManager(this);
            String name = (session.getUser() != null && session.getUser().getFullName() != null && !session.getUser().getFullName().isEmpty())
                    ? session.getUser().getFullName() : "Priyantha Logistics";
            tvGreeting.setText("Welcome, " + name + " • Active Dispatch Queue");
        }
    }

    private void loadJobs() {
        SessionManager session = new SessionManager(this);
        String transporterId = (session.getUser() != null && session.getUser().getId() != null)
                ? session.getUser().getId() : "user_transporter_priyantha";

        ApiService api = ApiClient.getClient(session).create(ApiService.class);
        api.getTransporterOrders(transporterId, null).enqueue(new Callback<List<Order>>() {
            @Override public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    showMessage("No assigned delivery jobs.");
                    return;
                }
                jobsContainer.removeAllViews();
                for (Order order : response.body()) addJob(order);
            }
            @Override public void onFailure(Call<List<Order>> call, Throwable throwable) {
                showMessage("Unable to load assigned jobs.");
            }
        });
    }

    private void addJob(Order order) {
        TextView job = new TextView(this);
        job.setText((order.getCropName() == null ? "Harvest delivery" : order.getCropName())
                + "\nOrder: " + order.getId() + "\nStatus: " + order.getStatus());
        job.setTextColor(Color.WHITE);
        job.setTextSize(16);
        job.setPadding(24, 24, 24, 24);
        job.setBackgroundColor(Color.rgb(27, 67, 50));
        job.setOnClickListener(v -> acceptJob(order));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(16, 12, 16, 0);
        jobsContainer.addView(job, params);
    }

    private void acceptJob(Order order) {
        SessionManager session = new SessionManager(this);
        TransporterEventRequest request = new TransporterEventRequest(
                session.getUser().getId(), "ACCEPT", "Transporter accepted dispatch");
        ApiClient.getClient(session).create(ApiService.class)
                .applyTransporterEvent(order.getId(), request)
                .enqueue(new Callback<Order>() {
                    @Override public void onResponse(Call<Order> call, Response<Order> response) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(DeliveryDashboardActivity.this, "Job is no longer available.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent intent = new Intent(DeliveryDashboardActivity.this, ArrivingPickupActivity.class);
                        intent.putExtra("ORDER_ID", order.getId());
                        startActivity(intent);
                    }
                    @Override public void onFailure(Call<Order> call, Throwable throwable) {
                        Toast.makeText(DeliveryDashboardActivity.this, "Unable to accept job.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showMessage(String message) {
        jobsContainer.removeAllViews();
        TextView text = new TextView(this);
        text.setText(message);
        text.setTextColor(Color.DKGRAY);
        text.setTextSize(16);
        text.setPadding(20, 40, 20, 40);
        jobsContainer.addView(text);
    }
}
