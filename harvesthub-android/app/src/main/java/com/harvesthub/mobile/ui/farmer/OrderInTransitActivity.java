package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;
import com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity;
import com.harvesthub.mobile.ui.transporter.TUi;
import com.harvesthub.mobile.utils.OsmMaps;
import com.harvesthub.mobile.utils.TripMap;

import org.osmdroid.views.MapView;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Farmer "Live Delivery" (replaces the static Figma 31 illustration and the drawn map): from loading until both
 * sides confirm the delivery. Real OSM map with the road route and the driver's last GPS point
 * (GET /orders/{id}/tracking, refreshed with the order every 10 s), ETA / distance / progress, driver contact,
 * and what happens to the money (20 % advance already paid, 80 % on confirmation – workflow 4.3, 5.1–5.2).
 */
public class OrderInTransitActivity extends BaseOrderActivity {

    private MapView mapView;
    private TripMap trip;
    private Tracking tracking;
    private LinearLayout header;
    private LinearLayout content;
    private TextView liveChip;

    @Override
    protected int layout() {
        return R.layout.activity_order_in_transit;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OsmMaps.init(this); // before the layout with the MapView is inflated
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        header = findViewById(R.id.tripHeader);
        content = findViewById(R.id.tripContent);
        liveChip = findViewById(R.id.tvLiveChip);
        mapView = findViewById(R.id.mapFarmerTrip);
        trip = new TripMap(mapView, TUi.DARK, Color.parseColor("#93C5A4"));
        // let the map pan inside the scrolling page
        mapView.setOnTouchListener((v, e) -> {
            v.getParent().requestDisallowInterceptTouchEvent(e.getAction() != MotionEvent.ACTION_UP
                    && e.getAction() != MotionEvent.ACTION_CANCEL);
            return false;
        });
        findViewById(R.id.btnRecenter).setOnClickListener(v -> trip.focusVehicle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }

    @Override
    protected void bind(@NonNull Order o) {
        render(o);
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (isFinishing() || isDestroyed() || !r.isSuccessful() || r.body() == null) return;
                tracking = r.body();
                trip.show(tracking);
                if (order != null) render(order);
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    private void render(Order o) {
        String s = o.getStatus() == null ? "" : o.getStatus();
        boolean moving = "IN_TRANSIT".equals(s);
        boolean atBuyer = "ARRIVED_AT_DESTINATION".equals(s);
        Tracking t = tracking;

        // ---- header
        header.removeAllViews();
        LinearLayout hr = TUi.hrow(header, 8);
        TUi.fill(hr, moving ? "On the way to the buyer" : atBuyer ? "At the buyer" : "Delivered – confirming", 22, TUi.DARK, true);
        TUi.tag(hr, moving ? "● LIVE" : atBuyer ? "INSPECTION" : "CONFIRMING",
                moving ? TUi.MINT : TUi.AMBER_BG, moving ? TUi.MINT_INK : TUi.AMBER);
        TUi.text(header, orderLabel(o) + "  •  " + kg(o.getQuantity()) + " " + (o.getCropName() == null ? "" : o.getCropName())
                + "  •  Buyer: " + (o.getBuyerName() == null ? "—" : o.getBuyerName()), 13, TUi.MUTED, false).setPadding(0, TUi.dp(this, 4), 0, 0);

        // ---- live chip on the map
        boolean gps = t != null && t.currentLatitude != null;
        if (moving) {
            liveChip.setText(gps ? "● Live • updated " + (t.lastUpdate == null ? "now" : BaseBuyerOrderActivity.time(t.lastUpdate))
                    : "Waiting for the driver's GPS…");
        } else {
            liveChip.setText(atBuyer ? "● Vehicle at the delivery point" : "● Goods delivered");
        }

        content.removeAllViews();

        // ---- trip progress
        LinearLayout pc = TUi.card(content);
        LinearLayout tiles = TUi.tiles(pc);
        ((LinearLayout.LayoutParams) tiles.getLayoutParams()).topMargin = 0;
        String eta = !moving ? "Arrived" : t == null || t.etaMinutes == null ? "—" : minutes(t.etaMinutes);
        String left = !moving ? "0 km" : t == null || t.remainingKm == null ? "—" : TUi.km(t.remainingKm);
        TUi.tile(tiles, R.drawable.ic_clock, "Arriving in", eta, null);
        TUi.tile(tiles, R.drawable.ic_location_pin, "Remaining", left, null);
        int pct = t == null ? (moving ? 0 : 100) : t.progressPercent;
        TUi.progress(pc, pct, 14);
        LinearLayout pr = TUi.hrow(pc, 6);
        TUi.fill(pr, pct + "% of " + (t != null && t.totalKm > 0 ? TUi.km(t.totalKm) : "the trip"), 12, TUi.MUTED, true);
        if (t != null && t.speedKmh != null && moving) TUi.label(pr, String.format(Locale.US, "%.0f km/h", t.speedKmh), 12, TUi.MUTED, true);

        // ---- route
        LinearLayout rc = TUi.card(content);
        TUi.iconHeader(rc, R.drawable.ic_local_shipping, "Route", null, 0, 0);
        LinearLayout tl = TUi.box(rc, Color.TRANSPARENT, 10);
        tl.setPadding(0, 0, 0, 0);
        TUi.timeline(tl,
                new String[]{"Pickup • your farm", nz(o.getPickupAddress(), "Farm"), loadedAt(o)},
                new String[]{"Delivery • buyer", nz(o.getDropAddress(), "Buyer's delivery point"),
                        o.getBuyerName() == null ? null : o.getBuyerName()});

        // ---- driver
        LinearLayout dc = TUi.card(content);
        LinearLayout dr = TUi.hrow(dc, 0);
        TUi.iconTile(dr, R.drawable.ic_truck, 48, TUi.SURFACE, TUi.DARK, true);
        LinearLayout dt = TUi.col(dr, 1f);
        dt.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
        TextView dn = TUi.text(dt, o.getTransporterName() == null ? "Driver" : o.getTransporterName(), 16, TUi.INK, true);
        dn.setMaxLines(1);
        dn.setEllipsize(TextUtils.TruncateAt.END);
        TUi.text(dt, vehicleLine(o), 12, TUi.MUTED, false);
        TUi.smallButton(dr, "Call", R.drawable.ic_call, TUi.DARK, Color.WHITE, v -> dial(o.getTransporterPhone()));

        // ---- money
        Order.Payment p = o.getPayment();
        LinearLayout mc = TUi.card(content);
        TUi.iconHeader(mc, R.drawable.ic_wallet, "Your payment", p != null && p.farmerAdvanceReleased ? "ADVANCE PAID" : null,
                TUi.MINT, TUi.MINT_INK);
        if (p != null) {
            TUi.row(mc, "Crop value", lkr(p.cropValue));
            TUi.row(mc, "Advance (20%) – in your wallet", p.farmerAdvanceReleased ? lkr(p.advanceCropPortion) : "pending",
                    p.farmerAdvanceReleased ? Color.parseColor("#15803D") : TUi.MUTED, true);
            TUi.row(mc, "Balance (80%) – on confirmation", lkr(Math.max(0, p.cropValue - p.advanceCropPortion)), TUi.INK, true);
        }
        TUi.text(mc, "The balance is released to your wallet automatically when the driver and the buyer both confirm the delivery.",
                12, TUi.MUTED, false).setPadding(0, TUi.dp(this, 8), 0, 0);

        // ---- steps
        LinearLayout sc = TUi.card(content);
        TUi.header(sc, "Delivery steps", null, 0, 0);
        TUi.step(sc, "Loaded at your farm", 2, null);
        TUi.step(sc, "On the way to the buyer", moving ? 1 : 2, null);
        TUi.step(sc, "Buyer inspects the goods", atBuyer ? 1 : moving ? 0 : 2, null);
        TUi.step(sc, "Both confirm – balance paid to you", moving || atBuyer ? 0 : 1, null);
    }

    private static String loadedAt(Order o) {
        Order.TransportEvent e = o.lastEvent("START_JOURNEY");
        return e == null || e.timestamp == null ? null : "Left the farm " + BaseBuyerOrderActivity.time(e.timestamp);
    }

    private static String minutes(int m) {
        return m >= 60 ? String.format(Locale.US, "%dh %02dm", m / 60, m % 60) : m + " min";
    }

    private static String nz(String s, String fallback) {
        return s == null || s.isEmpty() ? fallback : s;
    }
}
