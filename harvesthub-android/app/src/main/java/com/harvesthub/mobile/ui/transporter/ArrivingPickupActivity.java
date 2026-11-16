package com.harvesthub.mobile.ui.transporter;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma "Pickup Route Map". After accepting, the farmer harvests; when they tap Harvest Ready the driver gets the
 * pickup pin (workflow 4.1) and can confirm arrival. Turn-by-turn directions open in Google Maps.
 */
public class ArrivingPickupActivity extends BaseTransporterOrderActivity {

    private RouteMap map;
    private Tracking tracking;

    @Override
    protected String title() {
        return "Pickup Route";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (orderId != null) map = new RouteMap(this);
    }

    @Override
    protected void load() {
        super.load();
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (r.isSuccessful() && r.body() != null) {
                    tracking = r.body();
                    map.show(tracking, false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        boolean ready = "HARVEST_READY".equals(o.getStatus());
        LinearLayout stop = TUi.card(c);
        TUi.pill(stop, ready ? "PICKUP • Harvest ready" : "PICKUP • Farmer is harvesting", ready ? TUi.SURFACE : TUi.AMBER_BG, ready ? TUi.DARK : TUi.AMBER);
        TUi.text(stop, o.getFarmerName() == null ? "Farm pickup" : o.getFarmerName(), 20, TUi.DARK, true);
        TUi.body(stop, o.getPickupAddress() == null ? "Pickup address not set" : o.getPickupAddress());
        if (!ready) {
            TUi.body(stop, "You'll get a notification with the exact pickup pin when the farmer taps Harvest Ready. "
                    + "You can already head towards the farm.");
        }
        TUi.row(stop, "Cargo", cargo(o));
        TUi.row(stop, "Order", ref());
        if (o.getDistanceKm() != null) TUi.row(stop, "Farm → buyer", TUi.km(o.getDistanceKm()));
        TUi.row(stop, "Your payout", rs(o.driverPayout()), TUi.DARK, true);

        LinearLayout info = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
        TUi.text(info, "At the farm", 14, TUi.DARK, true);
        TUi.body(info, "Inspect the harvest and take at least 2 photos before loading. The buyer approves them remotely; "
                + "Start Journey stays locked until then.");

        TUi.secondary(actions, "Open navigation in Google Maps", v -> RouteMap.navigate(this,
                tracking == null ? null : tracking.pickupLatitude, tracking == null ? null : tracking.pickupLongitude));
        TUi.primary(actions, ready ? "✓  Confirm Arrival at Pickup" : "Waiting for Harvest Ready…",
                v -> sendEvent("ARRIVED_PICKUP", "Arrived at the farm", null, v)).setEnabled(ready);
    }
}
