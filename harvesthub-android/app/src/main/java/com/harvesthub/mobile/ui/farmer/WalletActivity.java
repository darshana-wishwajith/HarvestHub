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
import com.harvesthub.mobile.model.Transaction;
import com.harvesthub.mobile.model.WalletSummaryResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private TextView tvBalance, tvTrustScore, tvPenalties, tvViewAll, tvEmptyState;
    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private ApiService apiService;
    private final String farmerId = "FARMER_123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_wallet);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        fetchWalletSummary();
        fetchRecentTransactions();
    }

    private void initViews() {
        tvBalance = findViewById(R.id.tvBalance);
        tvTrustScore = findViewById(R.id.tvTrustScore);
        tvPenalties = findViewById(R.id.tvPenalties);
        tvViewAll = findViewById(R.id.tvViewAll);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        RecyclerView rvTransactions = findViewById(R.id.rvTransactions);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(transactionList);
        rvTransactions.setAdapter(adapter);

        findViewById(R.id.btnWithdraw).setOnClickListener(view ->
                Toast.makeText(this, "Withdraw Funds Clicked", Toast.LENGTH_SHORT).show());

        tvViewAll.setOnClickListener(view ->
                Toast.makeText(this, "View All Transactions Clicked", Toast.LENGTH_SHORT).show());

        findViewById(R.id.ivNotificationTop).setOnClickListener(view ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
        findViewById(R.id.navListingsItem).setOnClickListener(v -> {
            startActivity(new Intent(this, ListingsActivity.class));
            finish();
        });
    }

    private void fetchWalletSummary() {
        apiService.getWalletSummary(farmerId).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalletSummaryResponse data = response.body();
                    tvBalance.setText("LKR " + data.getAvailableBalance());
                    tvTrustScore.setText(data.getTrustScore());
                    tvPenalties.setText("Empty Trip Penalties: LKR " + data.getEmptyTripPenalties());
                }
            }
            @Override public void onFailure(@NonNull Call<WalletSummaryResponse> call, @NonNull Throwable t) {}
        });
    }

    private void fetchRecentTransactions() {
        apiService.getRecentTransactions(farmerId, 2).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    transactionList.clear();
                    transactionList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(View.GONE);
                } else {
                    addMockData();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) {
                addMockData();
            }
        });
    }

    private void addMockData() {
        transactionList.clear();
        transactionList.add(new Transaction("1", "Order #402 Settled", "Yesterday, 4:30 PM", "45,000", true));
        transactionList.add(new Transaction("2", "Order #398 Settled", "2 days ago", "22,000", true));
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
