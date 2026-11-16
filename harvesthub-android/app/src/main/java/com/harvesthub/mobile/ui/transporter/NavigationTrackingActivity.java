package com.harvesthub.mobile.ui.transporter;

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
 * Figma in-transit telemetry screen: map, remaining km, ETA and progress from GET /orders/{id}/tracking, which
 * the background GPS service feeds every 30 s (decision 15 Nov). Sensor values (temperature, humidity) are not
 * shown – no sensors in scope (decision 15 Nov).
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
        TUi.pill(c, "● Live tracking active • buyer connected", TUi.SURFACE, TUi.DARK);
        LinearLayout k = TUi.card(c);
        TUi.caption(k, "Active corridor");
        TUi.text(k, (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()) + " → " + (o.getDropAddress() == null ? "Buyer" : o.getDropAddress()), 16, TUi.INK, true);
        Tracking t = tracking;
        if (t != null) {
            TUi.row(k, "Arrival est.", t.etaMinutes == null ? "—" : JobOfferActivity.duration(t.etaMinutes * 40.0 / 60.0), TUi.DARK, true);
            TUi.row(k, "Remaining", t.remainingKm == null ? "—" : TUi.km(t.remainingKm));
            TUi.row(k, "Cargo load", String.format(Locale.US, "%,.0f kg", o.getQuantity()));
            if (t.speedKmh != null) TUi.row(k, "Speed", String.format(Locale.US, "%.0f km/h", t.speedKmh));
            TUi.caption(k, "Trip progress • " + t.progressPercent + "% completed");
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(t.progressPercent);
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(TUi.DARK));
            k.addView(bar);
            TUi.body(k, t.lastUpdate == null ? "Waiting for the first GPS point…"
                    : "Last GPS point " + time(t.lastUpdate) + " • " + t.pointCount + " points sent");
        } else {
            TUi.body(k, "Loading route…");
        }
        TUi.secondary(actions, "Open navigation in Google Maps", v -> RouteMap.navigate(this,
                tracking == null ? null : tracking.dropLatitude, tracking == null ? null : tracking.dropLongitude));
        TUi.primary(actions, "✓  Confirm Arrival at Destination", v -> sendEvent("ARRIVED_DESTINATION", "Arrived at the buyer", null, v));
    }

    @Override
    protected void onEventDone(String event, Order updated) {
        if ("ARRIVED_DESTINATION".equals(event)) TransporterTrackingService.stop(this);
        super.onEventDone(event, updated);
    }
}
