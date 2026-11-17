package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
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
        return order != null && "LOADING_APPROVED".equals(order.getStatus()) ? "Ready for Departure" : "Buyer Approval";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        boolean approved = "LOADING_APPROVED".equals(o.getStatus());
        int green = Color.parseColor("#15803D");
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        Order.TransportEvent e = lastEvidence(o);
        String buyer = o.getBuyerName() == null ? "the buyer" : o.getBuyerName();

        if (!approved) {
            LinearLayout w = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
            LinearLayout strip = TUi.box(w, TUi.DARK, 0);
            strip.setBackground(TUi.round(TUi.DARK, TUi.dp(this, 40)));
            TUi.text(strip, "\u25CF  Awaiting Buyer Remote Photo Approval\u2026", 13, Color.WHITE, true);
            LinearLayout pb = TUi.box(w, Color.WHITE, 12);
            LinearLayout ph = TUi.hrow(pb, 0);
            TUi.icon(ph, R.drawable.ic_shield_check, 18, TUi.INK);
            TextView pt = TUi.fill(ph, "Quality Evidence Verification in Progress", 13, TUi.INK, false);
            ((LinearLayout.LayoutParams) pt.getLayoutParams()).leftMargin = TUi.dp(this, 6);
            Long dl = o.getLoadingApprovalDeadline();
            if (dl != null) {
                long min = Math.max(0, (dl - System.currentTimeMillis()) / 60000);
                TUi.label(ph, "Auto in " + min + " min", 12, TUi.DARK, true);
            }
            TUi.progress(pb, 70, 10);
            LinearLayout steps = TUi.hrow(pb, 8);
            TUi.fill(steps, "Upload\nTransmitted", 11, TUi.MUTED, false);
            TUi.fill(steps, "Photos\nGeotagged", 11, TUi.MUTED, false);
            TUi.fill(steps, "Buyer\nReviewing", 11, TUi.DARK, true);
            LinearLayout or = TUi.hrow(w, 14);
            TUi.tag(or, "ORDER " + ref(), TUi.MINT, TUi.MINT_INK);
            TextView cg = TUi.fill(or, cargo(o), 15, TUi.INK, true);
            ((LinearLayout.LayoutParams) cg.getLayoutParams()).leftMargin = TUi.dp(this, 10);
            LinearLayout cr = TUi.hrow(w, 6);
            TUi.icon(cr, R.drawable.ic_dock_terminal, 14, TUi.MUTED);
            TUi.fill(cr, " Consignee: " + buyer, 12, TUi.INK, false);

            LinearLayout ev = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
            TUi.iconHeader(ev, R.drawable.ic_photo_outline, "Submitted Quality Evidence (Photo)", null, 0, 0);
            LinearLayout eb = TUi.box(ev, Color.WHITE, 12);
            TUi.tag(eb, "\u2713 Uploaded & Geotagged", green, Color.WHITE);
            TUi.photos(eb, o.getLoadingEvidenceUrls());
            LinearLayout meta = TUi.box(ev, Color.parseColor("#ECEAE8"), 10);
            LinearLayout m1 = TUi.hrow(meta, 0);
            TUi.icon(m1, R.drawable.ic_clock, 14, TUi.INK);
            TUi.fill(m1, " Timestamp: " + (o.getLoadingEvidenceAt() == null ? "\u2014" : time(o.getLoadingEvidenceAt())), 12, TUi.INK, false);
            LinearLayout m2 = TUi.hrow(meta, 4);
            TUi.icon(m2, R.drawable.ic_location_pin, 14, TUi.INK);
            TUi.fill(m2, " GPS: " + (e != null && e.latitude != null ? String.format(Locale.US, "%.4f\u00B0 N, %.4f\u00B0 E", e.latitude, e.longitude)
                    : (o.getPickupAddress() == null ? "\u2014" : o.getPickupAddress())), 12, TUi.INK, false);

            TUi.iconButton(actions, "Start Journey (Locked Until Approved)", R.drawable.ic_lock_gray, Color.parseColor("#D9D6D3"),
                    Color.parseColor("#57534E"), null).setEnabled(false);
            TextView note = TUi.text(actions, "Journey dispatch unlocks automatically upon buyer clearance.", 12, TUi.MUTED, false);
            note.setGravity(android.view.Gravity.CENTER);
            note.setPadding(0, TUi.dp(this, 6), 0, 0);
            return;
        }

        LinearLayout okc = TUi.notice(c, R.drawable.ic_check_circle_outline_white, Color.parseColor("#D7F5DD"), green, "Pre-Loading Photos Approved",
                o.isLoadingAutoApproved() ? "Approved automatically \u2013 the buyer did not answer within 15 minutes."
                        : "Clearance verified by " + buyer + ". Load the goods.");
        String at = o.reachedAt("LOADING_APPROVED");
        if (at != null) TUi.pill(okc, "\u2713 " + (o.getPickupAddress() == null ? "" : o.getPickupAddress() + " \u2022 ") + time(at), Color.WHITE, TUi.INK).setTextSize(11);

        LinearLayout ord = TUi.card(c);
        LinearLayout oh = TUi.hrow(ord, 0);
        TUi.tag(oh, ref(), TUi.GREY_BG, TUi.INK);
        TUi.fill(oh, "", 12, TUi.INK, false);
        TUi.tag(oh, "\u25CF Ready for Departure", TUi.MINT, TUi.MINT_INK);
        LinearLayout stops = TUi.box(ord, Color.WHITE, 6);
        stops.setPadding(0, TUi.dp(this, 10), 0, 0);
        TUi.timeline(stops, new String[]{"Pickup Hub", o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress()},
                new String[]{"Destination", buyer, o.getDropAddress()});
        LinearLayout tl = TUi.tiles(ord);
        TUi.tile(tl, 0, "Commodity", o.getCropName() == null ? "Produce" : o.getCropName(), null);
        TUi.tile(tl, 0, "Payload", String.format(Locale.US, "%,.0f kg", o.getQuantity()), null);
        if (o.getDistanceKm() != null) TUi.tile(tl, 0, "Distance", TUi.km(o.getDistanceKm()), null);
        LinearLayout pay = TUi.box(ord, TUi.GREY_BG, 12);
        TUi.row(pay, "Gross Freight Agreement", rs(fee));
        TUi.row(pay, "Platform Dispatch Fee (5%)", "-" + rs(fee - o.driverPayout()));
        LinearLayout net = TUi.box(pay, Color.WHITE, 6);
        LinearLayout nr = TUi.hrow(net, 0);
        LinearLayout nl = TUi.col(nr, 1f);
        TUi.text(nl, "NET PAYOUT", 11, green, true);
        TUi.text(nl, "Guaranteed on Delivery", 12, TUi.MUTED, false);
        TUi.label(nr, rs(o.driverPayout()), 22, TUi.DARK, true);

        TUi.notice(c, R.drawable.ic_lock_outline, TUi.SURFACE, TUi.DARK, "Smart Contract Notice",
                "Starting this journey releases the 20% crop advance to the farmer"
                        + (o.getPayment() == null ? "" : " (" + rs(o.getPayment().advanceCropPortion) + ")")
                        + ", secures your freight payout and shares your live location with the buyer.");
        TUi.iconButton(actions, "Start Journey", R.drawable.ic_navigation_pointer, TUi.DARK, Color.WHITE,
                v -> sendEvent("START_JOURNEY", "Journey started", null, v));
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
