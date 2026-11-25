package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/**
 * deal accepted, waiting for the buyer's advance payment.
 * Shows the farmer's 20% crop advance that will be held in escrow.
 */
public class WaitingForAdvancePaymentActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_waiting_for_advance_payment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        // negotiation is closed at this point; there is no buyer phone number in the workflow
        findViewById(R.id.btnContactBuyer).setVisibility(View.GONE);
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderId, o.shortId());
        setText(R.id.tvWaitingDesc, (o.getBuyerName() != null ? o.getBuyerName() : "The buyer")
                + " has confirmed the deal. The advance payment is being secured in escrow to guarantee your transaction.");
        Order.Payment p = o.getPayment();
        setText(R.id.tvEscrowLabel, "CROP ADVANCE IN ESCROW (20%)");
        setText(R.id.tvEscrowAmount, p != null ? lkr(p.advanceCropPortion) : "—");
        setText(R.id.tvQuantity, kg(o.getQuantity()));
        setText(R.id.tvUnitRate, lkr(o.getPricePerKg()) + "/kg");
    }
}
