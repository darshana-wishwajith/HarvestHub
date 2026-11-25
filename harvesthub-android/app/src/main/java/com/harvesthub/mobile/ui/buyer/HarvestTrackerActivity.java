package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * "Harvesting & Pickup Tracker" screen: from the driver accepting the job until the journey
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

    /** Stepper row in the style of "Dispatch Loop" screen: status circle, connector line, title, detail, time. */
    private void addStep(LinearLayout parent, String title, String body, boolean done, boolean active, String when) {
        boolean last = parent.getChildCount() == 3;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        row.addView(rail, new LinearLayout.LayoutParams(dp(30), LinearLayout.LayoutParams.MATCH_PARENT));
        android.widget.FrameLayout dot = new android.widget.FrameLayout(this);
        int green = 0xFF15803D, dark = 0xFF1B4332, grey = 0xFFCBD5E1;
        android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
        circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        if (done) {
            circle.setColor(0xFFDCFCE7);
        } else if (active) {
            circle.setColor(0xFFEEF4FF);
            circle.setStroke(dp(2), 0xFF2563EB);
        } else {
            circle.setColor(0xFFFFFFFF);
            circle.setStroke(dp(2), grey);
        }
        dot.setBackground(circle);
        if (done) {
            ImageView ok = new ImageView(this);
            ok.setImageResource(R.drawable.ic_check);
            ok.setColorFilter(green);
            dot.addView(ok, new android.widget.FrameLayout.LayoutParams(dp(14), dp(14), Gravity.CENTER));
        } else if (active) {
            View inner = new View(this);
            inner.setBackground(com.harvesthub.mobile.ui.transporter.TUi.round(0xFF2563EB, dp(6)));
            dot.addView(inner, new android.widget.FrameLayout.LayoutParams(dp(10), dp(10), Gravity.CENTER));
        }
        rail.addView(dot, new LinearLayout.LayoutParams(dp(28), dp(28)));
        if (!last) {
            View line = new View(this);
            line.setBackgroundColor(done ? 0xFF86EFAC : 0xFFE5E7EB);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(2), 0, 1f);
            lp.topMargin = dp(2);
            rail.addView(line, lp);
        }

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setPadding(dp(12), dp(3), 0, last ? 0 : dp(18));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(active ? 0xFF2563EB : done ? 0xFF111827 : 0xFF94A3B8);
        t.setTextSize(15);
        t.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(t.getContext()));
        head.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView w = new TextView(this);
        w.setText(done && when != null ? when : active ? "In progress" : "");
        w.setTextColor(0xFF9CA3AF);
        w.setTextSize(11);
        head.addView(w);
        text.addView(head);
        TextView b = new TextView(this);
        b.setText(body);
        b.setTextColor(done || active ? 0xFF6B7280 : 0xFFA1A1AA);
        b.setTextSize(12);
        b.setPadding(0, dp(2), 0, 0);
        text.addView(b);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
