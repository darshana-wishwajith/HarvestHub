package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class WalletFragment extends Fragment {

    private TextView tvBalance, tvTrustScore, tvPenalties, tvViewAll, tvEmptyState;
    private TransactionAdapter adapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private ApiService apiService;
    private final String farmerId = "FARMER_123";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews(view);
        fetchWalletSummary();
        fetchRecentTransactions();

        return view;
    }

    private void initViews(View v) {
        tvBalance = v.findViewById(R.id.tvBalance);
        tvTrustScore = v.findViewById(R.id.tvTrustScore);
        tvPenalties = v.findViewById(R.id.tvPenalties);
        tvViewAll = v.findViewById(R.id.tvViewAll);
        tvEmptyState = v.findViewById(R.id.tvEmptyState);
        RecyclerView rvTransactions = v.findViewById(R.id.rvTransactions);

        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TransactionAdapter(transactionList);
        rvTransactions.setAdapter(adapter);

        v.findViewById(R.id.btnWithdraw).setOnClickListener(view ->
                Toast.makeText(requireContext(), "Withdraw Funds Clicked", Toast.LENGTH_SHORT).show());

        tvViewAll.setOnClickListener(view ->
                Toast.makeText(requireContext(), "View All Transactions Clicked", Toast.LENGTH_SHORT).show());
    }

    private void fetchWalletSummary() {
        apiService.getWalletSummary(farmerId).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
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
                if (!isAdded()) return;
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
                if (isAdded()) addMockData();
            }
        });
    }

    private void addMockData() {
        transactionList.clear();
        transactionList.add(new Transaction("1", "Order #402 Settled", "Yesterday, 4:30 PM", "45,000", true));
        transactionList.add(new Transaction("2", "Order #398 Settled", "2 days ago", "22,000", true));
        if (adapter != null) adapter.notifyDataSetChanged();
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}