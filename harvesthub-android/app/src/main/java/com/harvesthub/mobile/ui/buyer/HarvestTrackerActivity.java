package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Figma buyer "Harvesting & Pickup Tracker" (563:228): from the driver accepting the job until the journey
 * starts (TRANSPORTER_ACCEPTED, HARVESTING, HARVEST_READY, ARRIVED_AT_PICKUP, LOADING_APPROVED). Opens 5.1
 * automatically when the loading photos arrive.
 */
public class HarvestTrackerActivity extends BaseBuyerOrderActivity {

    private static final List<String> ORDER = Arrays.asList("TRANSPORTER_ACCEPTED", "HARVESTING", "HARVEST_READY",
            "ARRIVED_AT_PICKUP", "AWAITING_BUYER_LOADING_APPROVAL", "LOADING_APPROVED");

    @Override
    protected int layout() {
        return R.layout.activity_buyer_harvest_tracker;
    }

    @Override
    protected void bind(@NonNull Order o) {
        int at = ORDER.indexOf(o.getStatus());
        setText(R.id.tvTrackerLot, String.format(Locale.US, "Order %s • %s (%s) • %s", ref(o), o.getCropName(),
                kg(o.getQuantity()), o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()));
        Order.Payment p = o.getPayment();
        String paidAt = o.reachedAt("DISPATCHING");
        setText(R.id.tvTrackerPaid, String.format(Locale.US, "Paid %s via escrow%s", p == null ? "" : rs(p.advanceDue),
                paidAt == null ? "" : " • " + time(paidAt)));
        setText(R.id.tvTrackerEscrow, String.format(Locale.US,
                "Your %s advance is safely held in escrow. The farmer receives 20%% only when the journey starts, "
                        + "after you approve the loading photos.", p == null ? "" : rs(p.advanceDue)));

        LinearLayout steps = findViewById(R.id.layoutTrackerSteps);
        steps.removeAllViews();
        String driver = o.getTransporterName() == null ? "Driver" : o.getTransporterName();
        addStep(steps, "Vehicle assigned", driver + " • " + vehicleLine(o), at >= 0, false, time(o.reachedAt("TRANSPORTER_ACCEPTED")));
        addStep(steps, at <= 1 ? "Farmer harvesting & packing…" : "Harvest packed",
                (o.getFarmerName() == null ? "The farmer" : o.getFarmerName()) + (at <= 1
                        ? " is harvesting and crating your order." : " marked the harvest ready for pickup."),
                at >= 2, at == 0 || at == 1, time(o.reachedAt("HARVEST_READY")));
        addStep(steps, at >= 3 ? "Transporter at the farm" : "Transporter en route to farm",
                at >= 3 ? driver + " arrived at the pickup point." : "Heading to the farm once the harvest is ready.",
                at >= 3, at == 2, time(o.reachedAt("ARRIVED_AT_PICKUP")));
        String verify = "LOADING_APPROVED".equals(o.getStatus())
                ? (o.isLoadingAutoApproved() ? "Approved automatically after 15 minutes. Loading in progress."
                : "You approved the photos. Loading in progress; the journey starts soon.")
                : "The driver uploads photos of the crop before loading. You approve or reject them.";
        addStep(steps, "Pre-loading quality verification", verify, at >= 5, at == 3 || at == 4, time(o.reachedAt("LOADING_APPROVED")));

        findViewById(R.id.btnTrackerCallDriver).setOnClickListener(v -> dial(o.getTransporterPhone()));
        findViewById(R.id.btnTrackerChat).setOnClickListener(v -> {
            Intent i = new Intent(this, NegotiationChatActivity.class);
            i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, o.getId());
            startActivity(i);
        });
    }

    private void addStep(LinearLayout parent, String title, String body, boolean done, boolean active, String when) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView dot = new TextView(this);
        dot.setText(done ? "✓" : active ? "●" : "○");
        dot.setTextSize(16);
        dot.setGravity(Gravity.CENTER);
        dot.setTextColor(done ? 0xFF2E7D32 : active ? 0xFFF59E0B : 0xFF94A3B8);
        row.addView(dot, new LinearLayout.LayoutParams(dp(28), dp(28)));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(10), 0, 0, 0);
        TextView t = new TextView(this);
        t.setText(active ? title + "  ·  IN PROGRESS" : title);
        t.setTextColor(done || active ? 0xFF1B4332 : 0xFF64748B);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        TextView b = new TextView(this);
        b.setText(done && when != null && !when.isEmpty() ? body + "\n" + when : body);
        b.setTextColor(0xFF475569);
        b.setTextSize(13);
        text.addView(t);
        text.addView(b);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        parent.addView(row);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
