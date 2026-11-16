package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma "Matchmaking Job Offer": payout (95% of the transport fee), distance, duration, route, cargo and the
 * 10-minute request countdown (decision 15 Nov). Accept → pickup screen; Decline → dispatch moves on.
 */
public class JobOfferActivity extends BaseTransporterOrderActivity {

    private TextView timer;
    private final Runnable tick = this::tick;

    @Override
    protected String title() {
        return "Job Offer";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout head = TUi.card(c, TUi.DARK, TUi.DARK);
        TUi.text(head, "● PRIORITY MATCH – sent to you as the nearest free vehicle", 12, TUi.SURFACE, true);
        timer = TUi.text(head, "", 18, Color.parseColor("#A7F3D0"), true);

        double payout = o.driverPayout();
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        double km = o.getDistanceKm() == null ? 0 : o.getDistanceKm();
        LinearLayout pay = TUi.card(c);
        TUi.caption(pay, "Payout offer");
        TUi.big(pay, rs(payout), TUi.DARK);
        TUi.pill(pay, "Guaranteed – held in escrow", TUi.SURFACE, TUi.DARK);
        TUi.row(pay, "Transport fee", rs(fee));
        TUi.row(pay, "5% platform commission", "-" + rs(fee - payout), TUi.RED, false);
        TUi.row(pay, "Total distance", TUi.km(km));
        TUi.row(pay, "Est. duration", duration(km));

        LinearLayout spec = TUi.card(c);
        TUi.title(spec, "Consignment");
        TUi.text(spec, cargo(o), 16, TUi.INK, true);
        if (o.getVehicleLabel() != null) TUi.body(spec, "Vehicle class: " + o.getVehicleLabel());
        if (o.getPickupDate() != null || o.getDeliveryDate() != null) {
            TUi.body(spec, "Delivery date: " + (o.getDeliveryDate() == null ? "—" : o.getDeliveryDate()));
        }
        TUi.route(spec, "Pickup – " + (o.getFarmerName() == null ? "farm" : o.getFarmerName()), o.getPickupAddress(),
                "Drop-off – " + (o.getBuyerName() == null ? "buyer" : o.getBuyerName()), o.getDropAddress());
        TUi.body(spec, "Take at least 2 pre-loading photos at the farm. Start Journey unlocks after the buyer approves them.");

        TUi.primary(actions, "✓  Accept Offer", v -> sendEvent("ACCEPT", "Accepted in the app", null, v));
        TUi.secondary(actions, "✕  Decline Offer", v -> new AlertDialog.Builder(this)
                .setTitle("Decline this job?")
                .setMessage("The request goes to the next nearest driver.")
                .setPositiveButton("Decline", (d, w) -> sendEvent("DECLINE", "Declined in the app", null, null))
                .setNegativeButton("Keep", null).show());
        tick();
    }

    @Override
    protected boolean handOver(Order o) {
        // after a decline / expiry the order belongs to someone else → back to the Jobs list
        if (!session.getUserId().equals(o.getTransporterId())) {
            startActivity(new Intent(this, TransporterJobsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return true;
        }
        return super.handOver(o);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void tick() {
        handler.removeCallbacks(tick);
        if (timer == null || order == null) return;
        timer.setText(countdown(order.getTransportRequestExpiresAt()));
        handler.postDelayed(tick, 1000);
    }

    /** "Expires in 07:42" from an epoch-ms deadline. */
    static String countdown(Long expiresAt) {
        if (expiresAt == null) return "Waiting for your answer";
        long left = Math.max(0, expiresAt - System.currentTimeMillis()) / 1000;
        if (left == 0) return "Expired – being sent to another driver";
        return String.format(Locale.US, "⏱ Expires in %02d:%02d", left / 60, left % 60);
    }

    /** Rough driving time at 40 km/h (MarketplaceRules.defaultTruckSpeedKmh). */
    static String duration(double km) {
        int min = (int) Math.round(km / 40.0 * 60);
        return min >= 60 ? String.format(Locale.US, "%dh %02dm", min / 60, min % 60) : min + " min";
    }
}
