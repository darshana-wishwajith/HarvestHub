package com.harvesthub.mobile.ui.buyer;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Transaction;
import com.harvesthub.mobile.model.WalletSummaryResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Buyer Wallet Page" screen. Mock wallet: balance, money locked in escrow,
 * today's inflow, escrow released, activity with an "Holds" filter. Deposit Funds = simulated top-up used for
 * wallet payments; Withdraw = mock withdrawal.
 */
public class BuyerWalletActivity extends AppCompatActivity {

    /** Ledger entries that are money the buyer put into escrow. */
    static final List<String> HOLD_TYPES = Arrays.asList("ADVANCE_PAYMENT", "BALANCE_PAYMENT");

    private ApiService api;
    private String me;
    private List<Transaction> all = new ArrayList<>();
    private boolean holdsOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_wallet);
        api = ApiClient.getApiService();
        me = new SessionManager(this).getUserId();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDeposit).setOnClickListener(v -> showTopUp(this, this::load));
        findViewById(R.id.btnWithdraw).setOnClickListener(v -> showWithdraw());
        findViewById(R.id.tabAll).setOnClickListener(v -> filter(false));
        findViewById(R.id.tabHolds).setOnClickListener(v -> filter(true));
        ((SwipeRefreshLayout) findViewById(R.id.swipeWallet)).setOnRefreshListener(this::load);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (me == null) return;
        api.getWalletSummary(me).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> r) {
                ((SwipeRefreshLayout) findViewById(R.id.swipeWallet)).setRefreshing(false);
                if (!r.isSuccessful() || r.body() == null) return;
                WalletSummaryResponse w = r.body();
                text(R.id.tvWalletBalance, BaseBuyerOrderActivity.rs(w.getBalance()));
                text(R.id.tvWalletLocked, "🔒 " + BaseBuyerOrderActivity.rs(w.getEscrowHeld()) + " currently locked in escrow");
                text(R.id.tvWalletState, w.isSuspended() ? "● Suspended" : "● Live Escrow");
                text(R.id.tvTodayInflow, BaseBuyerOrderActivity.rs(w.getTodayInflow()));
                text(R.id.tvTodayInflowCount, w.getTodayInflowCount() == 0 ? "No credits today"
                        : w.getTodayInflowCount() + (w.getTodayInflowCount() == 1 ? " credit" : " credits"));
                text(R.id.tvEscrowReleased, BaseBuyerOrderActivity.rs(w.getEscrowReleased()));
            }

            @Override
            public void onFailure(@NonNull Call<WalletSummaryResponse> call, @NonNull Throwable t) {
                ((SwipeRefreshLayout) findViewById(R.id.swipeWallet)).setRefreshing(false);
                Toast.makeText(BuyerWalletActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
        api.getRecentTransactions(me, 50).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                all = r.body();
                long holds = 0;
                for (Transaction t : all) if (HOLD_TYPES.contains(t.getType())) holds++;
                text(R.id.tabHolds, "Holds (" + holds + ")");
                render();
            }

            @Override
            public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) { }
        });
    }

    private void filter(boolean holds) {
        holdsOnly = holds;
        findViewById(R.id.tabAll).setBackgroundResource(holds ? 0 : R.drawable.bg_badge_light_green);
        findViewById(R.id.tabHolds).setBackgroundResource(holds ? R.drawable.bg_badge_light_green : 0);
        render();
    }

    private void render() {
        LinearLayout rows = findViewById(R.id.layoutWalletRows);
        rows.removeAllViews();
        int n = 0;
        for (Transaction t : all) {
            if (holdsOnly && !HOLD_TYPES.contains(t.getType())) continue;
            rows.addView(row(t));
            n++;
        }
        findViewById(R.id.tvWalletEmpty).setVisibility(n == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private LinearLayout row(Transaction t) {
        int pad = dp(10);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, pad, 0, pad);
        row.setGravity(Gravity.CENTER_VERTICAL);

        boolean hold = HOLD_TYPES.contains(t.getType());
        com.harvesthub.mobile.ui.transporter.TUi.iconTile(row, t.isCredit() ? R.drawable.ic_banknote : hold ? R.drawable.ic_lock : R.drawable.ic_credit_card,
                38, t.isCredit() ? 0xFFDCFCE7 : hold ? 0xFFFEF3C7 : 0xFFFDE2E2, t.isCredit() ? 0xFF15803D : hold ? 0xFFB45309 : 0xFFB91C1C, true);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setPadding(dp(12), 0, dp(8), 0);
        TextView title = new TextView(this);
        title.setText(t.getTitle());
        title.setTextColor(0xFF111827);
        title.setTextSize(14);
        title.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(title.getContext()));
        TextView sub = new TextView(this);
        sub.setText(label(t.getType()) + " • " + BaseBuyerOrderActivity.time(t.getTimestamp() == null ? null : t.getTimestamp().replace(' ', 'T')));
        sub.setTextColor(0xFF64748B);
        sub.setTextSize(12);
        left.addView(title);
        left.addView(sub);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView amount = new TextView(this);
        double v = t.getAmountValue();
        amount.setText((t.isCredit() ? "+" : "−") + BaseBuyerOrderActivity.rs(v));
        amount.setTextColor(t.isCredit() ? 0xFF2E7D32 : HOLD_TYPES.contains(t.getType()) ? 0xFFB45309 : 0xFFB91C1C);
        amount.setTextSize(14);
        amount.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(amount.getContext()));
        row.addView(amount);
        return row;
    }

    private static String label(String type) {
        if (type == null) return "Wallet";
        switch (type) {
            case "ADVANCE_PAYMENT": return "Escrow hold (advance)";
            case "BALANCE_PAYMENT": return "Escrow hold (80% balance)";
            case "REFUND": return "Refund";
            case "DAMAGE_CHARGE": return "Damage compensation";
            case "TOP_UP": return "Top-up";
            case "WITHDRAWAL": return "Withdrawal";
            default: return type.replace('_', ' ').toLowerCase();
        }
    }

    private void showWithdraw() {
        EditText in = amountInput();
        new AlertDialog.Builder(this).setTitle("Withdraw (simulated)").setMessage("Moves money out of your HarvestHub wallet. No real bank transfer.")
                .setView(wrap(in))
                .setPositiveButton("Withdraw", (d, w) -> {
                    double a = parse(in.getText().toString());
                    Map<String, Object> body = new HashMap<>();
                    body.put("amount", a);
                    api.withdraw(me, body).enqueue(new Callback<Transaction>() {
                        @Override
                        public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> r) {
                            Toast.makeText(BuyerWalletActivity.this, r.isSuccessful() ? "Withdrawal recorded."
                                    : AuthInputUtils.errorMessage(r, "Withdrawal failed."), Toast.LENGTH_LONG).show();
                            load();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable t) {
                            Toast.makeText(BuyerWalletActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
    }

    /** Simulated card top-up, shared with the checkout screen. */
    static void showTopUp(Activity a, Runnable after) {
        EditText in = new EditText(a);
        in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        in.setHint("Amount in Rs.");
        LinearLayout box = new LinearLayout(a);
        int p = Math.round(20 * a.getResources().getDisplayMetrics().density);
        box.setPadding(p, p / 2, p, 0);
        box.addView(in, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        new AlertDialog.Builder(a).setTitle("Deposit funds (simulated card)")
                .setMessage("Adds money to your HarvestHub wallet. Sandbox only: no real card is charged.")
                .setView(box)
                .setPositiveButton("Deposit", (d, w) -> {
                    double amount = parse(in.getText().toString());
                    if (amount <= 0) {
                        Toast.makeText(a, "Enter an amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("amount", amount);
                    String me = new SessionManager(a).getUserId();
                    ApiClient.getApiService().deposit(me, body).enqueue(new Callback<Transaction>() {
                        @Override
                        public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> r) {
                            Toast.makeText(a, r.isSuccessful() ? BaseBuyerOrderActivity.rs(amount) + " added to your wallet."
                                    : AuthInputUtils.errorMessage(r, "Top-up failed."), Toast.LENGTH_LONG).show();
                            if (after != null) after.run();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable t) {
                            Toast.makeText(a, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
    }

    private EditText amountInput() {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        in.setHint("Amount in Rs.");
        return in;
    }

    private LinearLayout wrap(EditText in) {
        LinearLayout box = new LinearLayout(this);
        int p = dp(20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(in, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    static double parse(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void text(int id, String s) {
        TextView v = findViewById(id);
        if (v != null) v.setText(s);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
