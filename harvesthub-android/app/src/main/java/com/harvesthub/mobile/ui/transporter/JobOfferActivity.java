package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;

import java.util.Locale;

/**
 * Figma "Matchmaking Job Offer": payout (95% of the transport fee), distance, duration, route, cargo and the
 * 10-minute request countdown (decision 15 Nov). Accept → pickup screen; Decline → dispatch moves on.
 */
public class JobOfferActivity extends BaseTransporterOrderActivity {

    private TextView timer;
    private final Runnable tick = this::tick;

    @Override
    protected String title() {
        return "Matchmaking Job Offer";
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout head = TUi.card(c, TUi.DARK, TUi.DARK);
        LinearLayout hr = TUi.hrow(head, 0);
        TUi.fill(hr, "\u25CF  PRIORITY MATCH", 12, Color.parseColor("#A7F3D0"), true).setLetterSpacing(0.05f);
        timer = TUi.label(hr, "", 14, Color.parseColor("#86EFAC"), true);
        bar = TUi.progress(head, 100, 10);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#86EFAC")));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2D6A4F")));

        double payout = o.driverPayout();
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        double km = o.getDistanceKm() == null ? 0 : o.getDistanceKm();

        LinearLayout pay = TUi.card(c);
        LinearLayout pr = TUi.hrow(pay, 0);
        TextView cap = TUi.fill(pr, "PAYOUT OFFER", 11, TUi.MUTED, true);
        cap.setLetterSpacing(0.06f);
        TUi.iconTile(pr, R.drawable.ic_banknote, 38, TUi.GREY_BG, TUi.DARK, true);
        LinearLayout amount = TUi.hrow(pay, 0);
        TUi.label(amount, rs(payout), 30, TUi.DARK, true);
        TextView g = TUi.tag(amount, "Guaranteed", TUi.MINT, TUi.MINT_INK);
        ((LinearLayout.LayoutParams) g.getLayoutParams()).leftMargin = TUi.dp(this, 10);
        LinearLayout inc = TUi.box(pay, TUi.GREY_BG, 10);
        LinearLayout ir = TUi.hrow(inc, 0);
        TUi.icon(ir, R.drawable.ic_receipt_header, 18, TUi.INK);
        TextView it = TUi.fill(ir, "Transport fee " + rs(fee) + " less 5% platform commission (-" + rs(fee - payout) + "). Held in escrow.",
                13, TUi.INK, false);
        ((LinearLayout.LayoutParams) it.getLayoutParams()).leftMargin = TUi.dp(this, 8);
        LinearLayout tl = TUi.tiles(pay);
        TUi.tile(tl, R.drawable.ic_clock, "Est. Duration", duration(km), null);
        TUi.tile(tl, R.drawable.ic_route, "Total Distance", TUi.km(km), null);
        LinearLayout dates = TUi.tiles(pay);
        TUi.tile(dates, R.drawable.ic_calendar, "Pickup Date", day(o.getPickupDate() != null ? o.getPickupDate() : o.getDeliveryDate()), null);
        TUi.tile(dates, R.drawable.ic_scale_manifest, "Load", String.format(java.util.Locale.US, "%,.0f kg", o.getQuantity()), null);

        LinearLayout rp = TUi.card(c);
        LinearLayout rh = TUi.hrow(rp, 0);
        TUi.fill(rh, "Route Preview", 16, TUi.DARK, true);
        TUi.label(rh, "Farm \u2192 Buyer", 12, Color.parseColor("#15803D"), true);
        LinearLayout rb = TUi.box(rp, Color.parseColor("#E3E8EF"), 10);
        LinearLayout ab = TUi.hrow(rb, 0);
        ab.setBackground(TUi.round(Color.WHITE, TUi.dp(this, 10)));
        ab.setPadding(TUi.dp(this, 10), TUi.dp(this, 10), TUi.dp(this, 10), TUi.dp(this, 10));
        TUi.tag(ab, "A", TUi.DARK, Color.WHITE);
        TextView from = TUi.fill(ab, "  " + (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()), 13, TUi.INK, true);
        from.setMaxLines(1);
        from.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.label(ab, " \u2192 ", 13, TUi.MUTED, false);
        TUi.tag(ab, "B", TUi.MINT, TUi.MINT_INK);
        TextView to = TUi.fill(ab, "  " + (o.getDropAddress() == null ? "Buyer" : o.getDropAddress()), 13, TUi.INK, true);
        to.setMaxLines(1);
        to.setEllipsize(android.text.TextUtils.TruncateAt.END);

        LinearLayout spec = TUi.card(c);
        TUi.text(spec, "Consignment Specs", 16, TUi.DARK, true);
        LinearLayout man = TUi.box(spec, TUi.GREY_BG, 12);
        LinearLayout mr = TUi.hrow(man, 0);
        TUi.iconTile(mr, R.drawable.ic_package, 48, Color.WHITE, TUi.DARK, false);
        LinearLayout mt = TUi.col(mr, 1f);
        mt.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.text(mt, "Produce Manifest", 11, TUi.MUTED, true);
        TUi.text(mt, cargo(o), 15, TUi.INK, true);
        if (o.getVehicleLabel() != null) TUi.text(mt, "Vehicle class: " + o.getVehicleLabel(), 12, TUi.MUTED, false);
        LinearLayout stops = TUi.box(spec, Color.WHITE, 6);
        stops.setPadding(0, TUi.dp(this, 10), 0, 0);
        TUi.timeline(stops, new String[]{"Pickup Origin", o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress()},
                new String[]{"Drop-off Destination", o.getBuyerName() == null ? "Buyer" : o.getBuyerName(),
                        (o.getDropAddress() == null ? "" : o.getDropAddress()) + (o.getDeliveryDate() == null ? "" : " \u00B7 deliver by " + o.getDeliveryDate())});
        LinearLayout note = TUi.box(spec, TUi.BLUE_BG, 12);
        LinearLayout nr = TUi.hrow(note, 0);
        TUi.icon(nr, R.drawable.ic_shield_check, 18, TUi.INK);
        TextView nt = TUi.fill(nr, "Take at least 2 pre-loading photos at the farm. Start Journey unlocks after the buyer approves them.", 13, TUi.INK, false);
        ((LinearLayout.LayoutParams) nt.getLayoutParams()).leftMargin = TUi.dp(this, 8);

        TUi.iconButton(actions, "Accept Offer", R.drawable.ic_check_circle_outline_white, TUi.DARK, Color.WHITE,
                v -> sendEvent("ACCEPT", "Accepted in the app", null, v));
        TUi.iconButton(actions, "Decline Offer", R.drawable.ic_close_dark, Color.parseColor("#E9E7E5"), TUi.INK, v -> new AlertDialog.Builder(this)
                .setTitle("Decline this job?")
                .setMessage("The request goes to the next nearest driver.")
                .setPositiveButton("Decline", (d, w) -> sendEvent("DECLINE", "Declined in the app", null, null))
                .setNegativeButton("Keep", null).show());
        tick();
    }

    private android.widget.ProgressBar bar;

    @Override
    protected boolean handOver(Order o) {
        // after a decline / expiry the order belongs to someone else → back to the Jobs list
        if (!session.getUserId().equals(o.getTransporterId())) {
            startActivity(new Intent(this, TransporterJobsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
            return true;
        }
        return super.handOver(o);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void tick() {
        handler.removeCallbacks(tick);
        if (timer == null || order == null) return;
        Long exp = order.getTransportRequestExpiresAt();
        timer.setText(countdown(exp));
        if (bar != null && exp != null) {
            long left = Math.max(0, exp - System.currentTimeMillis());
            bar.setProgress((int) Math.min(100, left * 100 / (10 * 60 * 1000L)));
        }
        handler.postDelayed(tick, 1000);
    }

    /** "Expires in 07:42" from an epoch-ms deadline. */
    static String countdown(Long expiresAt) {
        if (expiresAt == null) return "Waiting for your answer";
        long left = Math.max(0, expiresAt - System.currentTimeMillis()) / 1000;
        if (left == 0) return "Expired – being sent to another driver";
        return String.format(Locale.US, "⏱ Expires in %02d:%02d", left / 60, left % 60);
    }

    /** Rough driving time at 40 km/h (MarketplaceRules.defaultTruckSpeedKmh). */
    static String duration(double km) {
        int min = (int) Math.round(km / 40.0 * 60);
        return min >= 60 ? String.format(Locale.US, "%dh %02dm", min / 60, min % 60) : min + " min";
    }
}
