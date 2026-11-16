package com.harvesthub.mobile.ui.transporter;

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
        return "Delivery Rejected";
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
        TUi.banner(c, TUi.RED_BG, TUi.RED, "Transit damage – delivery rejected by the buyer",
                CargoRejectedActivity.reason(o) + "\nOrder " + ref() + " • " + cargo(o));

        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        double charge = o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion;
        LinearLayout m = TUi.card(c);
        TUi.title(m, "Financial impact");
        TUi.row(m, "Trip agreed fee", rs(fee));
        TUi.row(m, "Trip payout", rs(0) + " (forfeited)", TUi.RED, false);
        TUi.row(m, "Ruined cargo charge (crop advance)", "-" + rs(charge), TUi.RED, false);
        TUi.row(m, "Total wallet adjustment", "-" + rs(charge), TUi.RED, true);
        TUi.body(m, "The buyer got a full refund; the farmer keeps the advance (workflow 5.3). If your balance is below zero you get no new jobs until it is paid back.");

        LinearLayout ev = TUi.card(c);
        TUi.title(ev, "Evidence");
        TUi.caption(ev, "Your pre-loading photos (approved by the buyer)");
        TUi.photos(ev, o.getLoadingEvidenceUrls());

        LinearLayout r = TUi.card(c);
        TUi.title(r, "Return route");
        if (returned) {
            TUi.step(r, "Goods returned to the farm " + time(o.getGoodsReturnedAt()), 2, "Done");
        } else {
            TUi.pill(r, "Mandatory", TUi.RED_BG, TUi.RED);
            TUi.body(r, "Take the rejected goods back to " + (o.getFarmerName() == null ? "the farm" : o.getFarmerName())
                    + (o.getPickupAddress() == null ? "" : " – " + o.getPickupAddress()) + ", then tap Goods Returned.");
            TUi.secondary(r, "Navigate to the farm", v -> RouteMap.navigate(this,
                    tracking == null ? null : tracking.pickupLatitude, tracking == null ? null : tracking.pickupLongitude));
        }

        if (!returned) {
            TUi.primary(actions, "Goods Returned to Farm", v -> new AlertDialog.Builder(this)
                    .setTitle("Goods returned?")
                    .setMessage("Confirm that the rejected goods are back at the farm.")
                    .setPositiveButton("Confirm", (d, w) -> sendEvent("GOODS_RETURNED", "Returned to the farm", null, null))
                    .setNegativeButton("Cancel", null).show());
        }
        TUi.secondary(actions, "Appeal with transporter log", v -> startActivity(
                new Intent(this, AppealDisputeActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
        TUi.secondary(actions, "View dispute case", v -> startActivity(
                new Intent(this, DisputeCaseActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
    }
}
