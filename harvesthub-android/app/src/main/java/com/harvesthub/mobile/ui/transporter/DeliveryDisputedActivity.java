package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
import android.content.Intent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma "Transit Damage Dispute" – post-delivery rejection (workflow 5.3, transporter at fault). Decisions 15 Nov:
 * the crop-advance charge applies at once; the driver drives the goods back to the farm and taps Goods Returned;
 * an appeal opens a ticket for the officers but does not change the money.
 */
public class DeliveryDisputedActivity extends BaseTransporterOrderActivity {

    private Tracking tracking;

    @Override
    protected String title() {
        return "Transit Damage Dispute";
    }

    @Override
    protected void load() {
        super.load();
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (r.isSuccessful()) tracking = r.body();
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        boolean returned = "RETURNED".equals(o.getStatus());
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        double charge = o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion;
        String farmer = o.getFarmerName() == null ? "the farm" : o.getFarmerName();

        LinearLayout red = TUi.card(c, TUi.RED_BG, TUi.RED_BG);
        LinearLayout rr = TUi.hrow(red, 0);
        TUi.iconTile(rr, R.drawable.ic_shield, 38, Color.WHITE, TUi.RED, true);
        TextView cap = TUi.fill(rr, "ACTIVE DISPUTE NOTICE", 11, TUi.RED, true);
        cap.setLetterSpacing(0.04f);
        ((LinearLayout.LayoutParams) cap.getLayoutParams()).leftMargin = TUi.dp(this, 10);
        TUi.tag(rr, returned ? "Goods Returned" : "Priority Action", Color.WHITE, TUi.RED);
        TUi.text(red, "Transit Damage Dispute Filed by Buyer", 20, TUi.RED, true).setPadding(0, TUi.dp(this, 8), 0, 0);
        TUi.text(red, CargoRejectedActivity.reason(o), 13, TUi.RED, false).setPadding(0, TUi.dp(this, 4), 0, 0);
        String when = o.reachedAt("RETURN_JOURNEY");
        TUi.text(red, "Dispute Ref: " + ref() + " \u2022 " + cargo(o) + (when == null ? "" : " \u2022 Filed " + time(when)), 11, TUi.RED, true)
                .setPadding(0, TUi.dp(this, 8), 0, 0);

        LinearLayout m = TUi.card(c);
        TUi.iconHeader(m, R.drawable.ic_wallet, "Financial Impact & Ledger Settlement", "Applied", TUi.GREY_BG, TUi.INK);
        LinearLayout rows = TUi.box(m, Color.WHITE, 4);
        rows.setPadding(0, TUi.dp(this, 6), 0, 0);
        TUi.row(rows, "Trip Gross Agreed Fee", rs(fee));
        TUi.row(rows, "Trip Payout", rs(0) + " (Forfeited)", TUi.RED, false);
        TUi.row(rows, "Ruined Cargo Liability Charge", "-" + rs(charge), TUi.RED, false);
        TUi.text(m, "*The buyer got a full refund and the farmer keeps the advance, so the crop advance is charged to your wallet.", 11, TUi.MUTED, false);
        LinearLayout tot = TUi.box(m, TUi.RED_BG, 10);
        LinearLayout tr = TUi.hrow(tot, 0);
        TUi.icon(tr, R.drawable.ic_warning_triangle_red, 18, TUi.RED);
        TextView tl = TUi.fill(tr, "Total Wallet Adjustment", 13, TUi.RED, true);
        ((LinearLayout.LayoutParams) tl.getLayoutParams()).leftMargin = TUi.dp(this, 8);
        TUi.label(tr, "-" + rs(charge), 18, TUi.RED, true);

        LinearLayout ev = TUi.card(c);
        TUi.iconHeader(ev, R.drawable.ic_photo_outline, "Inspection Evidence", "Buyer approved pre-transit", TUi.MINT, TUi.MINT_INK);
        TUi.photos(ev, o.getLoadingEvidenceUrls());
        TUi.iconButton(ev, "View Dispute Evidence & Case", R.drawable.ic_eye, Color.parseColor("#E9E7E5"), TUi.INK, v -> startActivity(
                new Intent(this, DisputeCaseActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId))).setCornerRadius(TUi.dp(this, 40));

        LinearLayout r = TUi.card(c);
        TUi.iconHeader(r, R.drawable.ic_reverse_routing, "Reverse Logistics Return Route", returned ? "Done" : "Mandatory",
                returned ? TUi.MINT : TUi.RED_BG, returned ? TUi.MINT_INK : TUi.RED);
        LinearLayout rb = TUi.box(r, TUi.GREY_BG, 12);
        LinearLayout rl = TUi.hrow(rb, 0);
        rl.setGravity(android.view.Gravity.TOP);
        TUi.icon(rl, R.drawable.ic_return_queue, 18, TUi.INK);
        LinearLayout rt = TUi.col(rl, 1f);
        rt.setPadding(TUi.dp(this, 8), 0, 0, 0);
        if (returned) {
            TUi.text(rt, "Goods returned to the farm", 13, TUi.INK, true);
            TUi.text(rt, "Recorded " + time(o.getGoodsReturnedAt()) + ". The farmer was notified.", 12, TUi.MUTED, false);
        } else {
            TUi.text(rt, "Return leg: " + (o.getDropAddress() == null ? "buyer" : o.getDropAddress()) + " \u2192 " + (o.getPickupAddress() == null ? farmer : o.getPickupAddress()),
                    13, TUi.INK, true);
            TUi.text(rt, "Take the rejected goods back to " + farmer + ", then tap Goods Returned.", 12, TUi.MUTED, false);
            TUi.smallButton(rt, "Navigate to the Farm", R.drawable.ic_navigation_pointer, TUi.DARK, Color.WHITE, v -> RouteMap.navigate(this,
                    tracking == null ? null : tracking.pickupLatitude, tracking == null ? null : tracking.pickupLongitude));
        }

        if (!returned) {
            TUi.iconButton(actions, "Acknowledge & Confirm Goods Returned", R.drawable.ic_truck, TUi.DARK, Color.WHITE, v -> new AlertDialog.Builder(this)
                    .setTitle("Goods returned?")
                    .setMessage("Confirm that the rejected goods are back at the farm.")
                    .setPositiveButton("Confirm", (d, w) -> sendEvent("GOODS_RETURNED", "Returned to the farm", null, null))
                    .setNegativeButton("Cancel", null).show());
        }
        TUi.iconButton(actions, "Appeal Dispute with Transporter Log", R.drawable.ic_dispute_appeal, Color.parseColor("#E9E7E5"), TUi.INK, v -> startActivity(
                new Intent(this, AppealDisputeActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
    }

}
