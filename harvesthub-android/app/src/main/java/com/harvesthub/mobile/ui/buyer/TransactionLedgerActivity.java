package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Transaction;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer 4.3 "Payment Processing Ledger". Shows the advance held in escrow and the real dispatch state
 * (DISPATCHING → TRANSPORTER_ASSIGNED, or PAUSED when no vehicle is free; the server retries every 2 minutes).
 * Moves on by itself when the driver accepts.
 */
public class TransactionLedgerActivity extends BaseBuyerOrderActivity {

    private boolean txnLoaded;

    @Override
    protected int layout() {
        return R.layout.activity_transaction_ledger;
    }

    @Override
    protected void bind(@NonNull Order o) {
        Order.Payment p = o.getPayment();
        if (p != null) setText(R.id.tvSecuredAmount, rs(p.advanceDue));
        String method = "WALLET".equals(o.getAdvancePaymentMethod()) ? "wallet" : "card";
        setText(R.id.tvStep1Title, "Payment Verified");
        String paidAt = o.reachedAt("DISPATCHING");
        setText(R.id.tvStep1Time, paidAt == null ? "" : time(paidAt));
        setText(R.id.tvStep1Subtitle, "Advance paid by " + method + " and locked in escrow");
        setText(R.id.tvStep2Title, "Selecting " + (o.getVehicleLabel() == null ? "a vehicle" : o.getVehicleLabel()));
        setText(R.id.tvStep2Subtitle, String.format(Locale.US, "%s, %s",
                kg(o.getQuantity()), o.getDistanceKm() == null ? "" : String.format(Locale.US, "%.1f km route", o.getDistanceKm())));

        switch (o.getStatus()) {
            case "PAUSED":
                setText(R.id.tvSystemStatus, "System Status: Waiting for a free vehicle");
                setText(R.id.tvStep3Title, "No transporter is free nearby");
                setText(R.id.tvStep3Subtitle, "We retry automatically every 2 minutes. Your money stays in escrow.");
                break;
            case "TRANSPORTER_ASSIGNED":
                setText(R.id.tvSystemStatus, "System Status: Waiting for the driver to accept");
                setText(R.id.tvStep3Title, "Request sent to " + (o.getTransporterName() == null ? "the nearest driver" : o.getTransporterName()));
                setText(R.id.tvStep3Subtitle, vehicleLine(o) + ". If they decline, the next nearest driver is asked.");
                break;
            default:
                setText(R.id.tvSystemStatus, "System Status: Payment Processing / Dispatching");
                setText(R.id.tvStep3Title, "Contacting nearest available\nTransporters...");
                setText(R.id.tvStep3Subtitle, "Matching vehicle size and distance to the farm");
        }
        View spinner = findViewById(R.id.iconStepSpinner);
        if (spinner != null && spinner.getAnimation() == null) {
            android.view.animation.RotateAnimation r = new android.view.animation.RotateAnimation(0, 360,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
            r.setDuration(1200);
            r.setRepeatCount(android.view.animation.Animation.INFINITE);
            spinner.startAnimation(r);
        }
        findViewById(R.id.btnReturnToOrders).setOnClickListener(v -> {
            Intent i = new Intent(this, BuyerDashboardActivity.class);
            i.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.ORDERS.name());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
        if (!txnLoaded) loadTxn(o);
    }

    private void loadTxn(Order o) {
        txnLoaded = true;
        api.getRecentTransactions(session.getUserId(), 50).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                for (Transaction t : r.body()) {
                    if (o.getId().equals(t.getOrderId()) && "ADVANCE_PAYMENT".equals(t.getType()) && t.getId() != null) {
                        String id = t.getId();
                        setText(R.id.tvTxnRef, "Txn #" + id.substring(Math.max(0, id.length() - 8)).toUpperCase(Locale.US));
                        return;
                    }
                }
                setText(R.id.tvTxnRef, "Order " + ref(o));
            }

            @Override
            public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) { }
        });
    }
}
