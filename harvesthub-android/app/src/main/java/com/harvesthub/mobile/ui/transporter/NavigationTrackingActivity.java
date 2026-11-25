package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * In-transit telemetry screen: map, remaining km, ETA and progress from GET /orders/{id}/tracking, which
 * the background GPS service feeds every 30 s. Sensor values (temperature, humidity) are not
 * shown – no sensors in scope.
 */
public class NavigationTrackingActivity extends BaseTransporterOrderActivity {

    private RouteMap map;
    private Tracking tracking;

    @Override
    protected String title() {
        return "In Transit";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId != null) {
            map = new RouteMap(this);
            TransporterTrackingService.start(this);
        }
    }

    @Override
    protected void load() {
        super.load();
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (r.isSuccessful() && r.body() != null) {
                    tracking = r.body();
                    map.show(tracking, true);
                    if (order != null) render();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        int green = Color.parseColor("#15803D");
        Tracking t = tracking;
        if (map != null) {
            map.chip(t != null && t.speedKmh != null ? String.format(Locale.US, "%.0f km/h", t.speedKmh) : "Live GPS",
                    t == null || t.lastUpdate == null ? "Waiting for the first GPS point\u2026" : "Last point " + time(t.lastUpdate));
        }
        LinearLayout strip = TUi.hrow(c, 12);
        strip.setBackground(TUi.round(TUi.DARK, TUi.dp(this, 40)));
        strip.setPadding(TUi.dp(this, 14), TUi.dp(this, 8), TUi.dp(this, 14), TUi.dp(this, 8));
        TUi.fill(strip, "\u25CF  Live Telemetry Active \u2022 Buyer Connected", 12, Color.WHITE, true);

        LinearLayout k = TUi.card(c);
        LinearLayout top = TUi.hrow(k, 0);
        top.setGravity(android.view.Gravity.TOP);
        LinearLayout left = TUi.col(top, 1f);
        TUi.text(left, "ACTIVE CORRIDOR", 11, TUi.INK, true).setLetterSpacing(0.04f);
        TextView corridor = TUi.text(left, (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()) + " \u2013 " + (o.getDropAddress() == null ? "Buyer" : o.getDropAddress()),
                18, TUi.INK, true);
        corridor.setMaxLines(2);
        corridor.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.text(left, (o.getFarmerName() == null ? "Farm" : o.getFarmerName()) + " \u2192 " + (o.getBuyerName() == null ? "Buyer" : o.getBuyerName()), 12, TUi.MUTED, false);
        LinearLayout right = TUi.col(top, 0);
        right.setGravity(android.view.Gravity.END);
        right.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.label(right, "ARRIVAL EST.", 11, TUi.INK, true);
        TUi.label(right, t == null || t.etaMinutes == null ? "\u2014" : JobOfferActivity.duration(t.etaMinutes * 40.0 / 60.0), 22, TUi.DARK, true);
        TUi.label(right, "Order " + ref(), 11, green, true);

        LinearLayout tl = TUi.tiles(k);
        TUi.tile(tl, R.drawable.ic_odometer, "Remaining", t == null || t.remainingKm == null ? "\u2014" : TUi.km(t.remainingKm), null);
        TUi.tile(tl, R.drawable.ic_archive_box, "Cargo Load", String.format(Locale.US, "%,.0f kg", o.getQuantity()), null);
        int pct = t == null ? 0 : t.progressPercent;
        LinearLayout pr = TUi.hrow(k, 14);
        TUi.fill(pr, "Trip Progress", 13, TUi.INK, true);
        TUi.label(pr, pct + "% Completed", 13, TUi.INK, false);
        TUi.progress(k, pct, 6);
        if (t != null && t.remainingKm != null) {
            LinearLayout kr = TUi.hrow(k, 6);
            TUi.fill(kr, TUi.km(Math.max(0, t.totalKm - t.remainingKm)) + " covered", 12, TUi.MUTED, false);
            TUi.label(kr, TUi.km(t.remainingKm) + " remaining", 12, TUi.MUTED, false);
        }
        LinearLayout gps = TUi.box(k, TUi.GREY_BG, 12);
        LinearLayout gr = TUi.hrow(gps, 0);
        TUi.iconTile(gr, R.drawable.ic_gps_crosshair, 34, TUi.MINT, TUi.DARK, true);
        LinearLayout gt = TUi.col(gr, 1f);
        gt.setPadding(TUi.dp(this, 10), 0, TUi.dp(this, 8), 0);
        TUi.text(gt, "GPS shared every 30 s", 13, TUi.INK, true);
        TUi.text(gt, t == null ? "Loading route\u2026" : t.lastUpdate == null ? "Waiting for the first GPS point\u2026"
                : t.pointCount + " points sent \u2022 last " + time(t.lastUpdate), 12, TUi.MUTED, false);
        TUi.tag(gr, "ACTIVE", TUi.MINT, TUi.MINT_INK);
        LinearLayout links = TUi.hrow(k, 12);
        TextView nav = TUi.fill(links, "\u27A4 Open in Google Maps", 13, TUi.INK, true);
        nav.setOnClickListener(v -> RouteMap.navigate(this, tracking == null ? null : tracking.dropLatitude, tracking == null ? null : tracking.dropLongitude));

        TUi.iconButton(actions, "Confirm Arrival at Destination", R.drawable.ic_check_circle_outline_white, TUi.DARK, Color.WHITE,
                v -> sendEvent("ARRIVED_DESTINATION", "Arrived at the buyer", null, v));
    }

    @Override
    protected void onEventDone(String event, Order updated) {
        if ("ARRIVED_DESTINATION".equals(event)) TransporterTrackingService.stop(this);
        super.onEventDone(event, updated);
    }
}
