package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.net.Uri;

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
 * "Dispute & Refund Resolution" screen for rejected orders:
 * PRELOAD_REJECTED — 100% refund, the farmer pays the Empty Trip Fee to the transporter;
 * RETURN_JOURNEY — 100% refund, the transporter pays the crop advance and earns no delivery fee.
 * Amounts come from the buyer's real refund transactions.
 */
public class DisputeResolvedActivity extends BaseBuyerOrderActivity {

    private boolean txnLoaded;

    @Override
    protected int layout() {
        return R.layout.activity_dispute_resolved;
    }

    @Override
    protected void bind(@NonNull Order o) {
        boolean preload = "PRELOAD_REJECTED".equals(o.getStatus());
        Order.Payment p = o.getPayment();
        setText(R.id.tvHeaderTitle, "Order Rejected – Refunded");
        setText(R.id.tvHeaderSubtitle, preload ? "You rejected the crop photos before loading."
                : "You rejected the delivery because of transit damage.");
        setText(R.id.tvDisputeStatus, preload ? "System Status: Rejected before loading" : "System Status: Return Journey");
        if (p != null) setText(R.id.tvRefundAmount, rs(p.advanceDue + (p.balancePaid ? p.balanceDue : 0)));
        setText(R.id.tvRefundPolicy, "Full refund including the 3% fee. Reason: "
                + (p != null && p.settlementNote != null ? p.settlementNote : "not given"));
        if (preload) {
            setText(R.id.tvLiabilityTitle, "Farmer Liability (Empty Trip)");
            setText(R.id.tvLiabilityBody, String.format(Locale.US,
                    "The crop did not match the order. The farmer pays the Empty Trip Fee of %s to the transporter for the wasted trip.",
                    p == null ? "-" : rs(p.emptyTripFee > 0 ? p.emptyTripFee : p.transportFee)));
            setText(R.id.tvReturnRoute, "No goods were loaded");
            setText(R.id.tvReturnState, "");
            setText(R.id.tvFarmerOutcome, "The listing is closed. The farmer receives no advance.");
        } else {
            setText(R.id.tvLiabilityTitle, "Transporter Liability Protocol");
            setText(R.id.tvLiabilityBody, String.format(Locale.US,
                    "The goods were damaged in transit. The transporter earns no delivery fee and pays %s for the crop.",
                    p == null ? "-" : rs(p.advanceCropPortion)));
            setText(R.id.tvReturnRoute, "Route: " + (o.getDropAddress() == null ? "Your depot" : o.getDropAddress())
                    + " ➔ " + (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()));
            setText(R.id.tvReturnState, "(Return journey)");
            setText(R.id.tvFarmerOutcome, "The farmer keeps the 20% advance to cover the damaged crop.");
        }
        findViewById(R.id.btnReturnDashboard).setOnClickListener(v -> {
            Intent i = new Intent(this, BuyerDashboardActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
        findViewById(R.id.tvContactSupport).setOnClickListener(v -> {
            Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@harvesthubapp.tech"));
            mail.putExtra(Intent.EXTRA_SUBJECT, "Order " + o.getId());
            try {
                startActivity(mail);
            } catch (Exception e) {
                android.widget.Toast.makeText(this, "No email app found.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        if (!txnLoaded) loadRefunds(o);
    }

    private void loadRefunds(Order o) {
        txnLoaded = true;
        api.getRecentTransactions(session.getUserId(), 100).enqueue(new Callback<List<Transaction>>() {
            @Override
            public void onResponse(@NonNull Call<List<Transaction>> call, @NonNull Response<List<Transaction>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                double total = 0;
                String firstId = null;
                for (Transaction t : r.body()) {
                    if (o.getId().equals(t.getOrderId()) && t.isCredit()
                            && ("REFUND".equals(t.getType()) || "DAMAGE_CHARGE".equals(t.getType()))) {
                        total += t.getAmountValue();
                        if (firstId == null) firstId = t.getId();
                    }
                }
                if (total > 0) setText(R.id.tvRefundAmount, rs(total));
                if (firstId != null) {
                    setText(R.id.tvRefundId, "Credited to your wallet • ID: "
                            + firstId.substring(Math.max(0, firstId.length() - 8)).toUpperCase(Locale.US));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Transaction>> call, @NonNull Throwable t) { }
        });
    }
}
