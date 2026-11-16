package com.harvesthub.mobile.ui.transporter;

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
 * Suspended driver (workflow 5.3: negative wallet after a damage charge). Decision 15 Nov: clear it with a mock
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
            TUi.banner(content, TUi.SURFACE, TUi.DARK, "✓ Your account is active", "Your wallet balance is " + TUi.rs(d.balance) + ".");
            TUi.primary(content, "Back to Home", v -> finish());
            return;
        }
        LinearLayout b = TUi.banner(content, TUi.RED_BG, TUi.RED, "Account locked – balance below zero",
                "A damage charge for a rejected delivery took your wallet below zero. You get no new job requests until it is paid back.");
        TUi.big(b, "-" + TUi.rs(d.amountOwed), TUi.RED);
        TUi.row(b, "Damage charges (all time)", "-" + TUi.rs(d.damageChargesTotal), TUi.RED, false);

        LinearLayout h = TUi.card(content);
        TUi.title(h, "How to reactivate");
        TUi.step(h, "Pay back now with a mock top-up", 1, null);
        TUi.step(h, "Or wait: payouts from trips still in progress are credited too", 0, null);
        TUi.step(h, "Disagree? Appeal from the rejected trip in History", 0, null);

        TUi.primary(content, "Pay " + TUi.rs(d.amountOwed) + " now", v -> new AlertDialog.Builder(this)
                .setTitle("Pay back " + TUi.rs(d.amountOwed) + "?")
                .setMessage("Mock top-up (no real money).")
                .setPositiveButton("Pay", (x, y) -> pay(d.amountOwed))
                .setNegativeButton("Cancel", null).show());
        TUi.secondary(content, "View trip history", v -> startActivity(new Intent(this, JobHistoryActivity.class)));
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
