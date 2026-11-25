package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.model.Transaction;
import com.harvesthub.mobile.model.TransporterDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Suspended driver. Clear it with a mock
 * "Pay now" top-up or from future earnings. Suspended drivers receive no job requests.
 */
public class AccountSuspendedActivity extends AppCompatActivity {

    private LinearLayout content;
    private String me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, "Account Suspended", null);
        me = new SessionManager(this).getUserId();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ApiClient.getApiService().getTransporterDashboard(me).enqueue(new Callback<TransporterDashboard>() {
            @Override
            public void onResponse(@NonNull Call<TransporterDashboard> call, @NonNull Response<TransporterDashboard> r) {
                if (r.isSuccessful() && r.body() != null) render(r.body());
            }

            @Override
            public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                TUi.body(content, "Can't reach the server.");
            }
        });
    }

    private void render(TransporterDashboard d) {
        content.removeAllViews();
        if (!d.suspended) {
            TUi.notice(content, R.drawable.ic_check, Color.parseColor("#D7F5DD"), Color.parseColor("#15803D"), "Your Account Is Active",
                    "Your wallet balance is " + TUi.rs(d.balance) + ". You can receive job requests again.");
            TUi.iconButton(content, "Back to Home", R.drawable.transporter_ic_bottom_home, TUi.DARK, Color.WHITE, v -> finish());
            return;
        }
        LinearLayout b = TUi.card(content, TUi.RED_BG, TUi.RED_BG);
        LinearLayout hr = TUi.hrow(b, 0);
        hr.setGravity(android.view.Gravity.TOP);
        TUi.iconTile(hr, R.drawable.ic_wallet_frozen, 42, TUi.RED, Color.WHITE, true);
        LinearLayout ht = TUi.col(hr, 1f);
        ht.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.pill(ht, "\u26A0 ACCOUNT LOCKED", Color.WHITE, TUi.RED).setTextSize(10);
        TUi.text(ht, "Wallet Balance Below Zero", 19, TUi.RED, true);
        TUi.text(ht, "A damage charge for a rejected delivery took your wallet below zero. You get no new job requests until it is paid back.",
                13, TUi.RED, false);
        LinearLayout owed = TUi.box(b, Color.WHITE, 12);
        TUi.text(owed, "AMOUNT TO PAY BACK", 11, TUi.MUTED, true);
        TUi.text(owed, "-" + TUi.rs(d.amountOwed), 28, TUi.RED, true);
        TUi.row(owed, "Damage charges (all time)", "-" + TUi.rs(d.damageChargesTotal), TUi.RED, false);

        LinearLayout h = TUi.card(content, TUi.GREY_BG, TUi.GREY_BG);
        TUi.iconHeader(h, R.drawable.ic_mandatory_protocol, "How to Reactivate", null, 0, 0);
        TUi.numStep(h, 1, "Pay Back Now", "Use a mock top-up to bring your wallet back to zero.", false);
        TUi.numStep(h, 2, "Or Wait for Payouts", "Payouts from trips still in progress are credited to the wallet too.", false);
        TUi.numStep(h, 3, "Disagree?", "Appeal from the rejected trip in History; an officer reviews it.", false);

        TUi.iconButton(content, "Pay " + TUi.rs(d.amountOwed) + " Now", R.drawable.ic_bolt_green, TUi.DARK, Color.WHITE, v -> new AlertDialog.Builder(this)
                .setTitle("Pay back " + TUi.rs(d.amountOwed) + "?")
                .setMessage("Mock top-up (no real money).")
                .setPositiveButton("Pay", (x, y) -> pay(d.amountOwed))
                .setNegativeButton("Cancel", null).show());
        TUi.iconButton(content, "View Trip History", R.drawable.transporter_ic_bottom_history, Color.parseColor("#E9E7E5"), TUi.INK,
                v -> startActivity(new Intent(this, JobHistoryActivity.class)));
    }

    private void pay(double amount) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", Math.ceil(amount * 100) / 100.0);
        ApiClient.getApiService().deposit(me, body).enqueue(new Callback<Transaction>() {
            @Override
            public void onResponse(@NonNull Call<Transaction> call, @NonNull Response<Transaction> r) {
                Toast.makeText(AccountSuspendedActivity.this, r.isSuccessful() ? "Paid. Your account is active again."
                        : AuthInputUtils.errorMessage(r, "Payment failed."), Toast.LENGTH_LONG).show();
                onResume();
            }

            @Override
            public void onFailure(@NonNull Call<Transaction> call, @NonNull Throwable t) {
                Toast.makeText(AccountSuspendedActivity.this, "Can't reach the server.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
