package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma buyer 4.1 "Order Summary & Dynamic Billing". Shows the backend's quote for the accepted deal:
 * crop value, auto-selected vehicle and distance, and the advance = 20% crop + transport + 3% fee.
 */
public class OrderSummaryActivity extends BaseBuyerOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_order_summary;
    }

    @Override
    protected void bind(@NonNull Order o) {
        boolean cancelled = "CANCELLED".equals(o.getStatus());
        setText(R.id.tvOrderStatus, cancelled ? "Status: Cancelled" : "Status: Awaiting Payment");
        setText(R.id.tvOrderRefBadge, "Ref " + ref(o));
        setText(R.id.tvHarvestCropName, String.format(Locale.US, "%s %s", kg(o.getQuantity()), o.getCropName() == null ? "" : o.getCropName()));
        setText(R.id.tvHarvestFarmerName, "Farmer: " + (o.getFarmerName() == null ? "-" : o.getFarmerName()));
        setText(R.id.tvHarvestPriceTotal, rs(o.cropValue()));
        setText(R.id.tvHarvestPricePerKg, rs(o.getPricePerKg()) + "/kg locked");
        setText(R.id.tvVehicleTier, o.getVehicleLabel() == null ? "-" : o.getVehicleLabel());
        setText(R.id.tvDistance, o.getDistanceKm() == null ? "-" : String.format(Locale.US, "%.1f km", o.getDistanceKm()));

        Order.Payment p = o.getPayment();
        MaterialButton pay = findViewById(R.id.btnSecurePayment);
        if (p == null) {
            setText(R.id.tvTotalAdvancePayment, "Waiting for the quote");
            pay.setEnabled(false);
            return;
        }
        setText(R.id.tvCalculatedTransitCost, rs(p.transportFee));
        setText(R.id.tvPartialCropCost, rs(p.advanceCropPortion));
        setText(R.id.tvTransportFee, rs(p.transportFee));
        setText(R.id.tvConvenienceFee, rs(p.buyerFee));
        setText(R.id.tvTotalAdvancePayment, "Total Advance Payment " + rs(p.advanceDue));
        setText(R.id.tvEscrowNote, String.format(Locale.US,
                "Remaining 80%% (%s) is paid when you accept the delivery.", rs(p.balanceDue)));

        pay.setEnabled(!cancelled && !p.advancePaid);
        pay.setText(cancelled ? "Order cancelled" : "Secure Advance Payment");
        pay.setOnClickListener(v -> {
            Intent i = new Intent(this, SecureCheckoutActivity.class);
            i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, o.getId());
            startActivity(i);
        });
    }
}
