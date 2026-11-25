package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/**
 * "Vehicle Assigned – Start Harvesting" screen. Only reachable once a transporter
 * accepted; the backend refuses START_HARVESTING in any other state.
 */
public class StartHarvestingActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_start_harvesting;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        findViewById(R.id.btnStartHarvesting).setOnClickListener(v -> sendEvent("START_HARVESTING", v));
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderChip, orderLabel(o) + " | " + kg(o.getQuantity()) + " " + o.getCropName());
        setText(R.id.tvVehicleInfo, (o.getTransporterName() != null ? o.getTransporterName() : "The transporter")
                + " (" + vehicleLine(o) + ") accepted your job"
                + (o.getPickupDate() != null ? ". Pickup on " + o.getPickupDate() + "." : "."));
        Order.Payment p = o.getPayment();
        setText(R.id.tvFieldStatus, p != null && p.advancePaid
                ? lkr(p.advanceCropPortion) + " advance secured in escrow" : "Advance payment pending");
    }
}
