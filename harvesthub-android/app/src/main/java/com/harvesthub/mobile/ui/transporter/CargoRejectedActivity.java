package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
import android.content.Intent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;

/**
 * Figma "Cargo Rejected by Buyer" – pre-loading rejection (workflow 5.3, farmer at fault): nothing is loaded,
 * the driver receives the Empty Trip Fee (= full transport fee) from the farmer's wallet.
 */
public class CargoRejectedActivity extends BaseTransporterOrderActivity {

    @Override
    protected String title() {
        return "Dispatch Aborted";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout strip = TUi.hrow(c, 14);
        strip.setBackground(TUi.round(TUi.RED_BG, TUi.dp(this, 8)));
        strip.setPadding(TUi.dp(this, 12), TUi.dp(this, 8), TUi.dp(this, 12), TUi.dp(this, 8));
        TUi.fill(strip, "\u25CF  DISPATCH STATUS: ABORTED", 11, TUi.RED, true).setLetterSpacing(0.04f);
        TUi.label(strip, "Log " + ref(), 11, TUi.MUTED, false);

        LinearLayout red = TUi.card(c, TUi.RED_BG, TUi.RED_BG);
        LinearLayout rr = TUi.hrow(red, 0);
        rr.setGravity(android.view.Gravity.TOP);
        TUi.iconTile(rr, R.drawable.ic_shield, 42, TUi.RED, Color.WHITE, true);
        LinearLayout rt = TUi.col(rr, 1f);
        rt.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.pill(rt, "\u26A0 INSPECTION FAILURE", Color.WHITE, TUi.RED).setTextSize(10);
        TUi.text(rt, "Cargo Rejected by Buyer at Pre-Loading", 19, TUi.RED, true);
        TUi.text(rt, reason(o) + " Nothing is loaded. Dispatch cancelled.", 13, TUi.RED, false);
        LinearLayout info = TUi.box(red, Color.WHITE, 12);
        LinearLayout ir = TUi.hrow(info, 0);
        ir.setGravity(android.view.Gravity.TOP);
        LinearLayout i1 = TUi.col(ir, 1f);
        TUi.text(i1, "Commodity Batch", 11, TUi.MUTED, true);
        TUi.text(i1, cargo(o), 14, TUi.INK, true);
        LinearLayout i2 = TUi.col(ir, 1f);
        TUi.text(i2, "Buyer", 11, TUi.MUTED, true);
        TUi.text(i2, o.getBuyerName() == null ? "\u2014" : o.getBuyerName(), 14, TUi.INK, true);

        LinearLayout ph = TUi.card(c);
        TUi.iconHeader(ph, R.drawable.ic_photo_outline, "Your Pre-Loading Photos", "Rejected", TUi.RED_BG, TUi.RED);
        TUi.photos(ph, o.getLoadingEvidenceUrls());

        LinearLayout s = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
        TUi.iconHeader(s, R.drawable.ic_mandatory_protocol, "Mandatory Driver Actions", null, 0, 0);
        TUi.text(s, "Required sequence before leaving the farm", 12, TUi.INK, true);
        TUi.numStep(s, 1, "Do Not Load", "Do not load the crates into your vehicle.", true);
        TUi.numStep(s, 2, "Return Cargo", "Hand the produce back to " + (o.getFarmerName() == null ? "the farmer" : o.getFarmerName()) + " at the gate.", false);
        TUi.numStep(s, 3, "Leave the Farm", "No return trip is needed \u2013 the order is closed.", false);
        TUi.numStep(s, 4, "Go Online Again", "Switch to Available on Home to receive the next job.", false);

        double fee = o.getPayment() == null ? 0 : o.getPayment().emptyTripFee;
        LinearLayout pay = TUi.card(c, Color.parseColor("#D7F5DD"), Color.parseColor("#D7F5DD"));
        LinearLayout ph2 = TUi.hrow(pay, 0);
        TUi.tag(ph2, "\u2713 TRANSPORTER DEADHEAD PROTECTION", TUi.MINT, TUi.MINT_INK);
        TextView ic = TUi.fill(ph2, "\u26A1 Instant Credit", 11, Color.parseColor("#15803D"), true);
        ic.setGravity(android.view.Gravity.END);
        LinearLayout am = TUi.hrow(pay, 12);
        LinearLayout al = TUi.col(am, 1f);
        TUi.text(al, "Empty Trip Fee Credited", 12, TUi.INK, false);
        TUi.text(al, "+" + rs(fee), 30, Color.parseColor("#15803D"), true);
        TUi.iconTile(am, R.drawable.ic_wallet, 42, TUi.MINT, TUi.DARK, true);
        TUi.text(pay, "Paid directly by the farmer to compensate for wasted fuel and arrival time. Already deposited in your HarvestHub Wallet.",
                13, Color.parseColor("#374151"), false).setPadding(0, TUi.dp(this, 6), 0, 0);

        TUi.iconButton(actions, "Return to Dispatch Home", R.drawable.transporter_ic_bottom_home, TUi.DARK, Color.WHITE, v -> {
            startActivity(new Intent(this, DeliveryDashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
        TUi.iconButton(actions, "View Wallet", R.drawable.ic_wallet, Color.WHITE, TUi.INK,
                v -> startActivity(new Intent(this, TransporterWalletActivity.class)));
    }

    static String reason(Order o) {
        if (o.getStatusHistory() != null) {
            for (int i = o.getStatusHistory().size() - 1; i >= 0; i--) {
                Order.StatusChange sc = o.getStatusHistory().get(i);
                if (sc.status != null && sc.status.equals(o.getStatus()) && sc.note != null) {
                    int k = sc.note.indexOf(':');
                    return k > 0 ? "Buyer's reason: " + sc.note.substring(k + 1).trim() : sc.note;
                }
            }
        }
        return "The buyer rejected the delivery.";
    }
}
