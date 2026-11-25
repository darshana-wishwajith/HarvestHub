package com.harvesthub.mobile.ui.transporter;

import android.widget.TextView;
import android.graphics.Color;
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
 * Driver wallet (Home "Wallet" button). Mock wallet only: balance, ledger, mock withdrawal,
 * and a mock top-up that pays back a negative balance. No bank accounts.
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
        int green = Color.parseColor("#15803D");
        LinearLayout b = TUi.card(content);
        LinearLayout bh = TUi.hrow(b, 0);
        TUi.fill(bh, "Available Payout Balance", 13, TUi.MUTED, false).setTypeface(TUi.semi(this));
        TUi.tag(bh, w.isSuspended() ? "Suspended" : "Mock Wallet", w.isSuspended() ? TUi.RED_BG : TUi.MINT, w.isSuspended() ? TUi.RED : TUi.MINT_INK);
        LinearLayout br = TUi.hrow(b, 2);
        br.setGravity(android.view.Gravity.BOTTOM);
        TUi.label(br, TUi.rs(w.getBalance()), 32, w.getBalance() < 0 ? TUi.RED : TUi.INK, true);
        TUi.label(br, "  LKR", 12, TUi.DARK, true).setPadding(0, 0, 0, TUi.dp(this, 6));
        if (w.isSuspended()) {
            TUi.iconButton(b, "Pay Back " + TUi.rs(-w.getBalance()), R.drawable.ic_bolt_green, TUi.RED, Color.WHITE,
                    v -> amountDialog("Pay back (mock top-up)", -w.getBalance(), true));
        } else {
            TUi.iconButton(b, "Withdraw (Mock)", R.drawable.ic_bolt_green, TUi.DARK, Color.WHITE,
                    v -> amountDialog("Withdraw to your account", w.getBalance(), false));
        }
        TUi.iconButton(b, "Add Funds (Mock Top-up)", R.drawable.ic_add_circle, Color.parseColor("#E9E7E5"), TUi.INK, v -> amountDialog("Add funds", 0, true));

        TUi.sectionTitle(content, "Transactions", null);
        if (txns == null || txns.isEmpty()) {
            TUi.body(content, "No transactions yet.");
            return;
        }
        for (Transaction t : txns) {
            double amt = t.isCredit() ? t.getAmountValue() : -t.getAmountValue();
            boolean plus = amt >= 0;
            LinearLayout c = TUi.card(content);
            ((LinearLayout.LayoutParams) ((android.view.View) c.getParent()).getLayoutParams()).topMargin = TUi.dp(this, 10);
            LinearLayout r = TUi.hrow(c, 0);
            TUi.iconTile(r, plus ? R.drawable.ic_banknote : R.drawable.ic_credit_card, 40, plus ? TUi.MINT : TUi.RED_BG, plus ? TUi.DARK : TUi.RED, true);
            LinearLayout tt = TUi.col(r, 1f);
            tt.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
            TextView title = TUi.text(tt, t.getTitle(), 14, TUi.INK, true);
            title.setMaxLines(2);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TUi.text(tt, com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(t.getTimestamp()), 12, TUi.MUTED, false);
            TUi.label(r, TUi.signedRs(amt), 15, plus ? green : TUi.RED, true);
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
