package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/** Figma 29: harvesting in progress → "Confirm Harvest Ready" pings the driver with the pickup GPS (workflow 4.1). */
public class HarvestReadyActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_harvest_ready;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        findViewById(R.id.btnConfirmReady).setOnClickListener(v -> sendEvent("HARVEST_READY", v));
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> dial(order == null ? null : order.getTransporterPhone()));
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvStatusBadge, "Status: Harvesting in Progress");
        setText(R.id.tvOrderNumber, orderLabel(o));
        setText(R.id.tvHarvestInfo, "Pack " + kg(o.getQuantity()) + " of " + o.getCropName()
                + " in crates. When you confirm, " + (o.getTransporterName() != null ? o.getTransporterName() : "the driver")
                + " gets your farm's GPS location.");
        setText(R.id.tvDriverLabel, driverLine(o));
        setText(R.id.tvDriverEta, vehicleLine(o));
    }
}
