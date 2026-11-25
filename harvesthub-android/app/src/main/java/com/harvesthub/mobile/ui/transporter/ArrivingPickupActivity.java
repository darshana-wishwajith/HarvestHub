package com.harvesthub.mobile.ui.transporter;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.harvesthub.mobile.R;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Pickup Route Map" screen. After accepting, the farmer harvests; when they tap Harvest Ready the driver gets the
 * pickup pin and can confirm arrival. Turn-by-turn directions open in Google Maps.
 */
public class ArrivingPickupActivity extends BaseTransporterOrderActivity {

    private RouteMap map;
    private Tracking tracking;

    @Override
    protected String title() {
        return "Pickup Route Map";
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
        if (map != null) {
            map.chip(ready ? "Harvest ready \u2013 head to the farm" : "Farmer is harvesting",
                    o.getPickupAddress() == null ? null : "Pickup: " + o.getPickupAddress());
        }
        LinearLayout stop = TUi.card(c);
        LinearLayout tags = TUi.hrow(stop, 0);
        TUi.tag(tags, "PICKUP STOP", TUi.DARK, Color.WHITE);
        TextView st = TUi.tag(tags, ready ? "Harvest ready" : "Farmer harvesting", ready ? TUi.MINT : TUi.AMBER_BG, ready ? TUi.MINT_INK : TUi.AMBER);
        ((LinearLayout.LayoutParams) st.getLayoutParams()).leftMargin = TUi.dp(this, 8);
        TUi.text(stop, o.getFarmerName() == null ? "Farm pickup" : o.getFarmerName(), 22, TUi.DARK, true).setPadding(0, TUi.dp(this, 8), 0, 0);
        LinearLayout ad = TUi.hrow(stop, 4);
        TUi.icon(ad, R.drawable.ic_dock_terminal, 16, TUi.MUTED);
        TextView at = TUi.fill(ad, o.getPickupAddress() == null ? "Pickup address not set" : o.getPickupAddress(), 14, TUi.MUTED, false);
        ((LinearLayout.LayoutParams) at.getLayoutParams()).leftMargin = TUi.dp(this, 6);

        LinearLayout info = TUi.box(stop, TUi.GREY_BG, 14);
        LinearLayout ir = TUi.hrow(info, 0);
        ir.setGravity(android.view.Gravity.TOP);
        TUi.iconTile(ir, R.drawable.ic_info_outline, 34, TUi.DARK, Color.WHITE, false);
        LinearLayout it = TUi.col(ir, 1f);
        it.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(it, "PICKUP INSTRUCTIONS", 12, TUi.INK, true);
        TUi.text(it, ready ? "Inspect the harvest and take at least 2 photos before loading. The buyer approves them remotely; Start Journey stays locked until then."
                : "You'll get a notification with the exact pickup pin when the farmer taps Harvest Ready. You can already head towards the farm.",
                13, Color.parseColor("#374151"), false);

        LinearLayout t1 = TUi.tiles(stop);
        TUi.tile(t1, R.drawable.ic_tractor, "Cargo Type", o.getCropName() == null ? "Produce" : o.getCropName(), null);
        TUi.tile(t1, R.drawable.ic_scale_manifest, "Payload Weight", String.format(java.util.Locale.US, "%,.0f kg", o.getQuantity()), null);
        LinearLayout t2 = TUi.tiles(stop);
        TUi.tile(t2, R.drawable.ic_route, "Farm \u2192 Buyer", o.getDistanceKm() == null ? "\u2014" : TUi.km(o.getDistanceKm()), "Order " + ref());
        TUi.tile(t2, R.drawable.ic_banknote, "Your Payout", rs(o.driverPayout()), "On delivery");
        LinearLayout t3 = TUi.tiles(stop);
        TUi.tile(t3, R.drawable.ic_calendar, "Pickup Date", day(o.getPickupDate() != null ? o.getPickupDate() : o.getDeliveryDate()), null);
        TUi.tile(t3, R.drawable.ic_truck, "Vehicle", o.getVehiclePlate() != null ? o.getVehiclePlate()
                : o.getVehicleLabel() != null ? o.getVehicleLabel() : "—", null);

        // where the load goes after pickup
        LinearLayout drop = TUi.card(c);
        TUi.iconHeader(drop, R.drawable.ic_route, "Trip", "Order " + ref(), TUi.GREY_BG, TUi.INK);
        LinearLayout tl = TUi.box(drop, Color.WHITE, 10);
        tl.setPadding(0, 0, 0, 0);
        TUi.timeline(tl,
                new String[]{"Pickup • " + day(o.getPickupDate()), o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress()},
                new String[]{"Drop-off", o.getBuyerName() == null ? "Buyer" : o.getBuyerName(), o.getDropAddress()});

        TUi.iconButton(actions, "Open Navigation in Google Maps", R.drawable.ic_navigation_pointer, Color.parseColor("#E9E7E5"), TUi.INK,
                v -> RouteMap.navigate(this, tracking == null ? null : tracking.pickupLatitude, tracking == null ? null : tracking.pickupLongitude));
        TUi.iconButton(actions, ready ? "Confirm Arrival at Pickup" : "Waiting for Harvest Ready\u2026", R.drawable.ic_check_circle_outline_white,
                TUi.DARK, Color.WHITE, v -> sendEvent("ARRIVED_PICKUP", "Arrived at the farm", null, v)).setEnabled(ready);
    }

}
