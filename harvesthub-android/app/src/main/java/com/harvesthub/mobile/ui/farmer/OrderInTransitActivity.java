package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/** Figma 31: goods left the farm; the 20% crop advance is already in the farmer's wallet (workflow 4.3, 5.1–5.2). */
public class OrderInTransitActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_order_in_transit;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        findViewById(R.id.btnCall).setOnClickListener(v -> dial(order == null ? null : order.getTransporterPhone()));
        findViewById(R.id.btnTrackOrder).setOnClickListener(v -> {
            Intent i = new Intent(this, ActiveOrderTrackerActivity.class);
            i.putExtra(OrderFlowRouter.EXTRA_ORDER_ID, orderId);
            startActivity(i);
        });
    }

    @Override
    protected void bind(@NonNull Order o) {
        String title;
        switch (o.getStatus()) {
            case "ARRIVED_AT_DESTINATION":
                title = "At the buyer";
                break;
            case "BALANCE_PAID":
            case "DELIVERED":
                title = "Delivered – confirming";
                break;
            default:
                title = "In Transit";
        }
        setText(R.id.tvStatus, "Status: " + title);
        setText(R.id.tvOrderId, orderLabel(o));
        setText(R.id.tvInTransitTitle, title);
        Order.Payment p = o.getPayment();
        String paid = p != null && p.farmerAdvanceReleased ? lkr(p.advanceCropPortion) + " advance is in your wallet. " : "";
        setText(R.id.tvDescription, paid + "The remaining " + (p != null ? lkr(p.balanceDue) : "balance")
                + " is paid automatically when the driver and the buyer both confirm the delivery.");
        setText(R.id.tvDriverName, driverLine(o));
        setText(R.id.tvVehicleInfo, vehicleLine(o));
    }
}
