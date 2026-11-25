package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

/** farmer confirmed the deal, waiting for the buyer to confirm the same terms. */
public class WaitForBuyerApprovalActivity extends BaseOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_wait_for_buyer_approval;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        findViewById(R.id.btnMessage).setOnClickListener(v -> {
            // the chat stays open while negotiating; changing terms there is a counter-offer
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra(OrderFlowRouter.EXTRA_ORDER_ID, orderId);
            startActivity(i);
        });
        findViewById(R.id.btnViewPost).setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.Tab.LISTINGS.name());
            startActivity(i);
        });
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderId, "ID: #" + o.shortId());
        setText(R.id.tvStatusPill, "Pending");
        setText(R.id.tvWaitingText, "Waiting for " + (o.getBuyerName() != null ? o.getBuyerName() : "the buyer")
                + " to confirm the final deal.");
        setText(R.id.tvCommodity, o.getCropName());
        setText(R.id.tvQuantity, kg(o.getQuantity()));
        setText(R.id.tvUnitRate, lkr(o.getPricePerKg()) + "/kg");
        setText(R.id.tvTotalValue, lkr(o.cropValue()));
        setText(R.id.tvProTipContent, "You'll be notified on confirmation. Do not harvest until a vehicle is assigned"
                + (o.getPickupDate() != null ? " (pickup " + o.getPickupDate() + ")." : "."));
    }
}
