package com.harvesthub.mobile.ui.transporter;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Transaction;
import com.harvesthub.mobile.model.WalletSummaryResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Driver wallet (Home "Wallet" button). Mock wallet only (proposal §4.2): balance, ledger, mock withdrawal,
 * and a mock top-up that pays back a negative balance (decision 15 Nov). No bank accounts.
 */
public class TransporterWalletActivity extends AppCompatActivity {

    private LinearLayout content;
    private String me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, "Wallet", null);
        me = new SessionManager(this).getUserId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.getApiService().getWalletSummary(me).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    TUi.body(content, "Could not load the wallet.");
                    return;
                }
                WalletSummaryResponse w = r.body();
                ApiClient.getApiService().getRecentTransactions(me, 30).enqueue(new Callback<List<Transaction>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> r2) {
                        render(w, r2.isSuccessful() ? r2.body() : null);
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) {
                        render(w, null);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<WalletSummaryResponse> call, @NonNull Throwable t) {
                Toast.makeText(TransporterWalletActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(WalletSummaryResponse w, List<Transaction> txns) {
        content.removeAllViews();
        LinearLayout b = TUi.card(content);
        TUi.caption(b, "Available balance");
        TUi.big(b, TUi.rs(w.getBalance()) + " LKR", w.getBalance() < 0 ? TUi.RED : TUi.DARK);
        if (w.isSuspended()) {
            TUi.pill(b, "Suspended – balance below zero", TUi.RED_BG, TUi.RED);
            TUi.primary(b, "Pay back " + TUi.rs(-w.getBalance()), v -> amountDialog("Pay back (mock top-up)", -w.getBalance(), true));
        } else {
            TUi.primary(b, "Withdraw (mock)", v -> amountDialog("Withdraw to your account", w.getBalance(), false));
        }
        TUi.secondary(b, "Add funds (mock top-up)", v -> amountDialog("Add funds", 0, true));

        TUi.title(content, "Transactions").setPadding(0, TUi.dp(this, 16), 0, 0);
        if (txns == null || txns.isEmpty()) {
            TUi.body(content, "No transactions yet.");
            return;
        }
        LinearLayout list = TUi.card(content);
        for (Transaction t : txns) {
            double amt = t.isCredit() ? t.getAmountValue() : -t.getAmountValue();
            TUi.row(list, t.getTitle() + "\n" + com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(t.getTimestamp()),
                    TUi.signedRs(amt), amt >= 0 ? TUi.ACCENT : TUi.RED, false);
        }
    }

    private void amountDialog(String title, double suggested, boolean deposit) {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (suggested > 0) in.setText(String.valueOf(Math.round(suggested * 100) / 100.0));
        in.setHint("Amount in LKR");
        new AlertDialog.Builder(this).setTitle(title).setView(in)
                .setPositiveButton("Confirm", (d, x) -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(in.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter an amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("amount", amount);
                    Call<Transaction> call = deposit ? ApiClient.getApiService().deposit(me, body) : ApiClient.getApiService().withdraw(me, body);
                    call.enqueue(new Callback<Transaction>() {
                        @Override
                        public void onResponse(@NonNull Call<Transaction> c, @NonNull Response<Transaction> r) {
                            Toast.makeText(TransporterWalletActivity.this, r.isSuccessful() ? "Done." : AuthInputUtils.errorMessage(r, "Not possible."), Toast.LENGTH_LONG).show();
                            load();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Transaction> c, @NonNull Throwable t) {
                            Toast.makeText(TransporterWalletActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
    }
}
