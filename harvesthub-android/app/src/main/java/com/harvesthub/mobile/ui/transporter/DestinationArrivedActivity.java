package com.harvesthub.mobile.ui.transporter;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma "Arrived at destination" (buyer inspecting) and "2-Way Verification Handshake" (workflow 5.1–5.2):
 * ARRIVED_AT_DESTINATION → wait for the buyer to accept (pays 80%) or reject;
 * BALANCE_PAID / DELIVERED → driver taps Delivery Completed; buyer taps Goods Received; both → COMPLETED.
 */
public class DestinationArrivedActivity extends BaseTransporterOrderActivity {

    @Override
    protected String title() {
        return "Delivery";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        String s = o.getStatus();
        boolean inspecting = "ARRIVED_AT_DESTINATION".equals(s);
        boolean driverDone = o.isTransporterConfirmedDelivery();
        boolean buyerDone = o.isBuyerConfirmedReceipt();

        if (inspecting) {
            TUi.banner(c, TUi.DARK, android.graphics.Color.WHITE, "Arrived • Buyer inspecting the goods",
                    "Arrival recorded " + time(o.getActualDeliveryDate()) + ". Wait while " + (o.getBuyerName() == null ? "the buyer" : o.getBuyerName())
                            + " compares the goods with the pre-loading photos and accepts or rejects the delivery.");
        } else {
            TUi.pill(c, "● Physical handshake in progress", TUi.SURFACE, TUi.DARK);
            LinearLayout h = TUi.card(c);
            TUi.title(h, "2-Way Verification Handshake");
            TUi.body(h, "Both parties confirm the handover. Settlement runs when both have confirmed.");
            TUi.step(h, "Buyer accepted the goods and paid the 80% balance", 2, "Paid");
            TUi.step(h, "Buyer action: Goods Received", buyerDone ? 2 : 0, buyerDone ? "Confirmed" : "Pending");
            TUi.step(h, "Your action: Unloading complete", driverDone ? 2 : 1, driverDone ? "Confirmed" : "Your sign-off");
        }

        LinearLayout m = TUi.card(c);
        TUi.caption(m, "Shipment manifest " + ref());
        TUi.text(m, cargo(o), 16, TUi.INK, true);
        TUi.route(m, "Origin", o.getPickupAddress(), "Current destination", o.getDropAddress());

        LinearLayout pay = TUi.card(c, TUi.SURFACE, TUi.SURFACE);
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        TUi.text(pay, "🔒 Escrow final payout protected", 15, TUi.DARK, true);
        TUi.row(pay, "Gross freight fee", rs(fee));
        TUi.row(pay, "Platform commission (5%)", "-" + rs(fee - o.driverPayout()), TUi.RED, false);
        TUi.row(pay, "Net transporter payout", rs(o.driverPayout()), TUi.DARK, true);
        TUi.body(pay, "Credited to your HarvestHub wallet when both confirmations are in.");

        if (inspecting) {
            TUi.secondary(actions, "Waiting for the buyer's decision…", null).setEnabled(false);
        } else if (!driverDone) {
            TUi.primary(actions, "✓✓  Delivery Completed", v -> sendEvent("DELIVERY_COMPLETED", "Unloaded at the buyer", null, v));
        } else {
            TUi.secondary(actions, "Waiting for the buyer to confirm Goods Received…", null).setEnabled(false);
        }
        if (o.isDisputeOpen()) TUi.pill(c, String.format(Locale.US, "An issue was reported on this order – an officer will review it."), TUi.AMBER_BG, TUi.AMBER);
    }
}
