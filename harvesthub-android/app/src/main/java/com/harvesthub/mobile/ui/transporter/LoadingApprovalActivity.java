package com.harvesthub.mobile.ui.transporter;

import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma "Buyer Approval" (photos submitted, Start Journey locked) and "Pre-Loading Photos Approved" (Start Journey).
 * Start Journey releases the 20% crop advance to the farmer (workflow 4.3) and starts background GPS.
 */
public class LoadingApprovalActivity extends BaseTransporterOrderActivity {

    @Override
    protected String title() {
        return "Buyer Approval";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        boolean approved = "LOADING_APPROVED".equals(o.getStatus());
        if (approved) {
            TUi.banner(c, TUi.SURFACE, TUi.DARK, "✓ Pre-Loading Photos Approved",
                    o.isLoadingAutoApproved() ? "Approved automatically – the buyer did not answer within 15 minutes."
                            : "Cleared by " + (o.getBuyerName() == null ? "the buyer" : o.getBuyerName()) + ". Load the goods.");
        } else {
            LinearLayout w = TUi.banner(c, TUi.DARK, android.graphics.Color.WHITE, "● Awaiting buyer photo approval…", null);
            Long dl = o.getLoadingApprovalDeadline();
            if (dl != null) {
                long min = Math.max(0, (dl - System.currentTimeMillis()) / 60000);
                TUi.text(w, String.format(Locale.US, "Auto-approved in %d min if the buyer does not answer", min), 13,
                        android.graphics.Color.parseColor("#A7F3D0"), false);
            }
        }

        LinearLayout ord = TUi.card(c);
        TUi.pill(ord, "ORDER " + ref(), TUi.SURFACE, TUi.DARK);
        TUi.text(ord, cargo(o), 17, TUi.INK, true);
        TUi.route(ord, "Pickup hub", (o.getFarmerName() == null ? "" : o.getFarmerName() + " – ") + (o.getPickupAddress() == null ? "" : o.getPickupAddress()),
                "Destination", (o.getBuyerName() == null ? "" : o.getBuyerName() + " – ") + (o.getDropAddress() == null ? "" : o.getDropAddress()));

        LinearLayout ev = TUi.card(c);
        TUi.title(ev, "Submitted Quality Evidence");
        TUi.pill(ev, "Uploaded & geotagged", TUi.SURFACE, TUi.DARK);
        TUi.photos(ev, o.getLoadingEvidenceUrls());
        if (o.getLoadingEvidenceAt() != null) TUi.row(ev, "Timestamp", time(o.getLoadingEvidenceAt()));
        Order.TransportEvent e = lastEvidence(o);
        if (e != null && e.latitude != null) TUi.row(ev, "GPS", String.format(Locale.US, "%.4f° N, %.4f° E", e.latitude, e.longitude));

        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        LinearLayout pay = TUi.card(c);
        TUi.row(pay, "Gross freight agreement", rs(fee));
        TUi.row(pay, "Platform dispatch fee (5%)", "-" + rs(fee - o.driverPayout()), TUi.RED, false);
        TUi.row(pay, "Net payout – guaranteed on delivery", rs(o.driverPayout()), TUi.DARK, true);

        if (approved) {
            LinearLayout note = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
            TUi.text(note, "Smart contract notice", 14, TUi.DARK, true);
            TUi.body(note, "Starting the journey releases the 20% crop advance to the farmer"
                    + (o.getPayment() == null ? "" : " (" + rs(o.getPayment().advanceCropPortion) + ")")
                    + " and shares your live location with the buyer.");
            TUi.primary(actions, "▲  Start Journey", v -> sendEvent("START_JOURNEY", "Journey started", null, v));
        } else {
            TUi.secondary(actions, "🔒  Start Journey (locked until approved)", null).setEnabled(false);
        }
    }

    @Override
    protected void onEventDone(String event, Order updated) {
        if ("START_JOURNEY".equals(event)) TransporterTrackingService.start(this);
        super.onEventDone(event, updated);
    }

    private static Order.TransportEvent lastEvidence(Order o) {
        if (o.getTransportEvents() == null) return null;
        Order.TransportEvent last = null;
        for (Order.TransportEvent e : o.getTransportEvents()) if ("LOADING_EVIDENCE_SUBMITTED".equals(e.eventType)) last = e;
        return last;
    }
}
