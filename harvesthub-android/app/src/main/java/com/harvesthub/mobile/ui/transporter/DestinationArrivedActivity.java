package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * "Arrived at destination" screen (buyer inspecting) and "2-Way Verification Handshake":
 * ARRIVED_AT_DESTINATION → wait for the buyer to accept (pays 80%) or reject;
 * BALANCE_PAID / DELIVERED → driver taps Delivery Completed; buyer taps Goods Received; both → COMPLETED.
 */
public class DestinationArrivedActivity extends BaseTransporterOrderActivity {

    @Override
    protected String title() {
        return order != null && !"ARRIVED_AT_DESTINATION".equals(order.getStatus()) ? "Unloading Handshake" : "Arrived at Destination";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        String s = o.getStatus();
        boolean inspecting = "ARRIVED_AT_DESTINATION".equals(s);
        boolean driverDone = o.isTransporterConfirmedDelivery();
        boolean buyerDone = o.isBuyerConfirmedReceipt();
        int green = Color.parseColor("#15803D");
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        String buyer = o.getBuyerName() == null ? "the buyer" : o.getBuyerName();
        String photo = o.getLoadingEvidenceUrls() == null || o.getLoadingEvidenceUrls().isEmpty() ? null : o.getLoadingEvidenceUrls().get(0);

        if (inspecting) {
            LinearLayout a = TUi.card(c);
            LinearLayout ar = TUi.hrow(a, 0);
            TUi.tag(ar, "\u25CF GPS Position Recorded", TUi.MINT, TUi.MINT_INK);
            TextView tm = TUi.fill(ar, o.getActualDeliveryDate() == null ? "" : time(o.getActualDeliveryDate()), 12, TUi.MUTED, false);
            tm.setGravity(android.view.Gravity.END);
            LinearLayout sr = TUi.hrow(a, 10);
            TUi.fill(sr, "ARRIVAL STATUS", 11, TUi.INK, true).setLetterSpacing(0.04f);
            TUi.label(sr, "Buyer Inspecting", 12, green, true);
            LinearLayout dark = TUi.box(a, TUi.DARK, 8);
            LinearLayout dr = TUi.hrow(dark, 0);
            TUi.icon(dr, R.drawable.ic_verified_badge, 22, Color.parseColor("#86EFAC"));
            LinearLayout dt = TUi.col(dr, 1f);
            dt.setPadding(TUi.dp(this, 10), 0, 0, 0);
            TUi.text(dt, "Delivered \u2022 Awaiting Buyer Inspection", 14, Color.WHITE, true);
            TUi.text(dt, buyer + " compares the goods with the pre-loading photos.", 12, Color.parseColor("#D1FAE5"), false);

            LinearLayout m = TUi.card(c);
            LinearLayout mh = TUi.hrow(m, 0);
            TUi.iconTile(mh, R.drawable.ic_archive_box, 40, TUi.MINT, TUi.DARK, false);
            LinearLayout mt = TUi.col(mh, 1f);
            mt.setPadding(TUi.dp(this, 10), 0, 0, 0);
            TUi.text(mt, "Shipment Manifest", 12, TUi.MUTED, true);
            TUi.text(mt, ref(), 16, TUi.INK, true);
            if (o.getVehicleLabel() != null) TUi.tag(mh, o.getVehicleLabel(), TUi.MINT, TUi.MINT_INK);
            TUi.divider(m, 12);
            LinearLayout cr = TUi.hrow(m, 0);
            TUi.thumb(cr, photo, 56, 56);
            LinearLayout ct = TUi.col(cr, 1f);
            ct.setPadding(TUi.dp(this, 12), 0, 0, 0);
            TUi.text(ct, o.getCropName() == null ? "Produce" : o.getCropName(), 16, TUi.INK, true);
            TUi.text(ct, String.format(Locale.US, "%,.0f kg Net Weight", o.getQuantity()), 13, TUi.MUTED, false);
            LinearLayout stops = TUi.box(m, Color.WHITE, 6);
            stops.setPadding(0, TUi.dp(this, 10), 0, 0);
            String left = o.reachedAt("IN_TRANSIT");
            TUi.timeline(stops, new String[]{"Origin Departure" + (left == null ? "" : " (" + time(left) + ")"), o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress()},
                    new String[]{"Current Destination", buyer, o.getDropAddress()});

            LinearLayout u = TUi.card(c);
            TUi.text(u, "UNLOADING INSTRUCTIONS", 11, TUi.INK, true).setLetterSpacing(0.04f);
            TUi.text(u, buyer, 20, TUi.INK, true);
            if (o.getDropAddress() != null) {
                LinearLayout ad = TUi.hrow(u, 2);
                TUi.icon(ad, R.drawable.ic_location_pin, 14, TUi.MUTED);
                TUi.fill(ad, " " + o.getDropAddress(), 13, TUi.MUTED, false);
            }
            TUi.text(u, "UNLOADING PROTOCOL", 11, TUi.INK, true).setPadding(0, TUi.dp(this, 14), 0, 0);
            LinearLayout pbox = TUi.box(u, TUi.GREY_BG, 6);
            pbox.setPadding(TUi.dp(this, 8), 0, TUi.dp(this, 8), TUi.dp(this, 8));
            TUi.numStep(pbox, 1, "Wait for the inspection", "The buyer checks the goods against your pre-loading photos.", false);
            TUi.numStep(pbox, 2, "Unload after acceptance", "Unload once the buyer accepts and pays the 80% balance.", false);
            TUi.numStep(pbox, 3, "Confirm together", "You tap Delivery Completed, the buyer taps Goods Received.", false);

            TUi.notice(c, R.drawable.ic_lock, Color.parseColor("#C8EFD3"), TUi.DARK, "Escrow Final Payout Protected",
                    "2-way digital sign-off is required to release " + rs(o.driverPayout()) + " to your HarvestHub wallet.");
            TUi.iconButton(actions, "Waiting for the Buyer's Decision\u2026", R.drawable.ic_clock, Color.parseColor("#D9D6D3"),
                    Color.parseColor("#57534E"), null).setEnabled(false);
        } else {
            LinearLayout pr = TUi.hrow(c, 16);
            pr.setGravity(android.view.Gravity.CENTER);
            TUi.tag(pr, "\u25CF  Physical Handshake in Progress", TUi.MINT, TUi.MINT_INK).setTextSize(12);

            LinearLayout h = TUi.card(c);
            int doneCount = (buyerDone ? 1 : 0) + (driverDone ? 1 : 0);
            TUi.iconHeader(h, R.drawable.ic_handshake, "2-Way Verification Handshake", doneCount + " OF 2 COMPLETE", TUi.MINT, TUi.MINT_INK);
            TUi.text(h, "Both parties must digitally confirm the handover.", 13, TUi.MUTED, false).setPadding(0, TUi.dp(this, 4), 0, 0);
            LinearLayout box = TUi.box(h, TUi.GREY_BG, 12);
            party(box, R.drawable.ic_check, "Buyer Action: Goods Received",
                    buyerDone ? "Confirmed by " + buyer : buyer + " accepted and paid the 80% balance",
                    buyerDone ? "\u2713 SYNCED & VERIFIED" : "WAITING FOR BUYER", buyerDone);
            party(box, R.drawable.ic_truck, "Transporter Action: Unloading Complete",
                    "Confirm the vehicle is empty and the goods are handed over.",
                    driverDone ? "\u2713 CONFIRMED" : "PENDING YOUR SIGN-OFF", driverDone);
            LinearLayout tl = TUi.tiles(h);
            TUi.tile(tl, R.drawable.ic_scale_manifest, "CARGO", String.format(Locale.US, "%,.0f kg", o.getQuantity()), o.getCropName());
            TUi.tile(tl, R.drawable.ic_archive_box, "ORDER", ref(), null);

            LinearLayout pay = TUi.card(c);
            TUi.iconHeader(pay, R.drawable.ic_wallet, "Settlement Payout Breakdown", null, 0, 0);
            LinearLayout pb = TUi.box(pay, TUi.GREY_BG, 12);
            TUi.row(pb, "Gross Freight Fee", rs(fee));
            TUi.row(pb, "Platform Commission (5%)", "-" + rs(fee - o.driverPayout()), TUi.RED, false);
            TUi.divider(pb, 8);
            LinearLayout nr = TUi.hrow(pb, 0);
            LinearLayout nl = TUi.col(nr, 1f);
            TUi.text(nl, "TOTAL NET TRANSPORTER PAYOUT", 11, TUi.INK, true);
            TUi.text(nl, "\u26A1 Immediate settlement", 12, green, true);
            TUi.label(nr, "+" + rs(o.driverPayout()), 22, TUi.DARK, true);
            TUi.text(pb, "Credited to your HarvestHub wallet when both confirmations are in.", 12, TUi.MUTED, false).setPadding(0, TUi.dp(this, 8), 0, 0);

            if (!driverDone) {
                TUi.iconButton(actions, "Delivery Completed", R.drawable.ic_done_all, TUi.DARK, Color.WHITE,
                        v -> sendEvent("DELIVERY_COMPLETED", "Unloaded at the buyer", null, v));
            } else {
                TUi.iconButton(actions, "Waiting for the Buyer to Confirm\u2026", R.drawable.ic_clock, Color.parseColor("#D9D6D3"),
                        Color.parseColor("#57534E"), null).setEnabled(false);
            }
        }
        if (o.isDisputeOpen()) {
            TUi.link(actions, "\u2691 Issue reported on this order \u2013 view dispute", TUi.RED, v -> startActivity(
                    new android.content.Intent(this, DisputeCaseActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
        }
    }

    private void party(LinearLayout box, int iconRes, String title, String sub, String tagText, boolean done) {
        LinearLayout r = TUi.hrow(box, box.getChildCount() == 0 ? 0 : 14);
        r.setGravity(android.view.Gravity.TOP);
        TUi.iconTile(r, iconRes, 36, TUi.DARK, Color.WHITE, true);
        LinearLayout t = TUi.col(r, 1f);
        t.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.text(t, title, 15, TUi.INK, true);
        TUi.text(t, sub, 12, TUi.MUTED, false);
        TUi.pill(t, tagText, done ? TUi.DARK : TUi.MINT, done ? Color.WHITE : TUi.MINT_INK).setTextSize(10);
    }

}
