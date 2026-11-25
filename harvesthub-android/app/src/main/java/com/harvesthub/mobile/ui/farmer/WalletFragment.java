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

import com.harvesthub.mobile.utils.SessionManager;

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
    private SessionManager sessionManager;
    private String farmerId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        sessionManager = new SessionManager(requireContext());
        farmerId = sessionManager.getUserId();

        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        initViews(view);
        fetchWalletSummary();
        fetchRecentTransactions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!farmerId.isEmpty()) {
            fetchWalletSummary();
            fetchRecentTransactions();
        }
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

        v.findViewById(R.id.btnWithdraw).setOnClickListener(view -> showWithdrawDialog());

        tvViewAll.setOnClickListener(view -> {
            showAll = !showAll;
            tvViewAll.setText(showAll ? "Show recent only" : "View all transactions");
            fetchRecentTransactions();
        });
    }

    private boolean showAll = false;
    private double balance = 0;
    private boolean suspended = false;

    /** Mock withdrawal: records a debit; no real bank transfer. */
    private void showWithdrawDialog() {
        if (suspended) {
            Toast.makeText(requireContext(), "Your balance is negative. Withdrawals are blocked until it is paid back.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(String.format(java.util.Locale.US, "Up to LKR %,.0f", balance));
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Withdraw Funds")
                .setMessage("Demo wallet: the amount is deducted here; no real bank transfer is made.")
                .setView(input)
                .setPositiveButton("Withdraw", (d, w) -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(input.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        amount = 0;
                    }
                    withdraw(amount);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void withdraw(double amount) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("amount", amount);
        apiService.withdraw(farmerId, body).enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Withdrawal recorded.", Toast.LENGTH_SHORT).show();
                    fetchWalletSummary();
                    fetchRecentTransactions();
                } else {
                    Toast.makeText(requireContext(), com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(response,
                            "Withdrawal not possible."), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWalletSummary() {
        apiService.getWalletSummary(farmerId).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    WalletSummaryResponse data = response.body();
                    balance = data.getBalance();
                    suspended = data.isSuspended();
                    tvBalance.setText("LKR " + data.getAvailableBalance());
                    tvBalance.setTextColor(suspended ? 0xFFDC2626 : tvPenalties.getCurrentTextColor());
                    // trust % and star rating
                    String rating = data.getRatingAverage() == null ? "no ratings yet"
                            : String.format(java.util.Locale.US, "%.1f / 5.0 (%d)", data.getRatingAverage(), data.getRatingCount());
                    tvTrustScore.setText(data.getTrustPercent() + "%  ·  " + rating);
                    tvPenalties.setText("Empty Trip Penalties: LKR " + data.getEmptyTripPenalties()
                            + (suspended ? "  ·  Account suspended (negative balance)" : ""));
                }
            }
            @Override public void onFailure(@NonNull Call<WalletSummaryResponse> call, @NonNull Throwable t) {}
        });
    }

    private void fetchRecentTransactions() {
        apiService.getRecentTransactions(farmerId, showAll ? 50 : 3).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> response) {
                if (!isAdded()) return;
                transactionList.clear();
                if (response.isSuccessful() && response.body() != null) transactionList.addAll(response.body());
                adapter.notifyDataSetChanged();
                tvEmptyState.setText("No transactions yet");
                tvEmptyState.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                tvEmptyState.setText("Can't reach the server.");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}
