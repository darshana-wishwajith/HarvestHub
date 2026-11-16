package com.harvesthub.mobile.ui.farmer;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/** Figma 22: overview of any order (escrow state, buyer, driver, current step). Does not auto-advance. */
public class ActiveOrderTrackerActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_active_order_tracker;
    }

    @Override
    protected boolean followStatus() {
        return false;
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderNumber, orderLabel(o));
        setText(R.id.tvEscrow, escrowLabel(o));
        setText(R.id.tvOrderCrop, kg(o.getQuantity()) + " " + o.getCropName());
        setText(R.id.tvBuyer, "Buyer: " + (o.getBuyerName() != null ? o.getBuyerName() : "—"));
        setText(R.id.tvCurrentStep, stepLabel(o));
        setText(R.id.tvDriver, driverLine(o));
        setText(R.id.tvVehicle, "(" + vehicleLine(o) + ")");
    }

    static String escrowLabel(Order o) {
        String e = o.getEscrowStatus() == null ? "" : o.getEscrowStatus();
        switch (e) {
            case "ADVANCE_HELD":
            case "FULLY_PAID":
                return "FUNDS IN ESCROW";
            case "RELEASED":
                return "FUNDS RELEASED";
            case "REFUNDED":
                return "REFUNDED";
            default:
                return "AWAITING PAYMENT";
        }
    }

    static String stepLabel(Order o) {
        Order.Payment p = o.getPayment();
        switch (o.getStatus() == null ? "" : o.getStatus()) {
            case "NEGOTIATING": return "Negotiating the final deal";
            case "AWAITING_PAYMENT": return "Waiting for the buyer's advance payment";
            case "DISPATCHING":
            case "TRANSPORTER_ASSIGNED": return "Buyer payment cleared – finding a vehicle";
            case "PAUSED": return "Buyer payment cleared – no vehicle free yet, retrying";
            case "TRANSPORTER_ACCEPTED": return "Transporter assigned – you can start harvesting";
            case "HARVESTING": return "Harvesting in progress";
            case "HARVEST_READY": return "Harvest ready – vehicle on the way";
            case "ARRIVED_AT_PICKUP": return "Vehicle at the farm";
            case "AWAITING_BUYER_LOADING_APPROVAL": return "Buyer checking the pre-loading photos";
            case "LOADING_APPROVED": return "Approved – loading the goods";
            case "IN_TRANSIT": return "In transit to the buyer";
            case "ARRIVED_AT_DESTINATION": return "Arrived – buyer inspecting the goods";
            case "BALANCE_PAID":
            case "DELIVERED": return "Delivered – waiting for both confirmations";
            case "COMPLETED": return "Completed – " + (p != null ? lkr(p.cropValue) : "") + " paid to your wallet";
            case "PRELOAD_REJECTED": return "Rejected before loading – Empty Trip Fee "
                    + (p != null ? lkr(p.emptyTripFee) : "") + " charged";
            case "RETURN_JOURNEY": return "Rejected after delivery – you keep the advance";
            case "CANCELLED": return "Cancelled";
            default: return o.getStatus();
        }
    }
}
