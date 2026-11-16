package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma 30: harvest is ready; the vehicle is coming / at the farm / loading checks (workflow 4.1–4.2).
 * Moves on by itself to "In Transit" once the driver starts the journey.
 */
public class VehicleArrivalActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_vehicle_arrival;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> dial(order == null ? null : order.getTransporterPhone()));
    }

    @Override
    protected void bind(@NonNull Order o) {
        String status;
        String eta;
        switch (o.getStatus()) {
            case "ARRIVED_AT_PICKUP":
                status = "Status: Vehicle at your farm";
                eta = "Arrived";
                break;
            case "AWAITING_BUYER_LOADING_APPROVAL":
                status = "Status: Buyer checking the crop photos";
                eta = "Loading check";
                break;
            case "LOADING_APPROVED":
                status = "Status: Approved – loading";
                eta = "Loading";
                break;
            default:
                status = "Status: Vehicle on the way";
                eta = o.getDistanceKm() != null ? String.format(Locale.US, "%.0f km trip", o.getDistanceKm()) : "On the way";
        }
        setText(R.id.tvVehicleStatus, status);
        setText(R.id.tvOrderNumber, orderLabel(o));
        setText(R.id.tvEtaLabel, "CURRENT STEP");
        setText(R.id.tvEta, eta);
        setText(R.id.tvDriverName, o.getTransporterName() != null ? o.getTransporterName() : "—");
        setText(R.id.tvVehicleValue, o.getVehiclePlate() != null ? o.getVehiclePlate() : "—");
        setText(R.id.tvCapacity, o.getVehicleLabel() != null ? o.getVehicleLabel() : "—");
    }
}
