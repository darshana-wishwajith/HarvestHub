package com.harvesthub.mobile.ui.transporter;

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
        TUi.pill(c, "● DISPATCH STATUS: ABORTED • " + ref(), TUi.RED_BG, TUi.RED);
        TUi.banner(c, TUi.RED_BG, TUi.RED, "Cargo rejected by the buyer at pre-loading",
                reason(o) + "\nNothing is loaded. The order is closed.");

        LinearLayout b = TUi.card(c);
        TUi.row(b, "Commodity", cargo(o));
        TUi.row(b, "Farm", o.getFarmerName() == null ? "—" : o.getFarmerName());
        TUi.row(b, "Buyer", o.getBuyerName() == null ? "—" : o.getBuyerName());
        TUi.caption(b, "Your pre-loading photos");
        TUi.photos(b, o.getLoadingEvidenceUrls());

        LinearLayout s = TUi.card(c);
        TUi.title(s, "What to do now");
        TUi.step(s, "Do not load the crates", 2, null);
        TUi.step(s, "Hand the produce back to the farmer at the gate", 1, null);
        TUi.step(s, "Go online again for the next job", 0, null);

        double fee = o.getPayment() == null ? 0 : o.getPayment().emptyTripFee;
        LinearLayout pay = TUi.banner(c, TUi.SURFACE, TUi.DARK, "Transporter protection – Empty Trip Fee", null);
        TUi.big(pay, "+" + rs(fee), TUi.DARK);
        TUi.body(pay, "Paid by the farmer to compensate your fuel and time. Already in your HarvestHub wallet.");

        TUi.primary(actions, "Return to Dispatch Home", v -> {
            startActivity(new Intent(this, DeliveryDashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
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
